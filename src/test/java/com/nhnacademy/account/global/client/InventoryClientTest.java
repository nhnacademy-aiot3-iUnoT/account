package com.nhnacademy.account.global.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.UpstreamServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InventoryClientTest {

    private static final String BASE_URL = "http://inventory";
    private static final String PATH = "/api/test";

    private MockRestServiceServer server;
    private InventoryClient inventoryClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        inventoryClient = new InventoryClient(
                BASE_URL,
                builder.build(),
                new ObjectMapper()
        );
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void postReturnsDataFromSuccessfulResponse() {
        server.expect(requestTo(BASE_URL + PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                                {
                                  "success": true,
                                  "data": {"value": "ok"},
                                  "error": null
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        TestPayload result = inventoryClient.post(
                PATH,
                Map.of("request", "value"),
                TestPayload.class
        );

        assertEquals("ok", result.value());
    }

    @Test
    void postRejectsEmptyResponseBody() {
        server.expect(requestTo(BASE_URL + PATH))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> inventoryClient.post(PATH, Map.of(), TestPayload.class)
        );

        assertEquals(ErrorCode.UPSTREAM_SERVICE_ERROR, exception.getErrorCode());
        assertEquals("Inventory 서비스 응답 본문이 없습니다.", exception.getMessage());
    }

    @Test
    void postRejectsFailedApiResponse() {
        server.expect(requestTo(BASE_URL + PATH))
                .andRespond(withSuccess(
                        """
                                {
                                  "success": false,
                                  "data": null,
                                  "error": {
                                    "code": "INV001",
                                    "message": "초대가 유효하지 않습니다.",
                                    "fieldErrors": []
                                  }
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> inventoryClient.post(PATH, Map.of(), TestPayload.class)
        );

        assertEquals(ErrorCode.UPSTREAM_SERVICE_ERROR, exception.getErrorCode());
        assertEquals("초대가 유효하지 않습니다.", exception.getMessage());
    }

    @Test
    void postConvertsHttpErrorResponse() {
        server.expect(requestTo(BASE_URL + PATH))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "success": false,
                                  "data": null,
                                  "error": {
                                    "code": "INV001",
                                    "message": "이미 사용된 초대입니다.",
                                    "fieldErrors": []
                                  }
                                }
                                """));

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> inventoryClient.post(PATH, Map.of(), TestPayload.class)
        );

        assertEquals(ErrorCode.UPSTREAM_SERVICE_ERROR, exception.getErrorCode());
        assertEquals("이미 사용된 초대입니다.", exception.getMessage());
    }

    @Test
    void postConvertsConnectionFailure() {
        server.expect(requestTo(BASE_URL + PATH))
                .andRespond(request -> {
                    throw new ResourceAccessException("connection refused");
                });

        UpstreamServiceException exception = assertThrows(
                UpstreamServiceException.class,
                () -> inventoryClient.post(PATH, Map.of(), TestPayload.class)
        );

        assertEquals(ErrorCode.UPSTREAM_SERVICE_ERROR, exception.getErrorCode());
        assertEquals("Inventory 서비스에 연결할 수 없습니다.", exception.getMessage());
    }

    private record TestPayload(String value) {
    }
}
