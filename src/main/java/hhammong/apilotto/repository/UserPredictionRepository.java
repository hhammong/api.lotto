package hhammong.apilotto.repository;

import hhammong.apilotto.entity.UserPrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPredictionRepository extends JpaRepository<UserPrediction, UUID> {

    // 특정 사용자의 번호 목록 조회 (삭제되지 않은 것만)
    List<UserPrediction> findByUser_UserIdAndDeleteYnOrderByCreatedAtDesc(
            UUID userId, String deleteYn);

    // 페이징 처리된 목록 조회
    Page<UserPrediction> findByUser_UserIdAndDeleteYnOrderByCreatedAtDesc(
            UUID userId, String deleteYn, Pageable pageable);

    // 특정 번호 조회
    Optional<UserPrediction> findByPredictionIdAndUser_UserIdAndDeleteYn(
            UUID predictionId, UUID userId, String deleteYn);

    // 사용자의 번호 개수 조회
    long countByUser_UserIdAndDeleteYn(UUID userId, String deleteYn);

    // 중복 번호 확인 (같은 번호 조합이 이미 있는지)
    @Query("SELECT COUNT(p) > 0 FROM UserPrediction p WHERE " +
            "p.user.userId = :userId AND " +
            "p.deleteYn = 'N' AND " +
            "p.predictedNum1 = :num1 AND " +
            "p.predictedNum2 = :num2 AND " +
            "p.predictedNum3 = :num3 AND " +
            "p.predictedNum4 = :num4 AND " +
            "p.predictedNum5 = :num5 AND " +
            "p.predictedNum6 = :num6")
    boolean existsDuplicateNumbers(@Param("userId") UUID userId,
                                   @Param("num1") Short num1,
                                   @Param("num2") Short num2,
                                   @Param("num3") Short num3,
                                   @Param("num4") Short num4,
                                   @Param("num5") Short num5,
                                   @Param("num6") Short num6);
}