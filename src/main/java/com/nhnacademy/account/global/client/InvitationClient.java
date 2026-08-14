package com.nhnacademy.account.global.client;

import com.nhnacademy.account.dto.request.InvitationsSignupRequest;
import com.nhnacademy.account.dto.request.SignupCompensateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationClient {
    private static final String INVITATION_SERVICE = "/api/core/internal/invitations";
    private final InventoryClient inventoryClient;

    public void signup(InvitationsSignupRequest invitationsSignupRequest) {
        inventoryClient.post(INVITATION_SERVICE + "/use", invitationsSignupRequest);
    }

    public void compensate(SignupCompensateRequest request) {
        inventoryClient.post(INVITATION_SERVICE + "/compensate", request);
    }
}
