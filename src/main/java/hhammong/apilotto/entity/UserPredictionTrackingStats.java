package hhammong.apilotto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "USER_PREDICTION_TRACKING_STATS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPredictionTrackingStats {

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
    private Integer rank1Count;  // 1등 당첨 횟수

    @Column(name = "RANK_2_COUNT")
    private Integer rank2Count;  // 2등 당첨 횟수

    @Column(name = "RANK_3_COUNT")
    private Integer rank3Count;  // 3등 당첨 횟수

    @Column(name = "RANK_4_COUNT")
    private Integer rank4Count;  // 4등 당첨 횟수

    @Column(name = "RANK_5_COUNT")
    private Integer rank5Count;  // 5등 당첨 횟수
}