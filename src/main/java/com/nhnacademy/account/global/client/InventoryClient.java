package com.nhnacademy.account.global.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InventoryClient {
    private final RestClient restClient;

    public InventoryClient(
            @Qualifier("inventoryRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }
}
