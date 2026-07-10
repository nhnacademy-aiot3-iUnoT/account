package com.nhnacademy.account.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Table(name = "account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 외부 공개 식별자
     * JWT subject(sub)로 사용
     */
    @Column(
            name = "uuid",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID uuid;

    /**
     * 탈퇴 시 개인정보 제거를 위해 nullable 허용
     */
    @Column(
            name = "email",
            unique = true,
            length = 255
    )
    private String email;

    /**
     * 탈퇴 시 제거
     */
    @Column(
            name = "password_hash",
            length = 255
    )
    private String hashedPassword;

    /**
     * 탈퇴 시 제거
     */
    @Column(
            name = "name",
            length = 100
    )
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "account_status",
            nullable = false,
            length = 20
    )
    private AccountStatus accountStatus;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;


    /**
     * 계정 생성
     */
    public Account(
            String name,
            String hashedPassword,
            String email
    ) {
        this.uuid = UUID.randomUUID();
        this.name = requireText(name, "이름");
        this.hashedPassword = requireText(hashedPassword, "비밀번호 해시");
        this.email = requireText(email, "이메일");
        this.accountStatus = AccountStatus.ACTIVE;
    }


    /**
     * 이름 변경
     */
    public void changeName(String name) {
        validateActive();

        this.name = requireText(name, "이름");
    }


    /**
     * 이메일 변경
     */
    public void changeEmail(String email) {
        validateActive();

        this.email = requireText(email, "이메일");
    }


    /**
     * 비밀번호 변경
     *
     * 반드시 이미 해싱된 비밀번호를 전달해야 한다.
     */
    public void changePassword(String hashedPassword) {
        validateActive();

        this.hashedPassword =
                requireText(hashedPassword, "비밀번호 해시");
    }


    /**
     * 계정 잠금
     */
    public void lock() {
        if (this.accountStatus != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "활성 상태의 계정만 잠글 수 있습니다."
            );
        }

        this.accountStatus = AccountStatus.LOCKED;
    }


    /**
     * 계정 잠금 해제
     */
    public void unlock() {
        if (this.accountStatus != AccountStatus.LOCKED) {
            throw new IllegalStateException(
                    "잠긴 계정만 잠금 해제할 수 있습니다."
            );
        }

        this.accountStatus = AccountStatus.ACTIVE;
    }


    /**
     * 계정 비활성화
     */
    public void deactivate() {
        if (this.accountStatus != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "활성 상태의 계정만 비활성화할 수 있습니다."
            );
        }

        this.accountStatus = AccountStatus.INACTIVE;
    }


    /**
     * 비활성 계정 재활성화
     */
    public void activate() {
        if (this.accountStatus != AccountStatus.INACTIVE) {
            throw new IllegalStateException(
                    "비활성 상태의 계정만 활성화할 수 있습니다."
            );
        }

        this.accountStatus = AccountStatus.ACTIVE;
    }


    /**
     * 계정 탈퇴
     *
     * WITHDRAWN은 최종 상태로 취급한다.
     */
    public void withdraw() {
        if (this.accountStatus == AccountStatus.WITHDRAWN) {
            throw new IllegalStateException(
                    "이미 탈퇴한 계정입니다."
            );
        }

        Instant now = Instant.now();

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


    private void validateActive() {
        if (this.accountStatus != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "활성 상태의 계정만 수정할 수 있습니다."
            );
        }
    }


    private static String requireText(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + "은 null일 수 없습니다."
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + "은 비어 있을 수 없습니다."
            );
        }

        return value;
    }


    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();

        this.createdAt = now;
        this.updatedAt = now;
    }


    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }
}