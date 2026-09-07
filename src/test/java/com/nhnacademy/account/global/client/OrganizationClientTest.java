package com.nhnacademy.account.global.client;

import com.nhnacademy.account.dto.request.LeaveOrgRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OrganizationClientTest {

    @Mock
    private InventoryApiClient inventoryApiClient;

    @InjectMocks
    private OrganizationClient organizationClient;

    @Test
    void leaveOrganizationUsesInternalOrganizationEndpoint() {
        LeaveOrgRequest request = new LeaveOrgRequest(
                UUID.randomUUID(),
                "member@example.com"
        );

        organizationClient.leaveOrganization(request);

        then(inventoryApiClient).should()
                .post("/api/core/internal/leave-org", request);
    }
}
