package com.nhnacademy.account.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "account")
public class Account {

    // 계정 생성
    public Account(String username, String hashedPassword, String email) {
        this.uuid =  UUID.randomUUID();
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.username = username;

        accountStatus = AccountStatus.ACTIVE;
        createdAt = LocalDate.now();
        updatedAt = LocalDate.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // DB 식별자
    private Long id;

    @Column(name = "uuid", unique = true, nullable = false)
    // jwt sub
    private UUID uuid;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password_hash")
    private String hashedPassword;

    @Column(name = "username")
    private String username;

    @Column(name = "account_status")
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @Column(name = "withdrawn_at")
    private LocalDate withdrawnAt;


    public void updateAccount(String email, String hashedPassword, String username) {
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.username = username;

        this.updatedAt = LocalDate.now();
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
        this.updatedAt = LocalDate.now();

        if (accountStatus == AccountStatus.WITHDRAWN) {
            this.username = null;
            this.hashedPassword = null;
            this.email = null;
            this.withdrawnAt = LocalDate.now();
        }
    }

}
