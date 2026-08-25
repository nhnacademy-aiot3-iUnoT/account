package com.nhnacademy.account.global.client;

import com.nhnacademy.account.dto.request.InvitationsSignupRequest;
import com.nhnacademy.account.dto.request.SignupCompensateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationClient {
    private static final String INTERNAL_INVITATIONS_API = "/api/core/internal/invitations";

    private final InventoryApiClient inventoryApiClient;

    public void signup(InvitationsSignupRequest request) {
        inventoryApiClient.post(INTERNAL_INVITATIONS_API + "/use", request);
    }

    public void compensate(SignupCompensateRequest request) {
        inventoryApiClient.post(INTERNAL_INVITATIONS_API + "/compensate", request);
    }
}
