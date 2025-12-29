package hhammong.apilotto.controller;

import hhammong.apilotto.dto.ApiResponse;
import hhammong.apilotto.dto.CheckWinningRequest;
import hhammong.apilotto.dto.CheckWinningResponse;
import hhammong.apilotto.service.LottoCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lotto/check")
@RequiredArgsConstructor
public class LottoCheckController {

    private final LottoCheckService lottoCheckService;

    /**
     * 당첨 확인 (특정 회차 또는 최신 회차)
     * POST /api/lotto/check
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CheckWinningResponse>> checkWinning(
            @RequestBody CheckWinningRequest request) {

        CheckWinningResponse response = lottoCheckService.checkWinning(request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "당첨 확인 완료"));
    }
}