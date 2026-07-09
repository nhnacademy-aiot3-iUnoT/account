package com.nhnacademy.account.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class Account {

    // 계정 생성
    public Account(Long id, String username, String password, String email) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;

        accountStatus = AccountStatus.ACTIVE;
        createdAt = LocalDate.now();
        updatedAt = LocalDate.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String username;

    private String password;

    private String email;

    private AccountStatus accountStatus;

    private LocalDate createdAt;

    private LocalDate updatedAt;

    private LocalDate withdrawnAt;


    public void updateAccount(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.updatedAt = LocalDate.now();
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
        this.updatedAt = LocalDate.now();

        if (accountStatus == AccountStatus.WITHDRAWN) {
            this.username = null;
            this.password = null;
            this.email = null;
            this.withdrawnAt = LocalDate.now();
        }
    }

}
