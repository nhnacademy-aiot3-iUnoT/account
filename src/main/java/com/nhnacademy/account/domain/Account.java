package com.nhnacademy.account.domain;

import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BadRequestException;
import com.nhnacademy.account.global.error.exception.ConflictException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    /**
     * 외부 공개 식별자
     * JWT subject(sub)로 사용
     */
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(
            name = "uuid",
            nullable = false,
            unique = true,
            updatable = false,
            columnDefinition = "BINARY(16)"
    )
    private UUID uuid;

    @Column(
            name = "email",
            unique = true,
            length = 254
    )
    private String email;

    @Column(
            name = "password_hash",
            length = 256
    )
    private String hashedPassword;

    @Column(
            name = "name",
            length = 100
    )
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "account_status",
            nullable = false,
            length = 32
    )
    private AccountStatus accountStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "account_role",
            nullable = false,
            length = 32
    )
    private AccountRole accountRole;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;


    /**
     * 계정 생성
     */
    public Account(
            String name,
            String email,
            String hashedPassword,
            AccountRole role
    ) {
        this.uuid = UUID.randomUUID();
        this.name = requireText(name, "이름");
        this.hashedPassword = requireText(hashedPassword, "비밀번호 해시");
        this.email = requireText(email, "이메일");
        this.accountStatus = AccountStatus.ACTIVE;
        this.accountRole = role;
    }

    public Account(
            String name,
            String email,
            String hashedPassword
    ) {
        this(name, email, hashedPassword, AccountRole.USER);
    }


    /**
     * 이름 변경
     */
    public void changeName(String name) {
        validateActive();

        this.name = requireText(name, "이름");
    }

    /**
     * 비밀번호 변경
     *
     * 반드시 이미 해싱된 비밀번호를 전달해야 한다.
     */
    public void changeHashedPassword(String hashedPassword) {
        validateActive();

        this.hashedPassword =
                requireText(hashedPassword, "비밀번호 해시");
    }


    /**
     * 계정 잠금
     */
    public void lock() {
        if (this.accountStatus != AccountStatus.ACTIVE) {
            throw new ConflictException(
                    ErrorCode.INVALID_ACCOUNT_STATE
            );
        }

        this.accountStatus = AccountStatus.LOCKED;
    }


    /**
     * 계정 잠금 해제
     */
    public void unlock() {
        if (this.accountStatus != AccountStatus.LOCKED) {
            throw new ConflictException(
                    ErrorCode.INVALID_ACCOUNT_STATE
            );
        }

        this.accountStatus = AccountStatus.ACTIVE;
    }


    /**
     * 계정 비활성화
     */
    public void deactivate() {
        if (this.accountStatus != AccountStatus.ACTIVE) {
            throw new ConflictException(
                    ErrorCode.INVALID_ACCOUNT_STATE
            );
        }

        this.accountStatus = AccountStatus.INACTIVE;
    }


    /**
     * 비활성 계정 재활성화
     */
    public void activate() {
        if (this.accountStatus != AccountStatus.INACTIVE) {
            throw new ConflictException(
                    ErrorCode.INVALID_ACCOUNT_STATE
            );
        }

        this.accountStatus = AccountStatus.ACTIVE;
    }


    /**
     * 관리자 계정 상태 변경
     */
    public void changeStatus(AccountStatusAction action) {
        if (action == null) {
            throw new BadRequestException(
                    ErrorCode.INVALID_INPUT,
                    "상태 변경 작업은 null일 수 없습니다."
            );
        }

        switch (action) {
            case LOCK -> lock();
            case UNLOCK -> unlock();
            case DEACTIVATE -> deactivate();
            case REACTIVATE -> activate();
        }
    }


    /**
     * 계정 탈퇴
     *
     * WITHDRAWN은 최종 상태로 취급한다.
     */
    public void withdraw() {
        if (this.accountStatus == AccountStatus.WITHDRAWN) {
            throw new ConflictException(
                    ErrorCode.INVALID_ACCOUNT_STATE
            );
        }

        LocalDateTime now = LocalDateTime.now();

        this.accountStatus = AccountStatus.WITHDRAWN;
        this.withdrawnAt = now;

        // 개인정보 제거
        this.email = null;
        this.hashedPassword = null;
        this.name = null;
    }


    /**
     * 로그인 가능한 정상 계정인지 확인
     */
    public boolean isActive() {
        return this.accountStatus == AccountStatus.ACTIVE;
    }


    /**
     * 탈퇴 계정인지 확인
     */
    public boolean isWithdrawn() {
        return this.accountStatus == AccountStatus.WITHDRAWN;
    }

    public boolean isLocked() {
        return this.accountStatus == AccountStatus.LOCKED;
    }

    public boolean isAdmin() {
        return this.accountRole == AccountRole.ADMIN;
    }

    private void validateActive() {
        if (this.accountStatus != AccountStatus.ACTIVE) {
            throw new ConflictException(
                    ErrorCode.INVALID_ACCOUNT_STATE
            );
        }
    }


    private static String requireText(
            String value,
            String fieldName
    ) {

        if (value == null) {
            throw new BadRequestException(
                    ErrorCode.INVALID_INPUT,
                    fieldName + "은 null일 수 없습니다."
            );
        }

        if (value.isBlank()) {
            throw new BadRequestException(
                    ErrorCode.INVALID_INPUT,
                    fieldName + "은 비어 있을 수 없습니다."
            );
        }

        return value;
    }


    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
