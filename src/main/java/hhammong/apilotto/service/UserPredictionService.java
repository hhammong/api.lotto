package hhammong.apilotto.service;

import hhammong.apilotto.dto.UserPredictionCreateRequest;
import hhammong.apilotto.dto.UserPredictionResponse;
import hhammong.apilotto.entity.User;
import hhammong.apilotto.entity.UserPrediction;
import hhammong.apilotto.exception.DuplicateNumberException;
import hhammong.apilotto.exception.ResourceNotFoundException;
import hhammong.apilotto.repository.UserPredictionRepository;
import hhammong.apilotto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPredictionService {

    private final UserPredictionRepository predictionRepository;
    private final UserRepository userRepository;

    /**
     * 번호 등록
     */
    @Transactional
    public UserPredictionResponse createPrediction(UUID userId, UserPredictionCreateRequest request) {
        // 1. 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 2. 중복 번호 검증 (요청 내부)
        if (request.hasDuplicates()) {
            throw new IllegalArgumentException("중복된 번호가 있습니다");
        }

        // 3. 번호 정렬
        List<Integer> sortedNumbers = request.getSortedNumbers();

        // 4. 동일 번호 조합 이미 등록되었는지 확인
        boolean exists = predictionRepository.existsDuplicateNumbers(
                userId,
                sortedNumbers.get(0).shortValue(),
                sortedNumbers.get(1).shortValue(),
                sortedNumbers.get(2).shortValue(),
                sortedNumbers.get(3).shortValue(),
                sortedNumbers.get(4).shortValue(),
                sortedNumbers.get(5).shortValue()
        );

        if (exists) {
            throw new DuplicateNumberException("이미 등록된 번호 조합입니다");
        }

        // 5. Entity 생성
        UserPrediction prediction = UserPrediction.builder()
                .user(user)
                .predictedNum1(sortedNumbers.get(0).shortValue())
                .predictedNum2(sortedNumbers.get(1).shortValue())
                .predictedNum3(sortedNumbers.get(2).shortValue())
                .predictedNum4(sortedNumbers.get(3).shortValue())
                .predictedNum5(sortedNumbers.get(4).shortValue())
                .predictedNum6(sortedNumbers.get(5).shortValue())
                .memo(request.getMemo())
                .targetDrawNo(request.getTargetDrawNo())
                // startDrawId는 나중에 로직 추가 (현재 최신 회차 + 1)
                .build();

        // 6. 저장 (Entity의 @PrePersist에서 predictedNumbers 자동 생성)
        UserPrediction saved = predictionRepository.save(prediction);

        // 7. DTO 변환 후 반환
        return UserPredictionResponse.from(saved);
    }

    /**
     * 내 번호 목록 조회
     */
    public List<UserPredictionResponse> getMyPredictions(UUID userId) {
        List<UserPrediction> predictions = predictionRepository
                .findByUser_UserIdAndDeleteYnOrderByCreatedAtDesc(userId, "N");
        return predictions.stream()
                .map(UserPredictionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 내 번호 목록 조회 (페이징)
     */
    public Page<UserPredictionResponse> getMyPredictions(UUID userId, Pageable pageable) {
        Page<UserPrediction> predictions = predictionRepository
                .findByUser_UserIdAndDeleteYnOrderByCreatedAtDesc(userId, "N", pageable);

        return predictions.map(UserPredictionResponse::from);
    }

    /**
     * 번호 상세 조회
     */
    public UserPredictionResponse getPredictionDetail(UUID userId, UUID predictionId) {
        UserPrediction prediction = predictionRepository
                .findByPredictionIdAndUser_UserIdAndDeleteYn(predictionId, userId, "N")
                .orElseThrow(() -> new ResourceNotFoundException("해당 번호를 찾을 수 없습니다"));

        return UserPredictionResponse.from(prediction);
    }

    /**
     * 등록된 번호 개수 조회
     */
    public long getMyPredictionCount(UUID userId) {
        return predictionRepository.countByUser_UserIdAndDeleteYn(userId, "N");
    }
}