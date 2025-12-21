package hhammong.apilotto.dto;

import hhammong.apilotto.entity.LottoHistory;
import lombok.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LottoHistoryResponse {

    private Integer drawNo;           // 회차
    private LocalDate drawDate;       // 추첨날짜
    private Integer number1;
    private Integer number2;
    private Integer number3;
    private Integer number4;
    private Integer number5;
    private Integer number6;
    private List<Integer> numbers;    // 당첨번호 [1,2,3,4,5,6]
    private Integer bonusNumber;      // 보너스번호
    private Long prize1st;            // 1등 당첨금
    private Long prize2nd;
    private Long prize3rd;
    private Integer prize4th;
    private Integer prize5th;

    // Entity -> DTO 변환
    public static LottoHistoryResponse from(LottoHistory entity) {
        return LottoHistoryResponse.builder()
                .drawNo(entity.getDrawNo())
                .drawDate(entity.getDrawDate())
                .number1(entity.getNumber1().intValue())
                .number2(entity.getNumber2().intValue())
                .number3(entity.getNumber3().intValue())
                .number4(entity.getNumber4().intValue())
                .number5(entity.getNumber5().intValue())
                .number6(entity.getNumber6().intValue())
                .numbers(Arrays.asList(
                        entity.getNumber1().intValue(),
                        entity.getNumber2().intValue(),
                        entity.getNumber3().intValue(),
                        entity.getNumber4().intValue(),
                        entity.getNumber5().intValue(),
                        entity.getNumber6().intValue()
                ))
                .bonusNumber(entity.getBonusNumber().intValue())
                .prize1st(entity.getPrize1st())
                .prize2nd(entity.getPrize2nd())
                .prize3rd(entity.getPrize3rd())
                .prize4th(entity.getPrize4th())
                .prize5th(entity.getPrize5th())
                .build();
    }
}