package hhammong.apilotto.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllNumbersCheckResponse {

    // 확인한 회차 정보
    private Integer drawNo;
    private LocalDate drawDate;
    private List<Integer> winningNumbers;
    private Integer bonusNumber;

    // 전체 번호 결과
    private List<MyNumberCheckResult> results;

    // 요약 통계
    private Integer totalCount;           // 총 번호 개수
    private Integer winningCount;         // 당첨된 번호 개수
    private Long totalPrizeAmount;        // 총 당첨금

    // 등수별 카운트
    private Integer rank1Count;
    private Integer rank2Count;
    private Integer rank3Count;
    private Integer rank4Count;
    private Integer rank5Count;

    // 메시지
    private String summaryMessage;
}