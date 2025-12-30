package hhammong.apilotto.service;

import hhammong.apilotto.dto.AllNumbersCheckResponse;
import hhammong.apilotto.dto.DrawMatchResult;
import hhammong.apilotto.dto.MyNumberCheckResult;
import hhammong.apilotto.dto.PredictionHistoryResponse;
import hhammong.apilotto.entity.LottoHistory;
import hhammong.apilotto.entity.PredictionsHistory;
import hhammong.apilotto.entity.UserPrediction;
import hhammong.apilotto.exception.ResourceNotFoundException;
import hhammong.apilotto.repository.LottoHistoryRepository;
import hhammong.apilotto.repository.PredictionsHistoryRepository;
import hhammong.apilotto.repository.UserPredictionRepository;
import hhammong.apilotto.util.LottoMatchUtils;
import hhammong.apilotto.util.LottoMatchUtils.MatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPredictionCheckService {

    private final UserPredictionRepository predictionRepository;
    private final LottoHistoryRepository lottoHistoryRepository;
    private final PredictionsHistoryRepository predictionsHistoryRepository;

    /**
     * 내가 등록한 모든 번호를 최신 회차와 비교
     */
    public AllNumbersCheckResponse checkAllMyNumbers(UUID userId) {
        // 1. 최신 회차 조회
        LottoHistory latestDraw = lottoHistoryRepository
                .findTopByDeleteYnAndUseYnOrderByDrawNoDesc("N", "Y")
                .orElseThrow(() -> new ResourceNotFoundException("당첨번호가 존재하지 않습니다"));

        // 2. 당첨번호 리스트 생성
        List<Integer> winningNumbers = Arrays.asList(
                latestDraw.getNumber1().intValue(),
                latestDraw.getNumber2().intValue(),
                latestDraw.getNumber3().intValue(),
                latestDraw.getNumber4().intValue(),
                latestDraw.getNumber5().intValue(),
                latestDraw.getNumber6().intValue()
        );

        // 3. 내 번호 목록 조회
        List<UserPrediction> myPredictions = predictionRepository.findByUser_UserIdAndDeleteYnOrderByCreatedAtDesc(userId, "N");

        // 4. 각 번호마다 매칭 계산
        List<MyNumberCheckResult> results = myPredictions.stream()
                .map(prediction -> checkSingleNumber(prediction, winningNumbers,
                        latestDraw.getBonusNumber().intValue(), latestDraw))
                .collect(Collectors.toList());

        // 5. 통계 계산
        return buildResponse(latestDraw, winningNumbers, results);
    }

    /**
     * 개별 번호 매칭 계산
     */
    private MyNumberCheckResult checkSingleNumber(
            UserPrediction prediction,
            List<Integer> winningNumbers,
            Integer bonusNumber,
            LottoHistory draw) {

        // 내 번호 리스트
        List<Integer> myNumbers = Arrays.asList(
                prediction.getPredictedNum1().intValue(),
                prediction.getPredictedNum2().intValue(),
                prediction.getPredictedNum3().intValue(),
                prediction.getPredictedNum4().intValue(),
                prediction.getPredictedNum5().intValue(),
                prediction.getPredictedNum6().intValue()
        );

        // 매칭 계산
        MatchResult matchResult = LottoMatchUtils.calculateMatch(
                myNumbers, winningNumbers, bonusNumber);

        // 당첨금 계산
        Long prizeAmount = getPrizeAmount(draw, matchResult.getRank());

        return MyNumberCheckResult.builder()
                .predictionId(prediction.getPredictionId())
                .myNumbers(myNumbers)
                .memo(prediction.getMemo())
                .createdAt(prediction.getCreatedAt())
                .matchCount(matchResult.getMatchCount())
                .hasBonus(matchResult.isHasBonus())
                .rank(matchResult.getRank())
                .rankDescription(matchResult.getRankDescription())
                .prizeAmount(prizeAmount)
                .isWinning(matchResult.isWinning())
                .build();
    }

    /**
     * 등수별 당첨금 조회
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
     * 응답 생성 (통계 포함)
     */
    private AllNumbersCheckResponse buildResponse(
            LottoHistory draw,
            List<Integer> winningNumbers,
            List<MyNumberCheckResult> results) {

        // 통계 계산
        int totalCount = results.size();
        int winningCount = (int) results.stream().filter(MyNumberCheckResult::getIsWinning).count();
        long totalPrize = results.stream().mapToLong(MyNumberCheckResult::getPrizeAmount).sum();

        int rank1 = (int) results.stream().filter(r -> Integer.valueOf(1).equals(r.getRank())).count();
        int rank2 = (int) results.stream().filter(r -> Integer.valueOf(2).equals(r.getRank())).count();
        int rank3 = (int) results.stream().filter(r -> Integer.valueOf(3).equals(r.getRank())).count();
        int rank4 = (int) results.stream().filter(r -> Integer.valueOf(4).equals(r.getRank())).count();
        int rank5 = (int) results.stream().filter(r -> Integer.valueOf(5).equals(r.getRank())).count();

        // 메시지 생성
        String message = generateSummaryMessage(totalCount, winningCount, totalPrize,
                rank1, rank2, rank3, rank4, rank5);

        return AllNumbersCheckResponse.builder()
                .drawNo(draw.getDrawNo())
                .drawDate(draw.getDrawDate())
                .winningNumbers(winningNumbers)
                .bonusNumber(draw.getBonusNumber().intValue())
                .results(results)
                .totalCount(totalCount)
                .winningCount(winningCount)
                .totalPrizeAmount(totalPrize)
                .rank1Count(rank1)
                .rank2Count(rank2)
                .rank3Count(rank3)
                .rank4Count(rank4)
                .rank5Count(rank5)
                .summaryMessage(message)
                .build();
    }

    /**
     * 요약 메시지 생성
     */
    private String generateSummaryMessage(int total, int winning, long prize,
                                          int r1, int r2, int r3, int r4, int r5) {
        if (total == 0) {
            return "등록된 번호가 없습니다.";
        }

        if (winning == 0) {
            return String.format("총 %d개 번호 중 당첨된 번호가 없습니다.", total);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("총 %d개 번호 중 %d개 당첨! ", total, winning));

        if (r1 > 0) sb.append(String.format("1등 %d개, ", r1));
        if (r2 > 0) sb.append(String.format("2등 %d개, ", r2));
        if (r3 > 0) sb.append(String.format("3등 %d개, ", r3));
        if (r4 > 0) sb.append(String.format("4등 %d개, ", r4));
        if (r5 > 0) sb.append(String.format("5등 %d개, ", r5));

        // 마지막 쉼표 제거
        if (sb.charAt(sb.length() - 2) == ',') {
            sb.setLength(sb.length() - 2);
        }

        sb.append(String.format(" 총 당첨금: %,d원", prize));

        return sb.toString();
    }

    /**
     * PredictionHistory 테이블 특정 번호의 전체 이력 조회
     */
    public PredictionHistoryResponse getPredictionHistory2(UUID userId, UUID predictionId) {
        // 1. 내 번호 조회
        UserPrediction prediction = predictionRepository
                .findByPredictionIdAndUser_UserIdAndDeleteYn(predictionId, userId, "N")
                .orElseThrow(() -> new ResourceNotFoundException("해당 번호를 찾을 수 없습니다"));

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

        // ✨ 4. PREDICTIONS_HISTORY에서 당첨 이력만 조회!
        List<PredictionsHistory> winningHistories = predictionsHistoryRepository
                .findByPredictionIdOrderByDrawNoAsc(predictionId);

        // 5. DrawMatchResult로 변환 (LOTTO_HISTORY 조인 필요)
        List<DrawMatchResult> history = winningHistories.stream()
                .map(this::convertToDrawMatchResult)
                .sorted(Comparator.comparing(DrawMatchResult::getDrawNo).reversed())
                .collect(Collectors.toList());

        // 6. 통계 계산 및 응답 생성
        return buildHistoryResponse(prediction, myNumbers, startDrawNo, history);
    }

    /**
     * PredictionsHistory → DrawMatchResult 변환
     */
    private DrawMatchResult convertToDrawMatchResult(PredictionsHistory ph) {
        // LOTTO_HISTORY에서 회차 정보 조회 (캐싱 권장)
        LottoHistory draw = lottoHistoryRepository.findById(ph.getHistoryId())
                .orElseThrow(() -> new ResourceNotFoundException("회차 정보를 찾을 수 없습니다"));

        List<Integer> winningNumbers = Arrays.asList(
                draw.getNumber1().intValue(),
                draw.getNumber2().intValue(),
                draw.getNumber3().intValue(),
                draw.getNumber4().intValue(),
                draw.getNumber5().intValue(),
                draw.getNumber6().intValue()
        );

        return DrawMatchResult.builder()
                .drawNo(draw.getDrawNo())
                .drawDate(draw.getDrawDate())
                .winningNumbers(winningNumbers)
                .bonusNumber(draw.getBonusNumber().intValue())
                .matchCount(ph.getMatchedCount().intValue())
                .hasBonus(ph.getHasBonus())
                .rank(ph.getRank())
                .rankDescription(getRankDescription(ph.getRank()))
                .prizeAmount(ph.getPrizeAmount().longValue())
                .matchedNumbers(List.of())  // 필요시 계산
                .build();
    }

    private String getRankDescription(Integer rank) {
        if (rank == null) return "꽝";
        return switch (rank) {
            case 1 -> "1등";
            case 2 -> "2등";
            case 3 -> "3등";
            case 4 -> "4등";
            case 5 -> "5등";
            default -> "꽝";
        };
    }

    /**
     * 특정 번호의 전체 이력 조회
     */
    public PredictionHistoryResponse getPredictionHistory(UUID userId, UUID predictionId) {
        // 1. 내 번호 조회
        UserPrediction prediction = predictionRepository
                .findByPredictionIdAndUser_UserIdAndDeleteYn(predictionId, userId, "N")
                .orElseThrow(() -> new ResourceNotFoundException("해당 번호를 찾을 수 없습니다"));

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
                .map(draw -> calculateDrawMatch(myNumbers, draw))
                .collect(Collectors.toList());

        // 6. 통계 계산 및 응답 생성
        return buildHistoryResponse(prediction, myNumbers, startDrawNo, history);
    }

    /**
     * 시작 회차 결정
     * - startDrawId가 있으면 사용
     * - 없으면 등록일 기준 다음 회차
     * - 둘 다 없으면 1회부터
     */
    private Integer determineStartDrawNo(UserPrediction prediction) {
        if (prediction.getStartDrawId() != null) {
            return prediction.getStartDrawId();
        }

        // 등록일 기준 다음 회차 찾기 (간단하게 1회부터)
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
     * 특정 회차와 내 번호 매칭 계산
     */
    private DrawMatchResult calculateDrawMatch(List<Integer> myNumbers, LottoHistory draw) {
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
        MatchResult matchResult = LottoMatchUtils.calculateMatch(
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
        /*int totalDraws = history.size();
        int winningDraws = (int) history.stream()
                .filter(h -> h.getRank() != null)
                .count();*/

        int totalDraws = getTotalDrawsSinceStart(startDrawNo);  // 전체 참여 회차
        int winningDraws = history.size();  // 당첨 회차 = history 개수

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
}