package hhammong.apilotto.controller;

import hhammong.apilotto.dto.*;
import hhammong.apilotto.service.LottoHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @PostMapping("/lotto-history")
    @Operation(summary = "최신 로또 당첨 번호 등록", description = "매주 업데이트 되는 로또 당첨 번호.")
    public LottoHistoryResponse createLottoHistory(
            @Valid @RequestBody LottoHistoryCreateRequest request
    ) {
        return lottoHistoryService.createLottoHistory(request);
    }
}