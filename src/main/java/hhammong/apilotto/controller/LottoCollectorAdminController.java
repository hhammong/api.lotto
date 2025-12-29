package hhammong.apilotto.controller;

import hhammong.apilotto.entity.LottoHistory;
import hhammong.apilotto.service.LottoDataCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 로또 데이터 수집 관리자 API
 * - 수동 수집 트리거
 * - 상태 확인
 * - 누락 회차 확인
 */
@RestController
@RequestMapping("/api/admin/lotto")
@RequiredArgsConstructor
@Slf4j
public class LottoCollectorAdminController {

    private final LottoDataCollectorService collectorService;

    /**
     * 최신 회차 수동 수집
     * GET /api/admin/lotto/collect/latest
     */
    @PostMapping("/collect/latest")
    public ResponseEntity<Map<String, Object>> collectLatest() {
        log.info("📡 [API] 최신 회차 수동 수집 요청");

        try {
            collectorService.collectLatestResults();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "최신 회차 수집 완료");

            log.info("✅ [API] 최신 회차 수집 성공");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [API] 최신 회차 수집 실패", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "수집 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 회차 수집
     * POST /api/admin/lotto/collect/1201
     */
    @PostMapping("/collect/{drawNo}")
    public ResponseEntity<Map<String, Object>> collectSingle(@PathVariable Integer drawNo) {
        log.info("📡 [API] {}회차 수동 수집 요청", drawNo);

        try {
            LottoHistory result = collectorService.collectSingleResult(drawNo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", drawNo + "회차 수집 완료");
            response.put("data", result);

            log.info("✅ [API] {}회차 수집 성공", drawNo);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [API] {}회차 수집 실패", drawNo, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "수집 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 범위 수집
     * POST /api/admin/lotto/collect/range?start=1&end=10
     */
    @PostMapping("/collect/range")
    public ResponseEntity<Map<String, Object>> collectRange(
            @RequestParam Integer start,
            @RequestParam Integer end) {

        log.info("📡 [API] 범위 수집 요청: {}회 ~ {}회", start, end);

        // 유효성 검사
        if (start > end) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "시작 회차가 종료 회차보다 큽니다.");
            return ResponseEntity.badRequest().body(response);
        }

        if (end - start > 100) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "한 번에 최대 100회차까지만 수집 가능합니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            collectorService.collectRangeResults(start, end);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("%d ~ %d회차 수집 완료", start, end));

            log.info("✅ [API] 범위 수집 성공: {}회 ~ {}회", start, end);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [API] 범위 수집 실패", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "수집 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 누락 회차 확인
     * GET /api/admin/lotto/missing
     */
    @GetMapping("/missing")
    public ResponseEntity<Map<String, Object>> checkMissing() {
        log.info("📡 [API] 누락 회차 확인 요청");

        try {
            List<Integer> missing = collectorService.findMissingDraws();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", missing.size());
            response.put("missing", missing);

            if (missing.isEmpty()) {
                response.put("message", "누락된 회차가 없습니다.");
            } else {
                response.put("message", missing.size() + "개의 누락 회차 발견");
            }

            log.info("✅ [API] 누락 회차 확인 완료: {}개", missing.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [API] 누락 회차 확인 실패", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "확인 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 누락 회차 자동 수집
     * POST /api/admin/lotto/collect/missing
     */
    @PostMapping("/collect/missing")
    public ResponseEntity<Map<String, Object>> collectMissing() {
        log.info("📡 [API] 누락 회차 수집 요청");

        try {
            List<Integer> missingBefore = collectorService.findMissingDraws();

            if (missingBefore.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "누락된 회차가 없습니다.");
                response.put("collected", 0);

                return ResponseEntity.ok(response);
            }

            collectorService.collectMissingDraws();

            List<Integer> missingAfter = collectorService.findMissingDraws();
            int collected = missingBefore.size() - missingAfter.size();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", collected + "개 회차 수집 완료");
            response.put("collected", collected);
            response.put("before", missingBefore);
            response.put("after", missingAfter);

            log.info("✅ [API] 누락 회차 수집 완료: {}개", collected);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [API] 누락 회차 수집 실패", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "수집 실패: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }
}