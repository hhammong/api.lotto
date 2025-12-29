package hhammong.apilotto.service;

import hhammong.apilotto.dto.DrawMatchResult;
import hhammong.apilotto.dto.UserPredictionCreateRequest;
import hhammong.apilotto.dto.UserPredictionResponse;
import hhammong.apilotto.entity.LottoHistory;
import hhammong.apilotto.entity.PredictionsHistory;
import hhammong.apilotto.entity.User;
import hhammong.apilotto.entity.UserPrediction;
import hhammong.apilotto.exception.DuplicateNumberException;
import hhammong.apilotto.exception.ResourceNotFoundException;
import hhammong.apilotto.repository.LottoHistoryRepository;
import hhammong.apilotto.repository.PredictionsHistoryRepository;
import hhammong.apilotto.repository.UserPredictionRepository;
import hhammong.apilotto.repository.UserRepository;
import hhammong.apilotto.util.LottoMatchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPredictionService {

    private final UserPredictionRepository predictionRepository;
    private final UserRepository userRepository;

    private final PredictionsHistoryRepository predictionsHistoryRepository;
    private final LottoHistoryRepository lottoHistoryRepository;
    private final UserPredictionCheckService checkService;

    /**
     * 번호 등록
     */
    @Transactional
    public UserPredictionResponse createPrediction(UUID userId, UserPredictionCreateRequest request) {
        // 1. 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 2. 중복 번호 검증 (요청 내부)
        if (request.hasDuplicates()) {
            throw new IllegalArgumentException("중복된 번호가 있습니다");
        }

        // 3. 번호 정렬
        List<Integer> sortedNumbers = request.getSortedNumbers();

        // 4. 동일 번호 조합 이미 등록되었는지 확인
        boolean exists = predictionRepository.existsDuplicateNumbers(
                userId,
                sortedNumbers.get(0).shortValue(),
                sortedNumbers.get(1).shortValue(),
                sortedNumbers.get(2).shortValue(),
                sortedNumbers.get(3).shortValue(),
                sortedNumbers.get(4).shortValue(),
                sortedNumbers.get(5).shortValue()
        );

        if (exists) {
            throw new DuplicateNumberException("이미 등록된 번호 조합입니다");
        }

        // 5. Entity 생성
        UserPrediction prediction = UserPrediction.builder()
                .user(user)
                .predictedNum1(sortedNumbers.get(0).shortValue())
                .predictedNum2(sortedNumbers.get(1).shortValue())
                .predictedNum3(sortedNumbers.get(2).shortValue())
                .predictedNum4(sortedNumbers.get(3).shortValue())
                .predictedNum5(sortedNumbers.get(4).shortValue())
                .predictedNum6(sortedNumbers.get(5).shortValue())
                .memo(request.getMemo())
                .targetDrawNo(request.getTargetDrawNo())
                // startDrawId는 나중에 로직 추가 (현재 최신 회차 + 1)
                .build();

        // 6. 저장 (Entity의 @PrePersist에서 predictedNumbers 자동 생성)
        UserPrediction saved = predictionRepository.save(prediction);

        savePredictionsHistory(saved, userId);

        // 7. DTO 변환 후 반환
        return UserPredictionResponse.from(saved);
    }

    /**
     * PREDICTIONS_HISTORY 저장
     */
    private void savePredictionsHistory(UserPrediction prediction, UUID userId) {
        // 1. 내 번호 리스트
        List<Integer> myNumbers = Arrays.asList(
                prediction.getPredictedNum1().intValue(),
                prediction.getPredictedNum2().intValue(),
                prediction.getPredictedNum3().intValue(),
                prediction.getPredictedNum4().intValue(),
                prediction.getPredictedNum5().intValue(),
                prediction.getPredictedNum6().intValue()
        );

        // 2. 시작 회차 결정 (CheckService의 로직 사용)
        Integer startDrawNo = determineStartDrawNo(prediction);

        // 3. 과거 회차 조회
        List<LottoHistory> pastDraws = lottoHistoryRepository
                .findByDrawNoGreaterThanEqualAndDeleteYnAndUseYnOrderByDrawNoAsc(
                        startDrawNo, "N", "Y");

        // 4. 각 회차별로 매칭 계산 & PredictionsHistory 생성
        List<PredictionsHistory> histories = pastDraws.stream()
                .map(draw -> {
                    // CheckService의 calculateDrawMatch 로직 활용
                    DrawMatchResult result = calculateDrawMatchForHistory(myNumbers, draw);

                    // 꽝이면 null 반환
                    if (result.getRank() == null) {
                        return null;
                    }

                    return PredictionsHistory.builder()
                            .predictionId(prediction.getPredictionId())
                            .historyId(draw.getHistoryId())
                            .userId(userId)
                            .rank(result.getRank())
                            .hasBonus(result.getHasBonus())
                            .matchedCount(result.getMatchCount().shortValue())
                            .prizeAmount(result.getPrizeAmount().intValue())
                            .build();
                })
                .filter(history -> history != null)
                .collect(Collectors.toList());

        // 5. 당첨된 것만 일괄 저장
        if (!histories.isEmpty()) {
            predictionsHistoryRepository.saveAll(histories);
        }
    }

    /**
     * 회차와 내 번호 매칭 계산 (CheckService 로직 복사)
     */
    private DrawMatchResult calculateDrawMatchForHistory(List<Integer> myNumbers, LottoHistory draw) {
        // 당첨번호 리스트
        List<Integer> winningNumbers = Arrays.asList(
                draw.getNumber1().intValue(),
                draw.getNumber2().intValue(),
                draw.getNumber3().intValue(),
                draw.getNumber4().intValue(),
                draw.getNumber5().intValue(),
                draw.getNumber6().intValue()
        );

        // 매칭 계산
        LottoMatchUtils.MatchResult matchResult = LottoMatchUtils.calculateMatch(
                myNumbers, winningNumbers, draw.getBonusNumber().intValue());

        // 일치한 번호들 찾기
        List<Integer> matchedNumbers = myNumbers.stream()
                .filter(winningNumbers::contains)
                .sorted()
                .collect(Collectors.toList());

        // 당첨금 계산
        Long prizeAmount = getPrizeAmount(draw, matchResult.getRank());

        return DrawMatchResult.builder()
                .drawNo(draw.getDrawNo())
                .drawDate(draw.getDrawDate())
                .winningNumbers(winningNumbers)
                .bonusNumber(draw.getBonusNumber().intValue())
                .matchCount(matchResult.getMatchCount())
                .hasBonus(matchResult.isHasBonus())
                .rank(matchResult.getRank())
                .rankDescription(matchResult.getRankDescription())
                .prizeAmount(prizeAmount)
                .matchedNumbers(matchedNumbers)
                .build();
    }

    /**
     * 등수별 당첨금 조회 (CheckService 로직 복사)
     */
    private Long getPrizeAmount(LottoHistory draw, Integer rank) {
        if (rank == null) return 0L;

        return switch (rank) {
            case 1 -> draw.getPrize1st();
            case 2 -> draw.getPrize2nd();
            case 3 -> draw.getPrize3rd();
            case 4 -> draw.getPrize4th() != null ? draw.getPrize4th().longValue() : 50000L;
            case 5 -> draw.getPrize5th() != null ? draw.getPrize5th().longValue() : 5000L;
            default -> 0L;
        };
    }

    /**
     * 시작 회차 결정 (CheckService 로직 복사)
     */
    private Integer determineStartDrawNo(UserPrediction prediction) {
        if (prediction.getStartDrawId() != null) {
            return prediction.getStartDrawId();
        }
        return 1;
    }

    /**
     * 내 번호 목록 조회
     */
    public List<UserPredictionResponse> getMyPredictions(UUID userId) {
        List<UserPrediction> predictions = predictionRepository
                .findByUser_UserIdAndDeleteYnOrderByCreatedAtDesc(userId, "N");
        return predictions.stream()
                .map(UserPredictionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 내 번호 목록 조회 (페이징)
     */
    public Page<UserPredictionResponse> getMyPredictions(UUID userId, Pageable pageable) {
        Page<UserPrediction> predictions = predictionRepository
                .findByUser_UserIdAndDeleteYnOrderByCreatedAtDesc(userId, "N", pageable);

        return predictions.map(UserPredictionResponse::from);
    }

    /**
     * 번호 상세 조회
     */
    public UserPredictionResponse getPredictionDetail(UUID userId, UUID predictionId) {
        UserPrediction prediction = predictionRepository
                .findByPredictionIdAndUser_UserIdAndDeleteYn(predictionId, userId, "N")
                .orElseThrow(() -> new ResourceNotFoundException("해당 번호를 찾을 수 없습니다"));

        return UserPredictionResponse.from(prediction);
    }

    /**
     * 등록된 번호 개수 조회
     */
    public long getMyPredictionCount(UUID userId) {
        return predictionRepository.countByUser_UserIdAndDeleteYn(userId, "N");
    }
}