package hhammong.apilotto.controller;

import hhammong.apilotto.dto.ApiResponse;
import hhammong.apilotto.dto.LottoHistoryResponse;
import hhammong.apilotto.service.LottoHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lotto")
@RequiredArgsConstructor
public class LottoHistoryController {

    private final LottoHistoryService lottoHistoryService;

    /**
     * 최신 회차 당첨번호 조회
     * GET /api/lotto/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<LottoHistoryResponse>> getLatestDraw() {
        LottoHistoryResponse response = lottoHistoryService.getLatestDraw();
        return ResponseEntity.ok(ApiResponse.success(response, "최신 회차 조회 성공"));
    }

    /**
     * 특정 회차 당첨번호 조회
     * GET /api/lotto/draws/{drawNo}
     */
    @GetMapping("/draws/{drawNo}")
    public ResponseEntity<ApiResponse<LottoHistoryResponse>> getDrawByNumber(
            @PathVariable Integer drawNo) {
        LottoHistoryResponse response = lottoHistoryService.getDrawByNumber(drawNo);
        return ResponseEntity.ok(ApiResponse.success(response, drawNo + "회차 조회 성공"));
    }

    /**
     * 전체 회차 목록 조회 (페이징)
     * GET /api/lotto/draws?page=0&size=20
     */
    @GetMapping("/draws")
    public ResponseEntity<ApiResponse<Page<LottoHistoryResponse>>> getAllDraws(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LottoHistoryResponse> response = lottoHistoryService.getAllDraws(pageable);

        return ResponseEntity.ok(ApiResponse.success(response, "회차 목록 조회 성공"));
    }
}