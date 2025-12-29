package hhammong.apilotto.dto;

import lombok.*;
import java.time.LocalDate;

/**
 * 로또 웹 스크래핑용 DTO
 * - 동행복권 사이트에서 크롤링한 원본 데이터
 * - LottoHistory 엔티티로 변환 전 임시 데이터 홀더
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LottoScrapingDTO {

    // 기본 정보
    private Integer drawNo;        // 회차
    private LocalDate drawDate;    // 추첨일

    // 당첨 번호
    private Short number1;
    private Short number2;
    private Short number3;
    private Short number4;
    private Short number5;
    private Short number6;
    private Short bonusNumber;

    // 당첨금 (1인당)
    private Long prize1st;         // 1등
    private Long prize2nd;         // 2등
    private Long prize3rd;         // 3등
    private Integer prize4th;      // 4등
    private Integer prize5th;      // 5등

}