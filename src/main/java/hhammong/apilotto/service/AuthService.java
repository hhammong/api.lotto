package hhammong.apilotto.service;

import hhammong.apilotto.entity.User;
import hhammong.apilotto.repository.UserRepository;
import hhammong.apilotto.util.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입
    public User signup(String userUid, String password, String name, String nickname) {
        // ID 중복 체크
        if (checkUserUidDuplicate(userUid)) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // User 생성
        User user = new User();
        user.setUserUid(userUid);
        //user.setPassword(passwordEncoder.encode(password));  // 비밀번호 암호화
        user.setPassword(password);  // 임시
        user.setName(name);
        user.setNickname(nickname);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleteYn("N");
        user.setUseYn("Y");
        user.setNotificationEnabled(true);

        return userRepository.save(user);
    }

    // 로그인
    public Map<String, Object> login(String userUid, String password) {
        User user = userRepository.findByUserUid(userUid)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!password.equals(user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(user.getUserId().toString(), user.getUserUid());

        // 토큰 + 사용자 정보 함께 반환
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", Map.of(
                "userId", user.getUserId().toString(),
                "userUid", user.getUserUid(),
                "name", user.getName(),
                "nickname", user.getNickname()
        ));

        return result;
    }

    // ID 중복 체크
    public boolean checkUserUidDuplicate(String userUid) {
        return userRepository.existsByUserUid(userUid);
    }

}
