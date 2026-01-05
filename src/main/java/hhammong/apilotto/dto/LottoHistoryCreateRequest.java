package hhammong.apilotto.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LottoHistoryCreateRequest {

    private Integer drawNo;

    private LocalDate drawDate;

    private Short number1;

    private Short number2;

    private Short number3;

    private Short number4;

    private Short number5;

    private Short number6;

    private Short bonusNumber;

    // 선택 필드들
    private String numbers;  // 전체번호배열 (서버에서 자동 생성 가능)

    private Long prize1st;
    private Long prize2nd;
    private Long prize3rd;
    private Integer prize4th;
    private Integer prize5th;

    // CREATED_AT, UPDATED_AT, DELETE_YN, USE_YN은 서버에서 자동 설정
}