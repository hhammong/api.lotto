package hhammong.apilotto.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionHistoryResponse {

    // 내 번호 정보
    private UUID predictionId;
    private List<Integer> myNumbers;
    private String memo;
    private LocalDateTime createdAt;
    private Integer startDrawNo;  // 시작 회차

    // 회차별 상세 이력
    private List<DrawMatchResult> history;

    // 전체 통계
    private Integer totalDraws;        // 총 참여 회차
    private Integer winningDraws;      // 당첨된 회차

    // 등수별 통계
    private Integer rank1Count;
    private Integer rank2Count;
    private Integer rank3Count;
    private Integer rank4Count;
    private Integer rank5Count;

    // 금액 통계
    private Long totalPrizeAmount;     // 총 당첨금
    private Long totalInvestment;      // 총 투자금 (회차당 1,000원)
    private Long netProfit;            // 순수익 (당첨금 - 투자금)
    private Double returnRate;         // 수익률 (%)

    // 최고 기록
    private Integer bestRank;          // 최고 등수
    private Integer bestDrawNo;        // 최고 등수 회차

    // 요약 메시지
    private String summaryMessage;
}