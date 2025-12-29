package hhammong.apilotto.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckWinningRequest {
    private List<Integer> myNumbers;  // 내 번호
    private Integer drawNo;           // 확인할 회차 (선택, 없으면 최신)
}
