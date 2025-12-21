package hhammong.apilotto.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "`LOTTO_HISTORY`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LottoHistory {

    @Id
    @Column(name = "`HISTORY_ID`")
    private UUID historyId;

    @Column(name = "`DRAW_NO`", nullable = false, unique = true)
    private Integer drawNo;  // 회차

    @Column(name = "`DRAW_DATE`", nullable = false)
    private LocalDate drawDate;  // 추첨날짜

    @Column(name = "`NUMBER1`", nullable = false)
    private Short number1;

    @Column(name = "`NUMBER2`", nullable = false)
    private Short number2;

    @Column(name = "`NUMBER3`", nullable = false)
    private Short number3;

    @Column(name = "`NUMBER4`", nullable = false)
    private Short number4;

    @Column(name = "`NUMBER5`", nullable = false)
    private Short number5;

    @Column(name = "`NUMBER6`", nullable = false)
    private Short number6;

    @Column(name = "`BONUS_NUMBER`", nullable = false)
    private Short bonusNumber;

    @Column(name = "`NUMBERS`")
    private String numbers;  // 전체번호배열 (예: "1,2,3,4,5,6")

    @Column(name = "`PRIZE_1ST`")
    private Long prize1st;  // 1등 당첨금

    @Column(name = "`PRIZE_2ND`")
    private Long prize2nd;

    @Column(name = "`PRIZE_3RD`")
    private Long prize3rd;

    @Column(name = "`PRIZE_4TH`")
    private Integer prize4th;

    @Column(name = "`PRIZE_5TH`")
    private Integer prize5th;

    @CreationTimestamp
    @Column(name = "`CREATED_AT`", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "`UPDATED_AT`", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "`DELETE_YN`", length = 1)
    @ColumnDefault("'N'")
    private String deleteYn = "N";

    @Column(name = "`USE_YN`", length = 1)
    @ColumnDefault("'Y'")
    private String useYn = "Y";

    @PrePersist
    public void prePersist() {
        if (this.historyId == null) {
            this.historyId = UUID.randomUUID();
        }
    }
}