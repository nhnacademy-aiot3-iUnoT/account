package com.nhnacademy.account.dto;

import java.util.UUID;

public record UpdateAccountRequest(
        UUID uuid,
        String name,
        String email,
        String password
) {}
