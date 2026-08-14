package com.nhnacademy.account.global.client;

import com.nhnacademy.account.dto.request.InvitationsSignupRequest;
import com.nhnacademy.account.dto.request.SignupCompensateRequest;
import com.nhnacademy.account.dto.response.InvitationsSignupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationClient {
    private static final String INVITATION_SERVICE = "/api/core/internal/invitations";
    private final InventoryClient inventoryClient;

    public InvitationsSignupResponse signup(InvitationsSignupRequest invitationsSignupRequest) {
        return inventoryClient.post(INVITATION_SERVICE + "/use", invitationsSignupRequest, InvitationsSignupResponse.class);
    }

    public Void compensate(SignupCompensateRequest request) {
        return inventoryClient.post(INVITATION_SERVICE + "/compensate", request, Void.class);
    }
}
