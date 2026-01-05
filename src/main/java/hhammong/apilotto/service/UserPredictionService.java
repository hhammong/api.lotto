package hhammong.apilotto.service;

import hhammong.apilotto.dto.DrawMatchResult;
import hhammong.apilotto.dto.PredictionHistoryResponse;
import hhammong.apilotto.dto.UserPredictionCreateRequest;
import hhammong.apilotto.dto.UserPredictionResponse;
import hhammong.apilotto.entity.*;
import hhammong.apilotto.exception.DuplicateNumberException;
import hhammong.apilotto.exception.ResourceNotFoundException;
import hhammong.apilotto.repository.*;
import hhammong.apilotto.util.LottoMatchUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final UserPredictionHistoricalStatsRepository userPredictionHistoricalStatsRepository;
    private final UserPredictionTrackingStatsRepository userPredictionTrackingStatsRepository;

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

        Integer startDrawId = calculateStartDrawId();

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
                .startDrawId(startDrawId)
                // startDrawId는 나중에 로직 추가 (현재 최신 회차 + 1)
                .build();

        // 6. 저장 (Entity의 @PrePersist에서 predictedNumbers 자동 생성)
        UserPrediction saved = predictionRepository.save(prediction);

        savePredictionsHistory(saved, userId);
        saveUserPredictionHistoricalStats(userId, prediction);
        saveUserPredictionTrackingStats(userId, prediction);

        // 7. DTO 변환 후 반환
        return UserPredictionResponse.from(saved);
    }

    /**
     * 현재 시간 기준으로 시작 회차 계산
     * - 토요일 20:00 이전: 이번 주 회차
     * - 토요일 20:00 이후: 다음 주 회차
     */
    private Integer calculateStartDrawId() {
        LocalDateTime now = LocalDateTime.now();

        // 최신 회차 정보 조회
        LottoHistory latestHistory = lottoHistoryRepository
                .findTopByDeleteYnAndUseYnOrderByDrawNoDesc("N", "Y")
                .orElseThrow(() -> new IllegalStateException("최신 회차 정보를 찾을 수 없습니다"));

        Integer latestDrawNo = latestHistory.getDrawNo();
        LocalDate latestDrawDate = latestHistory.getDrawDate();

        // 다음 추첨일 계산 (최신 추첨일 + 7일씩 더해가며 현재보다 미래인 토요일 찾기)
        LocalDateTime nextDrawDateTime = latestDrawDate.atTime(20, 0);
        Integer nextDrawNo = latestDrawNo;

        while (nextDrawDateTime.isBefore(now) || nextDrawDateTime.isEqual(now)) {
            nextDrawDateTime = nextDrawDateTime.plusWeeks(1);
            nextDrawNo++;
        }

        return nextDrawNo;
    }

    /**
     * 이번 주 토요일 20:00 시간 계산
     */
    private LocalDateTime getThisWeekSaturdayDrawTime(LocalDateTime now) {
        LocalDate today = now.toLocalDate();

        // 이번 주 토요일 찾기
        LocalDate thisSaturday = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(
                java.time.DayOfWeek.SATURDAY
        ));

        return thisSaturday.atTime(20, 0, 0);
    }

    private void saveUserPredictionHistoricalStats(UUID userId, UserPrediction prediction) {
        // 1. 내 번호 조회
        /*UserPrediction prediction = predictionRepository
                .findByPredictionIdAndUser_UserIdAndDeleteYn(predictionId, userId, "N")
                .orElseThrow(() -> new ResourceNotFoundException("해당 번호를 찾을 수 없습니다"));*/

        // 2. 내 번호 리스트
        List<Integer> myNumbers = Arrays.asList(
                prediction.getPredictedNum1().intValue(),
                prediction.getPredictedNum2().intValue(),
                prediction.getPredictedNum3().intValue(),
                prediction.getPredictedNum4().intValue(),
                prediction.getPredictedNum5().intValue(),
                prediction.getPredictedNum6().intValue()
        );

        // 3. 시작 회차 결정
        //Integer startDrawNo = determineStartDrawNo(prediction);

        // 4. 시작 회차 이후 모든 회차 조회
        List<LottoHistory> allDraws = lottoHistoryRepository
                .findByDrawNoGreaterThanEqualAndDeleteYnAndUseYnOrderByDrawNoAsc(
                        1, "N", "Y");

        // 5. 각 회차마다 매칭 계산
        List<DrawMatchResult> history = allDraws.stream()
                .map(draw -> calculateDrawMatchForHistory(myNumbers, draw))
                .collect(Collectors.toList());

        // 6. 통계 계산 및 응답 생성
        PredictionHistoryResponse response = buildHistoryResponse(prediction, myNumbers, 1, history);

        UserPredictionHistoricalStats entity = UserPredictionHistoricalStats.builder()
                .userPrediction(prediction)  // UserPrediction 객체
                .totalDraws(response.getTotalDraws())
                .winningDraws(response.getWinningDraws())
                .totalPrizeAmount(response.getTotalPrizeAmount())
                .bestRank(response.getBestRank())
                .bestDrawNo(response.getBestDrawNo())
                .returnRate(response.getReturnRate())
                .rank1Count(response.getRank1Count())
                .rank2Count(response.getRank2Count())
                .rank3Count(response.getRank3Count())
                .rank4Count(response.getRank4Count())
                .rank5Count(response.getRank5Count())
                .build();

        userPredictionHistoricalStatsRepository.save(entity);

    }
    private void saveUserPredictionTrackingStats(UUID userId, UserPrediction prediction) {
        // 1. 내 번호 조회
        /*UserPrediction prediction = predictionRepository
                .findByPredictionIdAndUser_UserIdAndDeleteYn(predictionId, userId, "N")
                .orElseThrow(() -> new ResourceNotFoundException("해당 번호를 찾을 수 없습니다"));*/

        // 2. 내 번호 리스트
        List<Integer> myNumbers = Arrays.asList(
                prediction.getPredictedNum1().intValue(),
                prediction.getPredictedNum2().intValue(),
                prediction.getPredictedNum3().intValue(),
                prediction.getPredictedNum4().intValue(),
                prediction.getPredictedNum5().intValue(),
                prediction.getPredictedNum6().intValue()
        );

        // 3. 시작 회차 결정
        Integer startDrawNo = determineStartDrawNo(prediction);

        // 4. 시작 회차 이후 모든 회차 조회
        List<LottoHistory> allDraws = lottoHistoryRepository
                .findByDrawNoGreaterThanEqualAndDeleteYnAndUseYnOrderByDrawNoAsc(
                        startDrawNo, "N", "Y");

        // 5. 각 회차마다 매칭 계산
        List<DrawMatchResult> history = allDraws.stream()
                .map(draw -> calculateDrawMatchForHistory(myNumbers, draw))
                .collect(Collectors.toList());

        // 6. 통계 계산 및 응답 생성
        PredictionHistoryResponse response = buildHistoryResponse(prediction, myNumbers, startDrawNo, history);

        UserPredictionTrackingStats entity = UserPredictionTrackingStats.builder()
                .userPrediction(prediction)  // UserPrediction 객체
                .totalDraws(response.getTotalDraws())
                .winningDraws(response.getWinningDraws())
                .totalPrizeAmount(response.getTotalPrizeAmount())
                .bestRank(response.getBestRank())
                .bestDrawNo(response.getBestDrawNo())
                .returnRate(response.getReturnRate())
                .rank1Count(response.getRank1Count())
                .rank2Count(response.getRank2Count())
                .rank3Count(response.getRank3Count())
                .rank4Count(response.getRank4Count())
                .rank5Count(response.getRank5Count())
                .build();

        userPredictionTrackingStatsRepository.save(entity);

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
        //Integer startDrawNo = determineStartDrawNo(prediction);

        // 3. 과거 회차 조회
        List<LottoHistory> pastDraws = lottoHistoryRepository
                .findByDrawNoGreaterThanEqualAndDeleteYnAndUseYnOrderByDrawNoAsc(
                        1, "N", "Y");

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
                            .startDrawSortation("past")
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
     * 이력 응답 생성 (통계 포함)
     */
    private PredictionHistoryResponse buildHistoryResponse(
            UserPrediction prediction,
            List<Integer> myNumbers,
            Integer startDrawNo,
            List<DrawMatchResult> history) {

        // 기본 통계
        int totalDraws = history.size();
        int winningDraws = (int) history.stream()
                .filter(h -> h.getRank() != null)
                .count();

        /*int totalDraws = getTotalDrawsSinceStart(startDrawNo);  // 전체 참여 회차
        int winningDraws = history.size();  // 당첨 회차 = history 개수*/

        // 등수별 카운트
        int rank1 = (int) history.stream().filter(h -> Integer.valueOf(1).equals(h.getRank())).count();
        int rank2 = (int) history.stream().filter(h -> Integer.valueOf(2).equals(h.getRank())).count();
        int rank3 = (int) history.stream().filter(h -> Integer.valueOf(3).equals(h.getRank())).count();
        int rank4 = (int) history.stream().filter(h -> Integer.valueOf(4).equals(h.getRank())).count();
        int rank5 = (int) history.stream().filter(h -> Integer.valueOf(5).equals(h.getRank())).count();

        // 금액 통계
        long totalPrize = history.stream()
                .mapToLong(DrawMatchResult::getPrizeAmount)
                .sum();
        long totalInvestment = totalDraws * 1000L;  // 회차당 1,000원
        long netProfit = totalPrize - totalInvestment;
        double returnRate = totalInvestment > 0
                ? ((double) totalPrize / totalInvestment * 100)
                : 0.0;

        // 최고 등수 찾기
        Integer bestRank = history.stream()
                .map(DrawMatchResult::getRank)
                .filter(rank -> rank != null)
                .min(Integer::compareTo)
                .orElse(null);

        Integer bestDrawNo = null;
        if (bestRank != null) {
            bestDrawNo = history.stream()
                    .filter(h -> bestRank.equals(h.getRank()))
                    .map(DrawMatchResult::getDrawNo)
                    .findFirst()
                    .orElse(null);
        }

        // 요약 메시지
        String message = generateHistorySummaryMessage(
                totalDraws, winningDraws, totalPrize, netProfit, returnRate,
                rank1, rank2, rank3, rank4, rank5, bestRank);

        return PredictionHistoryResponse.builder()
                .predictionId(prediction.getPredictionId())
                .myNumbers(myNumbers)
                .memo(prediction.getMemo())
                .createdAt(prediction.getCreatedAt())
                .startDrawNo(startDrawNo)
                .history(history)
                .totalDraws(totalDraws)
                .winningDraws(winningDraws)
                .rank1Count(rank1)
                .rank2Count(rank2)
                .rank3Count(rank3)
                .rank4Count(rank4)
                .rank5Count(rank5)
                .totalPrizeAmount(totalPrize)
                .totalInvestment(totalInvestment)
                .netProfit(netProfit)
                .returnRate(Math.round(returnRate * 100.0) / 100.0)  // 소수점 2자리
                .bestRank(bestRank)
                .bestDrawNo(bestDrawNo)
                .summaryMessage(message)
                .build();
    }

    /**
     * 이력 요약 메시지 생성
     */
    private String generateHistorySummaryMessage(
            int total, int winning, long prize, long profit, double returnRate,
            int r1, int r2, int r3, int r4, int r5, Integer bestRank) {

        if (total == 0) {
            return "참여 이력이 없습니다.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("총 %d회 참여, %d회 당첨. ", total, winning));

        if (winning > 0) {
            if (r1 > 0) sb.append(String.format("1등 %d회, ", r1));
            if (r2 > 0) sb.append(String.format("2등 %d회, ", r2));
            if (r3 > 0) sb.append(String.format("3등 %d회, ", r3));
            if (r4 > 0) sb.append(String.format("4등 %d회, ", r4));
            if (r5 > 0) sb.append(String.format("5등 %d회, ", r5));

            // 마지막 쉼표 제거
            if (sb.charAt(sb.length() - 2) == ',') {
                sb.setLength(sb.length() - 2);
                sb.append(". ");
            }
        }

        sb.append(String.format("총 당첨금: %,d원, ", prize));

        if (profit >= 0) {
            sb.append(String.format("수익: +%,d원 (%.1f%%)", profit, returnRate));
        } else {
            sb.append(String.format("손실: %,d원 (%.1f%%)", profit, returnRate));
        }

        if (bestRank != null) {
            sb.append(String.format(". 최고 등수: %d등", bestRank));
        }

        return sb.toString();
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
     * 시작 회차부터 현재까지 총 회차 수 계산
     */
    private int getTotalDrawsSinceStart(Integer startDrawNo) {
        LottoHistory latestDraw = lottoHistoryRepository
                .findTopByDeleteYnAndUseYnOrderByDrawNoDesc("N", "Y")
                .orElse(null);

        if (latestDraw == null) {
            return 0;
        }

        return latestDraw.getDrawNo() - startDrawNo + 1;
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