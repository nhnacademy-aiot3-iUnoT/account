package com.nhnacademy.account.global.client;

import com.nhnacademy.account.dto.request.InvitationsSignupRequest;
import com.nhnacademy.account.dto.request.SignupCompensateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class InvitationClientTest {

    @Mock
    private InventoryApiClient inventoryApiClient;

    @InjectMocks
    private InvitationClient invitationClient;

    @Test
    void signupUsesInternalInvitationEndpoint() {
        InvitationsSignupRequest request = new InvitationsSignupRequest(
                UUID.randomUUID(),
                "member@example.com",
                UUID.randomUUID()
        );

        invitationClient.signup(request);

        then(inventoryApiClient).should()
                .post("/api/core/internal/invitations/use", request);
    }

    @Test
    void compensateUsesInternalInvitationEndpoint() {
        SignupCompensateRequest request = new SignupCompensateRequest(
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        invitationClient.compensate(request);

        then(inventoryApiClient).should()
                .post("/api/core/internal/invitations/compensate", request);
    }
}
