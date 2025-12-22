package hhammong.apilotto.controller;

import hhammong.apilotto.dto.LoginRequest;
import hhammong.apilotto.dto.SignupRequest;
import hhammong.apilotto.entity.User;
import hhammong.apilotto.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            User user = authService.signup(
                    request.getUserUid(),
                    request.getPassword(),
                    request.getName(),
                    request.getNickname()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            response.put("userId", user.getUserId());
            response.put("userUid", user.getUserUid());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Map<String, Object> loginResult = authService.login(request.getUserUid(), request.getPassword());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인 성공");
            response.put("token", loginResult.get("token"));
            response.put("user", loginResult.get("user"));  // 사용자 정보 포함!

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 로그아웃 (클라이언트에서 토큰 삭제)
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // JWT는 Stateless이므로 서버에서 할 일 없음
        // 클라이언트가 토큰을 삭제하면 됨
        return ResponseEntity.ok(
                Map.of("success", true, "message", "로그아웃 성공")
        );
    }

    // ID 중복 체크
    @GetMapping("/check-duplicate")
    public ResponseEntity<?> checkDuplicate(@RequestParam String userUid) {
        boolean isDuplicate = authService.checkUserUidDuplicate(userUid);
        return ResponseEntity.ok(Map.of("duplicate", isDuplicate));
    }

}
