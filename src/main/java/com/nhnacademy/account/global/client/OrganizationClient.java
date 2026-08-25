package com.nhnacademy.account.global.client;

import com.nhnacademy.account.dto.request.LeaveOrgRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationClient {
    private static final String LEAVE_ORGANIZATION_API = "/api/core/internal/leave-org";

    private final InventoryApiClient inventoryApiClient;

    public void leaveOrganization(LeaveOrgRequest request) {
        inventoryApiClient.post(LEAVE_ORGANIZATION_API, request);
    }
}
