package com.nhnacademy.account.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("loadBalancedRestClientBuilder")
    @Profile("prod")
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean("inventoryRestClient")
    @Profile("prod")
    public RestClient prodInventoryRestClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
            @Value("${clients.inventory.base-url}") String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }

    @Bean("inventoryRestClient")
    @Profile("!prod")
    public RestClient devInventoryRestClient(
            @Value("${clients.inventory.base-url}") String baseUrl
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
