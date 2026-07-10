package com.nhnacademy.account.domain;

public enum AccountStatus {
    // 기본
    ACTIVE,
    // 로그인 차단
    LOCKED,
    // 장기 미사용 등의 이유로 비활성화
    INACTIVE,
    // 탈퇴 완료, 복구 불가
    WITHDRAWN
}
