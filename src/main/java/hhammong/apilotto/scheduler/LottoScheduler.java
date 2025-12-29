package hhammong.apilotto.scheduler;

import hhammong.apilotto.service.LottoDataCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 로또 당첨 결과 자동 수집 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LottoScheduler {

    private final LottoDataCollectorService collectorService;

    /**
     * 매주 토요일 밤 9시 30분 실행
     * - 로또 추첨: 매주 토요일 오후 8시 35분경
     * - 여유있게 9시 30분에 자동 수집
     */
    @Scheduled(cron = "0 30 21 * * SAT")
    public void collectWeeklyResults() {
        log.info("========================================");
        log.info("🎰 로또 당첨 결과 자동 수집 시작");
        log.info("========================================");

        try {
            collectorService.collectLatestResults();

            log.info("========================================");
            log.info("✅ 로또 당첨 결과 자동 수집 완료");
            log.info("========================================");

        } catch (Exception e) {
            log.error("========================================");
            log.error("❌ 로또 당첨 결과 자동 수집 실패");
            log.error("========================================", e);

            // TODO: 관리자에게 알림 전송 (이메일, 슬랙 등)
            sendAlertToAdmin("로또 자동 수집 실패", e.getMessage());
        }
    }

    /**
     * 매일 새벽 2시 - 누락 회차 체크 및 재수집
     * - 서버 다운 등으로 누락된 회차 자동 복구
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void checkAndCollectMissing() {
        log.info("========================================");
        log.info("🔍 누락 회차 체크 시작");
        log.info("========================================");

        try {
            List<Integer> missing = collectorService.findMissingDraws();

            if (!missing.isEmpty()) {
                log.warn("⚠️ 누락된 회차 발견: {}", missing);
                log.info("🔄 누락 회차 재수집 시작");

                collectorService.collectMissingDraws();

                log.info("✅ 누락 회차 재수집 완료");
            } else {
                log.info("✅ 누락된 회차 없음");
            }

            log.info("========================================");
            log.info("✅ 누락 회차 체크 완료");
            log.info("========================================");

        } catch (Exception e) {
            log.error("========================================");
            log.error("❌ 누락 회차 체크 실패");
            log.error("========================================", e);
        }
    }

    /**
     * 관리자 알림 전송 (추후 구현)
     */
    private void sendAlertToAdmin(String title, String message) {
        // TODO: 이메일, 슬랙, 디스코드 등 알림 구현
        log.error("🚨 [알림] {}: {}", title, message);
    }
}