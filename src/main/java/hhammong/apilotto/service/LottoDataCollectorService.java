package hhammong.apilotto.service;

import hhammong.apilotto.dto.LottoScrapingDTO;
import hhammong.apilotto.entity.LottoHistory;
import hhammong.apilotto.repository.LottoHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 로또 당첨 결과 수집 비즈니스 로직 서비스
 * - 스크래핑 → 변환 → 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LottoDataCollectorService {

    private final LottoHistoryRepository lottoHistoryRepository;
    private final LottoScrapingService scrapingService;

    /**
     * 최신 회차 3개 수집 (누락 방지)
     */
    @Transactional
    public void collectLatestResults() {
        try {
            Integer latestDrawNo = scrapingService.getLatestDrawNo();
            log.info("📊 현재 최신 회차: {}", latestDrawNo);

            // 최근 3회차 수집
            for (int i = 0; i < 3; i++) {
                Integer targetDrawNo = latestDrawNo - i;
                collectSingleResult(targetDrawNo);
            }

            log.info("✅ 최신 결과 수집 완료");

        } catch (Exception e) {
            log.error("❌ 최신 결과 수집 실패", e);
            throw new RuntimeException("최신 결과 수집 실패", e);
        }
    }

    /**
     * 특정 회차 수집
     */
    @Transactional
    public LottoHistory collectSingleResult(Integer drawNo) {
        try {
            // 이미 존재하는지 확인
            if (lottoHistoryRepository.existsByDrawNo(drawNo)) {
                log.info("ℹ️ {}회차는 이미 존재합니다. 스킵", drawNo);
                return lottoHistoryRepository.findByDrawNoAndDeleteYnAndUseYn(drawNo, "N", "Y")
                        .orElseThrow(() -> new RuntimeException("데이터 조회 실패"));
            }

            log.info("🔍 {}회차 수집 시작", drawNo);

            // 스크래핑
            LottoScrapingDTO dto = scrapingService.scrapDrawResult(drawNo);

            // 엔티티 변환 및 저장
            LottoHistory entity = convertToEntity(dto);
            LottoHistory saved = lottoHistoryRepository.save(entity);

            log.info("💾 {}회차 저장 완료 - ID: {}, 당첨번호: {},{},{},{},{},{} + {}",
                    drawNo,
                    saved.getHistoryId(),
                    saved.getNumber1(), saved.getNumber2(), saved.getNumber3(),
                    saved.getNumber4(), saved.getNumber5(), saved.getNumber6(),
                    saved.getBonusNumber());

            return saved;

        } catch (Exception e) {
            log.error("❌ {}회차 수집 실패", drawNo, e);
            throw new RuntimeException(drawNo + "회차 수집 실패", e);
        }
    }

    /**
     * 범위 수집 (과거 데이터 수집용)
     */
    @Transactional
    public void collectRangeResults(Integer startDrawNo, Integer endDrawNo) {
        log.info("📦 범위 수집 시작: {}회 ~ {}회", startDrawNo, endDrawNo);

        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (int drawNo = startDrawNo; drawNo <= endDrawNo; drawNo++) {
            try {
                if (lottoHistoryRepository.existsByDrawNo(drawNo)) {
                    log.info("⏭️ {}회차 스킵 (이미 존재)", drawNo);
                    skipCount++;
                } else {
                    collectSingleResult(drawNo);
                    successCount++;

                    // 서버 부하 방지 (1초 대기)
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                log.error("❌ {}회차 수집 중 오류 발생, 계속 진행", drawNo, e);
                failCount++;
            }
        }

        log.info("✅ 범위 수집 완료 - 성공: {}, 스킵: {}, 실패: {}",
                successCount, skipCount, failCount);
    }

    /**
     * 누락된 회차 찾기
     */
    public List<Integer> findMissingDraws() {
        Optional<LottoHistory> latestOpt = lottoHistoryRepository
                .findTopByDeleteYnAndUseYnOrderByDrawNoDesc("N", "Y");

        if (latestOpt.isEmpty()) {
            log.warn("⚠️ DB에 저장된 회차가 없습니다.");
            return new ArrayList<>();
        }

        Integer latestDrawNo = latestOpt.get().getDrawNo();
        List<Integer> missing = new ArrayList<>();

        for (int i = 1; i <= latestDrawNo; i++) {
            if (!lottoHistoryRepository.existsByDrawNo(i)) {
                missing.add(i);
            }
        }

        if (missing.isEmpty()) {
            log.info("✅ 누락된 회차 없음 (1 ~ {}회)", latestDrawNo);
        } else {
            log.warn("⚠️ 누락된 회차 발견: {}", missing);
        }

        return missing;
    }

    /**
     * 누락된 회차 자동 수집
     */
    @Transactional
    public void collectMissingDraws() {
        List<Integer> missing = findMissingDraws();

        if (missing.isEmpty()) {
            log.info("ℹ️ 수집할 누락 회차가 없습니다.");
            return;
        }

        log.info("🔄 누락된 {}개 회차 수집 시작", missing.size());

        for (Integer drawNo : missing) {
            try {
                collectSingleResult(drawNo);
                Thread.sleep(1000); // 서버 부하 방지
            } catch (Exception e) {
                log.error("❌ {}회차 재수집 실패", drawNo, e);
            }
        }

        log.info("✅ 누락 회차 수집 완료");
    }

    /**
     * DTO → Entity 변환
     */
    private LottoHistory convertToEntity(LottoScrapingDTO dto) {
        // numbers 필드: "1,2,3,4,5,6" 형식
        String numbers = Arrays.asList(
                        dto.getNumber1(),
                        dto.getNumber2(),
                        dto.getNumber3(),
                        dto.getNumber4(),
                        dto.getNumber5(),
                        dto.getNumber6()
                ).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return LottoHistory.builder()
                .drawNo(dto.getDrawNo())
                .drawDate(dto.getDrawDate())
                .number1(dto.getNumber1())
                .number2(dto.getNumber2())
                .number3(dto.getNumber3())
                .number4(dto.getNumber4())
                .number5(dto.getNumber5())
                .number6(dto.getNumber6())
                .bonusNumber(dto.getBonusNumber())
                .numbers(numbers)
                .prize1st(dto.getPrize1st())
                .prize2nd(dto.getPrize2nd())
                .prize3rd(dto.getPrize3rd())
                .prize4th(dto.getPrize4th())
                .prize5th(dto.getPrize5th())
                .deleteYn("N")
                .useYn("Y")
                .build();
    }
}