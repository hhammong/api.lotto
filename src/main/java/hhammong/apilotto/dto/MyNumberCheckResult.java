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
public class MyNumberCheckResult {

    // 내 번호 정보
    private UUID predictionId;
    private List<Integer> myNumbers;
    private String memo;
    private LocalDateTime createdAt;

    // 매칭 결과
    private Integer matchCount;       // 일치 개수
    private Boolean hasBonus;         // 보너스 일치 여부
    private Integer rank;             // 등수 (null이면 꽝)
    private String rankDescription;   // "3등", "꽝"
    private Long prizeAmount;         // 당첨금

    // 당첨 여부
    private Boolean isWinning;
}