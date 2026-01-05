package hhammong.apilotto.repository;

import hhammong.apilotto.entity.UserPredictionHistoricalStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPredictionHistoricalStatsRepository extends JpaRepository<UserPredictionHistoricalStats, UUID> {


}
