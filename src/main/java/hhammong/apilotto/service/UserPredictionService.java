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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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
                            .drawNo(draw.getDrawNo())
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

    /**
     * USER_PREDICTION_HISTORICAL_STATS 통계 조회
     */
    public PredictionHistoryResponse getUserPredictionHistoricalStats(UUID userId, UUID predictionId) {
        UserPredictionHistoricalStats prediction = userPredictionHistoricalStatsRepository
                .findByPredictionId(predictionId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 번호를 찾을 수 없습니다"));

        return PredictionHistoryResponse.from(prediction);
    }

    /**
     * USER_PREDICTION_TRACKING_STATS 통계 조회
     */
    public PredictionHistoryResponse getUserPredictionTrackingStats(UUID userId, UUID predictionId) {
        UserPredictionTrackingStats prediction = userPredictionTrackingStatsRepository
                .findByPredictionId(predictionId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 번호를 찾을 수 없습니다"));

        return PredictionHistoryResponse.from(prediction);
    }


    /**
     * 새 당첨번호와 모든 사용자 번호 매칭
     * 스케줄러에서 당첨번호 업데이트 후 호출됨
     */
    @Transactional
    public void matchNewDrawWithAllUserPredictions(Integer drawNo) {
        log.info("새 당첨번호({}회차)와 사용자 번호 매칭 시작", drawNo);

        try {
            // 1. 새로 저장된 당첨번호 조회
            LottoHistory newDraw = lottoHistoryRepository
                    .findByDrawNoAndDeleteYnAndUseYn(drawNo, "N", "Y")
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "당첨번호를 찾을 수 없습니다: " + drawNo));

            // 2. 모든 활성 사용자 번호 조회 (시작 회차 필터링)
            List<UserPrediction> activePredictions = predictionRepository
                    .findByDeleteYnAndStartDrawIdLessThanEqual("N", drawNo);

            if (activePredictions.isEmpty()) {
                log.info("매칭할 사용자 번호가 없습니다.");
                return;
            }

            log.info("매칭 대상 사용자 번호: {}개", activePredictions.size());

            // 3. 각 사용자 번호와 매칭 계산
            List<PredictionsHistory> historyList = new ArrayList<>();
            List<UserPrediction> winningPredictions = new ArrayList<>(); // 당첨된 예측 저장
            List<DrawMatchResult> winningResults = new ArrayList<>(); // 당첨 결과 저장

            for (UserPrediction prediction : activePredictions) {
                // 내 번호 리스트
                List<Integer> myNumbers = Arrays.asList(
                        prediction.getPredictedNum1().intValue(),
                        prediction.getPredictedNum2().intValue(),
                        prediction.getPredictedNum3().intValue(),
                        prediction.getPredictedNum4().intValue(),
                        prediction.getPredictedNum5().intValue(),
                        prediction.getPredictedNum6().intValue()
                );

                // 매칭 계산 (기존 메서드 재사용)
                DrawMatchResult result = calculateDrawMatchForHistory(myNumbers, newDraw);

                // 꽝이면 PREDICTIONS_HISTORY 저장 안함 (하지만 통계는 업데이트 필요)
                if (result.getRank() == null) {
                    // 꽝인 경우에도 totalDraws는 증가해야 하므로 통계 업데이트
                    updateStatsForNonWinningPrediction(prediction, newDraw);
                    continue;
                }

                // PredictionsHistory 생성
                PredictionsHistory history = PredictionsHistory.builder()
                        .predictionId(prediction.getPredictionId())
                        .historyId(newDraw.getHistoryId())
                        .userId(prediction.getUser().getUserId())
                        .drawNo(newDraw.getDrawNo())
                        .rank(result.getRank())
                        .hasBonus(result.getHasBonus())
                        .matchedCount(result.getMatchCount().shortValue())
                        .prizeAmount(result.getPrizeAmount().intValue())
                        .startDrawSortation("current") // 구분값 (past: 등록시, current: 업데이트시)
                        .build();

                historyList.add(history);
                winningPredictions.add(prediction);
                winningResults.add(result);
            }

            // 4. PREDICTIONS_HISTORY 배치 저장
            if (!historyList.isEmpty()) {
                predictionsHistoryRepository.saveAll(historyList);
                log.info("{}회차 매칭 결과 {}건 저장 완료", drawNo, historyList.size());
                
                // 5. 당첨된 예측의 통계 테이블 업데이트
                updateStatsForWinningPredictions(winningPredictions, historyList, newDraw);
            } else {
                log.info("{}회차에 당첨된 사용자가 없습니다.", drawNo);
            }

        } catch (Exception e) {
            log.error("사용자 번호 매칭 중 오류 발생: {}회차", drawNo, e);
            throw e; // 트랜잭션 롤백
        }
    }

    /**
     * 당첨된 예측들의 통계 테이블 업데이트
     */
    private void updateStatsForWinningPredictions(
            List<UserPrediction> winningPredictions,
            List<PredictionsHistory> historyList,
            LottoHistory newDraw) {
        
        for (int i = 0; i < winningPredictions.size(); i++) {
            UserPrediction prediction = winningPredictions.get(i);
            PredictionsHistory history = historyList.get(i);
            
            // HISTORICAL_STATS 업데이트 (전체 회차 기준)
            updateHistoricalStats(prediction, history, newDraw);
            
            // TRACKING_STATS 업데이트 (시작 회차 이후 기준)
            updateTrackingStats(prediction, history, newDraw);
        }
    }

    /**
     * 꽝인 예측의 통계 업데이트 (totalDraws만 증가)
     */
    private void updateStatsForNonWinningPrediction(
            UserPrediction prediction,
            LottoHistory newDraw) {
        
        // HISTORICAL_STATS 업데이트
        UserPredictionHistoricalStats historicalStats = userPredictionHistoricalStatsRepository
                .findByPredictionId(prediction.getPredictionId())
                .orElseGet(() -> {
                    return UserPredictionHistoricalStats.builder()
                            .userPrediction(prediction)
                            .totalDraws(0)
                            .winningDraws(0)
                            .totalPrizeAmount(0L)
                            .rank1Count(0)
                            .rank2Count(0)
                            .rank3Count(0)
                            .rank4Count(0)
                            .rank5Count(0)
                            .build();
                });
        
        historicalStats.setTotalDraws(newDraw.getDrawNo());
        long totalInvestment = historicalStats.getTotalDraws() * 1000L;
        historicalStats.setReturnRate(totalInvestment > 0 
                ? (double) historicalStats.getTotalPrizeAmount() / totalInvestment * 100 
                : 0.0);
        userPredictionHistoricalStatsRepository.save(historicalStats);
        
        // TRACKING_STATS 업데이트
        Integer startDrawNo = prediction.getStartDrawId() != null 
                ? prediction.getStartDrawId() 
                : 1;
        
        UserPredictionTrackingStats trackingStats = userPredictionTrackingStatsRepository
                .findByPredictionId(prediction.getPredictionId())
                .orElseGet(() -> {
                    return UserPredictionTrackingStats.builder()
                            .userPrediction(prediction)
                            .totalDraws(0)
                            .winningDraws(0)
                            .totalPrizeAmount(0L)
                            .rank1Count(0)
                            .rank2Count(0)
                            .rank3Count(0)
                            .rank4Count(0)
                            .rank5Count(0)
                            .build();
                });
        
        trackingStats.setTotalDraws(newDraw.getDrawNo() - startDrawNo + 1);
        long trackingInvestment = trackingStats.getTotalDraws() * 1000L;
        trackingStats.setReturnRate(trackingInvestment > 0 
                ? (double) trackingStats.getTotalPrizeAmount() / trackingInvestment * 100 
                : 0.0);
        userPredictionTrackingStatsRepository.save(trackingStats);
    }

    /**
     * USER_PREDICTION_HISTORICAL_STATS 업데이트
     */
    private void updateHistoricalStats(
            UserPrediction prediction,
            PredictionsHistory history,
            LottoHistory newDraw) {
        
        UserPredictionHistoricalStats stats = userPredictionHistoricalStatsRepository
                .findByPredictionId(prediction.getPredictionId())
                .orElseGet(() -> {
                    // 없으면 새로 생성 (초기값 설정)
                    return UserPredictionHistoricalStats.builder()
                            .userPrediction(prediction)
                            .totalDraws(0)
                            .winningDraws(0)
                            .totalPrizeAmount(0L)
                            .rank1Count(0)
                            .rank2Count(0)
                            .rank3Count(0)
                            .rank4Count(0)
                            .rank5Count(0)
                            .build();
                });
        
        // 통계 업데이트
        stats.setTotalDraws(newDraw.getDrawNo()); // 전체 회차 수
        stats.setWinningDraws((stats.getWinningDraws() != null ? stats.getWinningDraws() : 0) + 1);
        stats.setTotalPrizeAmount((stats.getTotalPrizeAmount() != null ? stats.getTotalPrizeAmount() : 0L) 
                + history.getPrizeAmount().longValue());
        
        // 등수별 카운트 증가
        Integer rank = history.getRank();
        if (rank != null) {
            switch (rank) {
                case 1 -> stats.setRank1Count((stats.getRank1Count() != null ? stats.getRank1Count() : 0) + 1);
                case 2 -> stats.setRank2Count((stats.getRank2Count() != null ? stats.getRank2Count() : 0) + 1);
                case 3 -> stats.setRank3Count((stats.getRank3Count() != null ? stats.getRank3Count() : 0) + 1);
                case 4 -> stats.setRank4Count((stats.getRank4Count() != null ? stats.getRank4Count() : 0) + 1);
                case 5 -> stats.setRank5Count((stats.getRank5Count() != null ? stats.getRank5Count() : 0) + 1);
            }
            
            // 최고 등수 업데이트
            if (stats.getBestRank() == null || rank < stats.getBestRank()) {
                stats.setBestRank(rank);
                stats.setBestDrawNo(newDraw.getDrawNo());
            }
        }
        
        // 수익률 재계산
        long totalInvestment = stats.getTotalDraws() * 1000L;
        stats.setReturnRate(totalInvestment > 0 
                ? (double) stats.getTotalPrizeAmount() / totalInvestment * 100 
                : 0.0);
        
        userPredictionHistoricalStatsRepository.save(stats);
    }

    /**
     * USER_PREDICTION_TRACKING_STATS 업데이트
     */
    private void updateTrackingStats(
            UserPrediction prediction,
            PredictionsHistory history,
            LottoHistory newDraw) {
        
        UserPredictionTrackingStats stats = userPredictionTrackingStatsRepository
                .findByPredictionId(prediction.getPredictionId())
                .orElseGet(() -> {
                    // 없으면 새로 생성
                    return UserPredictionTrackingStats.builder()
                            .userPrediction(prediction)
                            .totalDraws(0)
                            .winningDraws(0)
                            .totalPrizeAmount(0L)
                            .rank1Count(0)
                            .rank2Count(0)
                            .rank3Count(0)
                            .rank4Count(0)
                            .rank5Count(0)
                            .build();
                });
        
        // 시작 회차 이후 회차 수 계산
        Integer startDrawNo = prediction.getStartDrawId() != null 
                ? prediction.getStartDrawId() 
                : 1;
        stats.setTotalDraws(newDraw.getDrawNo() - startDrawNo + 1);
        
        // 통계 업데이트
        stats.setWinningDraws((stats.getWinningDraws() != null ? stats.getWinningDraws() : 0) + 1);
        stats.setTotalPrizeAmount((stats.getTotalPrizeAmount() != null ? stats.getTotalPrizeAmount() : 0L) 
                + history.getPrizeAmount().longValue());
        
        // 등수별 카운트 증가
        Integer rank = history.getRank();
        if (rank != null) {
            switch (rank) {
                case 1 -> stats.setRank1Count((stats.getRank1Count() != null ? stats.getRank1Count() : 0) + 1);
                case 2 -> stats.setRank2Count((stats.getRank2Count() != null ? stats.getRank2Count() : 0) + 1);
                case 3 -> stats.setRank3Count((stats.getRank3Count() != null ? stats.getRank3Count() : 0) + 1);
                case 4 -> stats.setRank4Count((stats.getRank4Count() != null ? stats.getRank4Count() : 0) + 1);
                case 5 -> stats.setRank5Count((stats.getRank5Count() != null ? stats.getRank5Count() : 0) + 1);
            }
            
            // 최고 등수 업데이트
            if (stats.getBestRank() == null || rank < stats.getBestRank()) {
                stats.setBestRank(rank);
                stats.setBestDrawNo(newDraw.getDrawNo());
            }
        }
        
        // 수익률 재계산
        long totalInvestment = stats.getTotalDraws() * 1000L;
        stats.setReturnRate(totalInvestment > 0 
                ? (double) stats.getTotalPrizeAmount() / totalInvestment * 100 
                : 0.0);
        
        userPredictionTrackingStatsRepository.save(stats);
    }

}