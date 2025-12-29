package hhammong.apilotto.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckWinningResponse {

    // 기본 정보
    private Integer drawNo;
    private List<Integer> myNumbers;
    private List<Integer> winningNumbers;
    private Integer bonusNumber;

    // 매칭 결과
    private Integer matchCount;       // 일치 개수
    private Boolean hasBonus;         // 보너스 일치 여부
    private Integer rank;             // 등수 (null이면 꽝)
    private String rankDescription;   // "3등", "꽝" 등
    private Long prizeAmount;         // 당첨금 (해당 회차 기준)

    // 메시지
    private String message;
}
