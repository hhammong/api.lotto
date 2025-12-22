package hhammong.apilotto.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPredictionCreateRequest {

    // 사용자가 보내는 건 이것만!
    private List<Integer> numbers;  // [3, 1, 6, 4, 5, 2]

    private String memo;  // 선택사항
    private Integer targetDrawNo;  // 선택사항

    // 중복 번호 검증
    public boolean hasDuplicates() {
        if (numbers == null || numbers.isEmpty()) {
            return false;
        }
        return numbers.stream().distinct().count() != numbers.size();
    }

    // 정렬된 번호 반환
    public List<Integer> getSortedNumbers() {
        if (numbers == null) {
            return List.of();
        }
        return numbers.stream().sorted().toList();
    }

    // 수동 검증 (validation 의존성 없을 때)
    public void validate() {
        if (numbers == null || numbers.size() != 6) {
            throw new IllegalArgumentException("번호는 정확히 6개여야 합니다");
        }

        if (hasDuplicates()) {
            throw new IllegalArgumentException("중복된 번호가 있습니다");
        }

        for (Integer num : numbers) {
            if (num == null || num < 1 || num > 45) {
                throw new IllegalArgumentException("번호는 1~45 사이여야 합니다: " + num);
            }
        }
    }
}