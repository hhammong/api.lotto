package hhammong.apilotto.repository;

import hhammong.apilotto.entity.LottoHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LottoHistoryRepository extends JpaRepository<LottoHistory, UUID> {

    // 최신 회차 조회 (drawNo 기준 내림차순 첫번째)
    Optional<LottoHistory> findTopByDeleteYnAndUseYnOrderByDrawNoDesc(String deleteYn, String useYn);

    // 회차 번호로 조회
    Optional<LottoHistory> findByDrawNoAndDeleteYnAndUseYn(Integer drawNo, String deleteYn, String useYn);

    // 전체 목록 조회 (페이징, 최신순)
    Page<LottoHistory> findByDeleteYnAndUseYnOrderByDrawNoDesc(String deleteYn, String useYn, Pageable pageable);

    // 회차 번호 존재 여부 확인
    boolean existsByDrawNo(Integer drawNo);

    Optional<LottoHistory> findByDrawNo(Integer drawNo);

    /**
     * 특정 회차 이상의 모든 당첨번호 조회 (오름차순)
     */
    List<LottoHistory> findByDrawNoGreaterThanEqualAndDeleteYnAndUseYnOrderByDrawNoAsc(
            Integer drawNo, String deleteYn, String useYn);

}