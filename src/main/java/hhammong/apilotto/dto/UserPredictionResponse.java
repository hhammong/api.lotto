package hhammong.apilotto.dto;

import hhammong.apilotto.entity.UserPrediction;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPredictionResponse {

    private UUID predictionId;
    private UUID userId;

    // 개별 번호
    private Integer number1;
    private Integer number2;
    private Integer number3;
    private Integer number4;
    private Integer number5;
    private Integer number6;

    // 배열 형태
    private List<Integer> numbers;

    private String memo;
    private Integer targetDrawNo;
    private Integer startDrawId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity -> DTO 변환
    public static UserPredictionResponse from(UserPrediction entity) {
        return UserPredictionResponse.builder()
                .predictionId(entity.getPredictionId())
                .userId(entity.getUser().getUserId())
                .number1(entity.getPredictedNum1().intValue())
                .number2(entity.getPredictedNum2().intValue())
                .number3(entity.getPredictedNum3().intValue())
                .number4(entity.getPredictedNum4().intValue())
                .number5(entity.getPredictedNum5().intValue())
                .number6(entity.getPredictedNum6().intValue())
                .numbers(Arrays.asList(
                        entity.getPredictedNum1().intValue(),
                        entity.getPredictedNum2().intValue(),
                        entity.getPredictedNum3().intValue(),
                        entity.getPredictedNum4().intValue(),
                        entity.getPredictedNum5().intValue(),
                        entity.getPredictedNum6().intValue()
                ))
                .memo(entity.getMemo())
                .targetDrawNo(entity.getTargetDrawNo())
                .startDrawId(entity.getStartDrawId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}