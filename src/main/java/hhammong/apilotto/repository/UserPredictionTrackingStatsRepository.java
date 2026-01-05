package hhammong.apilotto.repository;

import hhammong.apilotto.entity.UserPredictionTrackingStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserPredictionTrackingStatsRepository extends JpaRepository<UserPredictionTrackingStats, UUID> {

}
