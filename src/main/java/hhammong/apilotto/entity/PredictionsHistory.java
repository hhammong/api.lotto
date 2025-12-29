package hhammong.apilotto.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "`PREDICTIONS_HISTORY`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionsHistory {

    @Id
    @Column(name = "`PREDICTIONS_HISTORY_ID`")
    private UUID predictionsHistoryId;

    @Column(name = "`PREDICTION_ID`", nullable = false)
    private UUID predictionId;  // 예측번호

    @Column(name = "`HISTORY_ID`", nullable = false)
    private UUID historyId;  // 역대번호

    @Column(name = "`USER_ID`", nullable = false)
    private UUID userId;  // 사용자

    @Column(name = "`RANK`", nullable = false)
    private Integer rank;  // 당첨등수 (null이면 꽝)

    @Column(name = "`HAS_BONUS`")
    @ColumnDefault("false")
    @Builder.Default
    private Boolean hasBonus = false;  // 보너스 번호 일치 여부 (2,3등 구분)

    @Column(name = "`MATCHED_COUNT`", nullable = false)
    private Short matchedCount;  // 일치개수

    @Column(name = "`PRIZE_AMOUNT`")
    @ColumnDefault("0")
    @Builder.Default
    private Integer prizeAmount = 0;  // 해당 회차 당첨금

    @CreationTimestamp
    @Column(name = "`CREATED_AT`", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성일

    @PrePersist
    public void prePersist() {
        if (this.predictionsHistoryId == null) {
            this.predictionsHistoryId = UUID.randomUUID();
        }
    }
}