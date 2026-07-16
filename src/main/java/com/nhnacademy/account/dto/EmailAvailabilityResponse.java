package com.nhnacademy.account.dto;

public record EmailAvailabilityResponse(
        boolean available
) {
    public static EmailAvailabilityResponse from(boolean available) {
        return new EmailAvailabilityResponse(available);
    }
}
