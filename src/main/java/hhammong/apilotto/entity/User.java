package hhammong.apilotto.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "USERS")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "USER_ID")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(name = "USER_UID", nullable = false, length = 50, unique = true)
    private String userUid;  // 로그인 ID

    @Column(name = "NAME", nullable = false, length = 50)
    private String name;

    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    @Column(name = "NICKNAME", length = 50)
    private String nickname;

    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;

    @Column(name = "NOTIFICATION_ENABLED")
    private Boolean notificationEnabled = true;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "DELETE_YN", length = 1)
    @ColumnDefault("N")
    @Builder.Default
    private String deleteYn = "N";

    @Column(name = "USE_YN", length = 1)
    @ColumnDefault("Y")
    @Builder.Default
    private String useYn = "Y";

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (this.deleteYn == null) this.deleteYn = "N";
        if (this.useYn == null) this.useYn = "Y";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
