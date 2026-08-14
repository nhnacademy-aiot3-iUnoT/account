package com.nhnacademy.account.global.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.ErrorDetail;
import com.nhnacademy.account.global.error.exception.UpstreamServiceException;
import com.nhnacademy.account.global.util.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

@Slf4j
@Component
public class InventoryClient {
    private final String baseUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public InventoryClient(
            @Value("${clients.inventory.base-url}") String baseUrl,
            @Qualifier("inventoryRestClient") RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.baseUrl = baseUrl;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public <T> T post(String path, Object body, Class<T> dataType) {
        return execute(() ->
                restClient.post()
                        .uri(baseUrl + path)
                        .body(body)
                        .retrieve()
                        .body(responseTypeOf(dataType)));
    }

    public void post(String path, Object body) {
        executeVoid(() ->
                restClient.post()
                        .uri(baseUrl + path)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity());
    }

    private <T> T execute(Supplier<ApiResponse<T>> supplier) {
        try {
            ApiResponse<T> response = supplier.get();

            if (response == null) {
                throw new UpstreamServiceException(
                        "Inventory 서비스 응답 본문이 없습니다."
                );
            }

            if (!response.success()) {
                throw convertApiResponseError(response.error());
            }

            return response.data();

        } catch (HttpStatusCodeException e) {
            throw convertApiException(e);
        } catch (ResourceAccessException e) {
            throw new UpstreamServiceException(
                    "Inventory 서비스에 연결할 수 없습니다.",
                    e
            );
        } catch (RestClientException e) {
            throw new UpstreamServiceException(
                    ErrorCode.UPSTREAM_SERVICE_ERROR.getMessage(),
                    e
            );
        }
    }

    private void executeVoid(Runnable request) {
        try {
            request.run();
        } catch (HttpStatusCodeException e) {
            throw convertApiException(e);
        } catch (ResourceAccessException e) {
            throw new UpstreamServiceException(
                    "Inventory 서비스에 연결할 수 없습니다.",
                    e
            );
        } catch (RestClientException e) {
            throw new UpstreamServiceException(
                    ErrorCode.UPSTREAM_SERVICE_ERROR.getMessage(),
                    e
            );
        }
    }

    private UpstreamServiceException convertApiResponseError(ErrorDetail error) {
        if (error == null || error.message() == null || error.message().isBlank()) {
            return new UpstreamServiceException(
                    ErrorCode.UPSTREAM_SERVICE_ERROR.getMessage()
            );
        }

        return new UpstreamServiceException(error.message());
    }

    private UpstreamServiceException convertApiException(HttpStatusCodeException exception) {
        log.warn(
                "Inventory request failed. status={}",
                exception.getStatusCode()
        );

        try {
            ApiResponse<Void> response = objectMapper.readValue(
                    exception.getResponseBodyAsString(),
                    new TypeReference<>() {
                    }
            );

            if (response != null && response.error() != null) {
                String message = response.error().message();
                if (message != null && !message.isBlank()) {
                    return new UpstreamServiceException(message, exception);
                }
            }
        } catch (Exception parseException) {
            exception.addSuppressed(parseException);
        }

        return new UpstreamServiceException(
                ErrorCode.UPSTREAM_SERVICE_ERROR.getMessage(),
                exception
        );
    }

    private <T> ParameterizedTypeReference<ApiResponse<T>> responseTypeOf(Class<T> dataType) {
        ResolvableType type = ResolvableType.forClassWithGenerics(ApiResponse.class, dataType);
        return ParameterizedTypeReference.forType(type.getType());
    }
}
