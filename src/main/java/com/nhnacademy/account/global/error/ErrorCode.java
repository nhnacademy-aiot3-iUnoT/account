package com.nhnacademy.account.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Account
    ACCOUNT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "A001",
            "존재하지 않는 회원입니다."
    ),

    EMAIL_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "A002",
            "이미 사용 중인 이메일입니다."
    ),

    ACCOUNT_INACTIVE(
            HttpStatus.FORBIDDEN,
            "A003",
            "비활성화된 회원입니다."
    ),

    ACCOUNT_LOCKED(
            HttpStatus.FORBIDDEN,
            "A004",
            "잠긴 회원입니다."
    ),

    ACCOUNT_WITHDRAWN(
            HttpStatus.FORBIDDEN,
            "A005",
            "탈퇴한 회원입니다."
    ),

    // Authentication
    INVALID_CREDENTIALS(
            HttpStatus.UNAUTHORIZED,
            "AU001",
            "이메일 또는 비밀번호가 올바르지 않습니다."
    ),

    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "AU002",
            "인증이 필요합니다."
    ),

    INVALID_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AU003",
            "유효하지 않은 토큰입니다."
    ),

    EXPIRED_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AU004",
            "만료된 토큰입니다."
    ),

    UNSUPPORTED_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "AU005",
            "지원하지 않는 토큰입니다."
    ),

    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "AU006",
            "접근 권한이 없습니다."
    ),

    // Validation
    INVALID_INPUT(
            HttpStatus.BAD_REQUEST,
            "V001",
            "입력값이 올바르지 않습니다."
    ),

    // Common
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "C001",
            "서버 내부 오류가 발생했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}

