package hhammong.apilotto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "USER_PREDICTION_HISTORICAL_STATS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPredictionHistoricalStats {

    @Id
    @Column(name = "PREDICTION_ID")
    private UUID predictionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "PREDICTION_ID")
    private UserPrediction userPrediction;

    @Column(name = "TOTAL_DRAWS")
    private Integer totalDraws;  // 총 참여 횟수

    @Column(name = "WINNING_DRAWS")
    private Integer winningDraws;  // 당첨횟수

    @Column(name = "TOTAL_PRIZE_AMOUNT")
    private Long totalPrizeAmount;  // 총 당첨금

    @Column(name = "BEST_RANK")
    private Integer bestRank;  // 최고 등수

    @Column(name = "BEST_DRAW_NO")
    @Builder.Default
    private Integer bestDrawNo = 0;  // 최고등수회차

    @Column(name = "RETURN_RATE")
    private Double returnRate;  // 수익률

    @Column(name = "RANK_1_COUNT")
    @Builder.Default
    private Integer rank1Count = 0;  // 1등 당첨 횟수

    @Column(name = "RANK_2_COUNT")
    @Builder.Default
    private Integer rank2Count = 0;  // 2등 당첨 횟수

    @Column(name = "RANK_3_COUNT")
    @Builder.Default
    private Integer rank3Count = 0;  // 3등 당첨 횟수

    @Column(name = "RANK_4_COUNT")
    @Builder.Default
    private Integer rank4Count = 0;  // 4등 당첨 횟수

    @Column(name = "RANK_5_COUNT")
    @Builder.Default
    private Integer rank5Count = 0;  // 5등 당첨 횟수

    // 수익률 계산 메서드
    /*public void calculateReturnRate() {
        if (totalDraws != null && totalDraws > 0) {
            long totalInvestment = totalDraws * 1000L;  // 로또 한 게임당 1000원
            if (totalPrizeAmount != null) {
                this.returnRate = (double) totalPrizeAmount / totalInvestment * 100;
            }
        }
    }

    // 통계 업데이트 메서드
    public void updateStatistics(int rank, long prizeAmount) {
        if (this.totalDraws == null) this.totalDraws = 0;
        if (this.winningDraws == null) this.winningDraws = 0;
        if (this.totalPrizeAmount == null) this.totalPrizeAmount = 0L;

        this.totalDraws++;

        if (rank > 0 && rank <= 5) {
            this.winningDraws++;
            this.totalPrizeAmount += prizeAmount;

            // 등수별 카운트 증가
            switch (rank) {
                case 1 -> this.rank1Count++;
                case 2 -> this.rank2Count++;
                case 3 -> this.rank3Count++;
                case 4 -> this.rank4Count++;
                case 5 -> this.rank5Count++;
            }

            // 최고 등수 업데이트 (낮은 숫자가 높은 등수)
            if (this.bestRank == null || rank < this.bestRank) {
                this.bestRank = rank;
            }
        }

        calculateReturnRate();
    }

    @PrePersist
    protected void onCreate() {
        if (this.totalDraws == null) this.totalDraws = 0;
        if (this.winningDraws == null) this.winningDraws = 0;
        if (this.totalPrizeAmount == null) this.totalPrizeAmount = 0L;
        if (this.rank1Count == null) this.rank1Count = 0;
        if (this.rank2Count == null) this.rank2Count = 0;
        if (this.rank3Count == null) this.rank3Count = 0;
        if (this.rank4Count == null) this.rank4Count = 0;
        if (this.rank5Count == null) this.rank5Count = 0;
    }*/

}
