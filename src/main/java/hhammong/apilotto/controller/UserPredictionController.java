package hhammong.apilotto.controller;

import hhammong.apilotto.dto.ApiResponse;
import hhammong.apilotto.dto.UserPredictionCreateRequest;
import hhammong.apilotto.dto.UserPredictionResponse;
import hhammong.apilotto.service.UserPredictionService;
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

@RestController
@RequestMapping("/api/users/{userId}/predictions")
@RequiredArgsConstructor
public class UserPredictionController {

    private final UserPredictionService predictionService;

    /**
     * 번호 등록
     * POST /api/users/{userId}/predictions
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserPredictionResponse>> createPrediction(
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
}