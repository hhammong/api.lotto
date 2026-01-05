package hhammong.apilotto.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "USER_PREDICTIONS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPrediction {

    @Id
    @Column(name = "PREDICTION_ID")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID predictionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(name = "TARGET_DRAW_NO")
    private Integer targetDrawNo;  // 예측회차

    @Column(name = "PREDICTED_NUM1", nullable = false)
    private Short predictedNum1;  // 예측번호1

    @Column(name = "PREDICTED_NUM2", nullable = false)
    private Short predictedNum2;  // 예측번호2

    @Column(name = "PREDICTED_NUM3", nullable = false)
    private Short predictedNum3;  // 예측번호3

    @Column(name = "PREDICTED_NUM4", nullable = false)
    private Short predictedNum4;  // 예측번호4

    @Column(name = "PREDICTED_NUM5", nullable = false)
    private Short predictedNum5;  // 예측번호5

    @Column(name = "PREDICTED_NUM6", nullable = false)
    private Short predictedNum6;  // 예측번호6

    @Column(name = "PREDICTED_NUMBERS", nullable = false, length = 255)
    private String predictedNumbers;  // 예측번호배열

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;  // 생성일

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;  // 수정일

    @Column(name = "DELETE_YN", length = 1)
    @ColumnDefault("N")
    @Builder.Default
    private String deleteYn = "N";

    @Column(name = "USE_YN", length = 1)
    @ColumnDefault("Y")
    @Builder.Default
    private String useYn = "Y";

    @Column(name = "MEMO", length = 200)
    private String memo;  // 사용자 메모

    @Column(name = "START_DRAW_ID")
    private Integer startDrawId;  // 시작 회차

    @OneToOne(mappedBy = "userPrediction")
    private UserPredictionHistoricalStats userPredictionHistoricalStat;

    @OneToOne(mappedBy = "userPrediction")
    private UserPredictionTrackingStats userPredictionTrackingStat;

    // 번호 배열 문자열 자동 생성
    public void generatePredictedNumbersString() {
        this.predictedNumbers = String.format("%d,%d,%d,%d,%d,%d",
                predictedNum1, predictedNum2, predictedNum3,
                predictedNum4, predictedNum5, predictedNum6);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (this.deleteYn == null) this.deleteYn = "N";
        if (this.useYn == null) this.useYn = "Y";

        generatePredictedNumbersString();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        generatePredictedNumbersString();
    }
}