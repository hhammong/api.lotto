package hhammong.apilotto.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrawMatchResult {

    // 회차 정보
    private Integer drawNo;
    private LocalDate drawDate;
    private List<Integer> winningNumbers;
    private Integer bonusNumber;

    // 매칭 결과
    private Integer matchCount;
    private Boolean hasBonus;
    private Integer rank;
    private String rankDescription;
    private Long prizeAmount;

    // 일치한 번호들
    private List<Integer> matchedNumbers;
}