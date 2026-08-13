package com.nhnacademy.account.domain;

import com.nhnacademy.account.global.error.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    @DisplayName("일반 생성자는 UUID를 만들고 USER, ACTIVE를 기본 설정한다.")
    void testConstructor() {
        Account account = new Account("test", "test@test.com", "hashed");

        assertNotNull(account.getUuid());
        assertEquals("test", account.getName());
        assertEquals("test@test.com", account.getEmail());
        assertEquals("hashed", account.getHashedPassword());
        assertEquals(AccountRole.USER, account.getAccountRole());
        assertEquals(AccountStatus.ACTIVE, account.getAccountStatus());
    }

    @Test
    @DisplayName("관리자 생성자는 ADMIN, ACTIVE를 설정한다.")
    void testConstructor2() {
        Account account = new Account("test", "test@test.com", "hashed", AccountRole.ADMIN);

        assertNotNull(account.getUuid());
        assertEquals("test", account.getName());
        assertEquals("test@test.com", account.getEmail());
        assertEquals("hashed", account.getHashedPassword());
        assertEquals(AccountRole.ADMIN, account.getAccountRole());
        assertEquals(AccountStatus.ACTIVE, account.getAccountStatus());
    }

    @Test
    @DisplayName("이름의 null, 빈 문자열, 공백 문자열을 각각 거부한다.")
    void testNameValidation() {
        assertThrows(BadRequestException.class,
                () -> new Account(null, "test@test.com", "hashed"));

        assertThrows(BadRequestException.class,
                () -> new Account("", "test@test.com", "hashed"));

        assertThrows(BadRequestException.class,
                () -> new Account(" ", "test@test.com", "hashed"));
    }

    @Test
    @DisplayName("이메일과 비밀번호 해시는 계정 생성 시 null을 거부하고 탈퇴 시 null로 변경된다.")
    void credentialsCanBeNullOnlyAfterWithdrawal() {
        assertThrows(BadRequestException.class,
                () -> new Account("test", null, "hashed"));
        assertThrows(BadRequestException.class,
                () -> new Account("test", "test@test.com", null));

        Account account = new Account("test", "test@test.com", "hashed");

        account.withdraw();

        assertNull(account.getEmail());
        assertNull(account.getHashedPassword());
        assertEquals(AccountStatus.WITHDRAWN, account.getAccountStatus());
    }

    // 이메일의 빈 문자열, 공백 문자열을 각각 거부한다.
    // 비밀번호 해시의 null, 빈 문자열, 공백 문자열을 각각 거부한다
    //  role이 null인 경우를 명시적으로 거부한다.
    //  예외 타입, ErrorCode, 사용자 메시지를 함께 검증한다.
    // DTO 검증을 우회해도 도메인 불변식이 유지된다.

}
