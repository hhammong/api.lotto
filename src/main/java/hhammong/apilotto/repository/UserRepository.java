package hhammong.apilotto.repository;

import hhammong.apilotto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // 로그인 ID로 찾기
    Optional<User> findByUserUid(String userUid);

    // 로그인 ID 중복 체크
    boolean existsByUserUid(String userUid);

    // 닉네임 중복 체크 (필요시)
    boolean existsByNickname(String nickname);

}
