package hhammong.apilotto.controller;

import hhammong.apilotto.dto.*;
import hhammong.apilotto.service.UserPredictionCheckService;
import hhammong.apilotto.service.UserPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "사용자 번호 예측", description = "사용자가 등록한 예측 번호 관련 API")
@RestController
@RequestMapping("/api/users/{userId}/predictions")
@RequiredArgsConstructor
public class UserPredictionController {

    private final UserPredictionService predictionService;
    private final UserPredictionCheckService checkService;

    /**
     * 번호 등록
     * POST /api/users/{userId}/predictions
     */
    @PostMapping
    @Operation(summary = "예측 번호 등록", description = "사용자가 예측하는 번호 6개를 입력한다.")
    public ResponseEntity<ApiResponse<UserPredictionResponse>> createPrediction(
            @Parameter(description = "userId", required = true)
            @PathVariable UUID userId,

            @Valid @RequestBody UserPredictionCreateRequest request) {

        UserPredictionResponse response = predictionService.createPrediction(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "번호가 성공적으로 등록되었습니다"));
    }

    /**
     * 내 번호 목록 조회
     * GET /api/users/{userId}/predictions
     */
    @GetMapping
    @Operation(summary = "예측 번호 조회", description = "사용자가 등록한 번호 목록 조회.")
    public ResponseEntity<ApiResponse<List<UserPredictionResponse>>> getMyPredictions(
            @PathVariable UUID userId) {

        List<UserPredictionResponse> response = predictionService.getMyPredictions(userId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "번호 목록 조회 성공"));
    }

    /**
     * 내 번호 목록 조회 (페이징)
     * GET /api/users/{userId}/predictions/page?page=0&size=10
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<UserPredictionResponse>>> getMyPredictionsPage(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UserPredictionResponse> response = predictionService.getMyPredictions(userId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(response, "번호 목록 조회 성공"));
    }

    /**
     * 번호 상세 조회
     * GET /api/users/{userId}/predictions/{predictionId}
     */
    @GetMapping("/{predictionId}")
    public ResponseEntity<ApiResponse<UserPredictionResponse>> getPredictionDetail(
            @PathVariable UUID userId,
            @PathVariable UUID predictionId) {

        UserPredictionResponse response = predictionService.getPredictionDetail(userId, predictionId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "번호 상세 조회 성공"));
    }

    /**
     * 등록된 번호 개수
     * GET /api/users/{userId}/predictions/count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getMyPredictionCount(
            @PathVariable UUID userId) {

        long count = predictionService.getMyPredictionCount(userId);

        return ResponseEntity.ok(
                ApiResponse.success(count, "번호 개수 조회 성공"));
    }

    /**
     * 내 모든 번호 최신 회차 당첨 확인
     * GET /api/users/{userId}/predictions/check-latest
     */
    @GetMapping("/check-latest")
    @Operation(summary = "내 모든 번호 최신 회차 당첨 확인", description = "내 모든 번호 최신 회차 당첨 확인.")
    public ResponseEntity<ApiResponse<AllNumbersCheckResponse>> checkLatestDraw(
            @PathVariable UUID userId) {

        AllNumbersCheckResponse response = checkService.checkAllMyNumbers(userId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "최신 회차 당첨 확인 완료"));
    }

    /**
     * 특정 번호의 전체 당첨 이력 조회
     * GET /api/users/{userId}/predictions/{predictionId}/history
     */
    @GetMapping("/{predictionId}/history")
    public ResponseEntity<ApiResponse<PredictionHistoryResponse>> getPredictionHistory(
            @PathVariable UUID userId,
            @PathVariable UUID predictionId) {

        //PredictionHistoryResponse response = checkService.getPredictionHistory(userId, predictionId);
        PredictionHistoryResponse response = checkService.getPredictionHistory2(userId, predictionId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "번호 이력 조회 완료"));
    }

}