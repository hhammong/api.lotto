package hhammong.apilotto.repository;

import hhammong.apilotto.entity.PredictionsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PredictionsHistoryRepository extends JpaRepository<PredictionsHistory, UUID> {

    // 특정 예측 번호의 전체 이력 조회
    List<PredictionsHistory> findByPredictionIdOrderByDrawNoAsc(UUID predictionId);

    // 특정 사용자의 특정 예측 이력 조회
    List<PredictionsHistory> findByUserIdAndPredictionId(UUID userId, UUID predictionId);
}