package hhammong.apilotto.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long validityInMilliseconds = 3600000; // 1시간

    // 토큰 생성
    public String createToken(String userId, String userUid) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(userId)              // ← 이렇게!
                .claim("userUid", userUid)
                .issuedAt(now)
                .expiration(validity)         // ← 이렇게!
                .signWith(secretKey)
                .compact();
    }

    // 토큰에서 사용자 ID 추출
    public String getUserId(String token) {
        return Jwts.parser()                  // ← parserBuilder 아님!
                .verifyWith(secretKey)        // ← setSigningKey 아님!
                .build()
                .parseSignedClaims(token)     // ← parseClaimsJws 아님!
                .getPayload()                 // ← getBody 아님!
                .getSubject();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}