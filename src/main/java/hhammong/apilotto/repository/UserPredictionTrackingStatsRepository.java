package hhammong.apilotto.repository;

import hhammong.apilotto.entity.UserPredictionHistoricalStats;
import hhammong.apilotto.entity.UserPredictionTrackingStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPredictionTrackingStatsRepository extends JpaRepository<UserPredictionTrackingStats, UUID> {

    Optional<UserPredictionTrackingStats> findByPredictionId(UUID predictionId);

}
