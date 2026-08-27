package com.nhnacademy.account.controller;

import com.nhnacademy.account.dto.request.LoginRequest;
import com.nhnacademy.account.dto.request.RefreshTokenRequest;
import com.nhnacademy.account.dto.response.LoginResponse;
import com.nhnacademy.account.service.AuthService;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String REFRESH_TOKEN = "a".repeat(32) + "." + "b".repeat(64);
    private static final String ROTATED_REFRESH_TOKEN = "c".repeat(32) + "." + "d".repeat(64);

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    @DisplayName("POST - 로그인")
    void testLogin() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@test.com", "password");

        given(authService.login(loginRequest))
                .willReturn(new LoginResponse("token-1", REFRESH_TOKEN));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(objectMapper.writeValueAsBytes(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.accessToken").value("token-1"))
                .andExpect(jsonPath("$.data.refreshToken").value(REFRESH_TOKEN))
                .andDo(document("login",
                        requestFields(
                                fieldWithPath("email").description("회원 이메일"),
                                fieldWithPath("password").description("회원 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("응답 데이터"),
                                fieldWithPath("data.accessToken").description("인증에 사용할 액세스 토큰"),
                                fieldWithPath("data.refreshToken").description("액세스 토큰 갱신에 사용할 리프레시 토큰"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        then(authService).should().login(loginRequest);
    }

    @Test
    @DisplayName("POST - 토큰 갱신")
    void refreshRotatesRefreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);
        given(authService.refresh(request))
                .willReturn(new LoginResponse("token-2", ROTATED_REFRESH_TOKEN));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.accessToken").value("token-2"))
                .andExpect(jsonPath("$.data.refreshToken").value(ROTATED_REFRESH_TOKEN))
                .andDo(document("refresh-token",
                        requestFields(
                                fieldWithPath("refreshToken").description("현재 리프레시 토큰")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("응답 데이터"),
                                fieldWithPath("data.accessToken").description("새로 발급된 액세스 토큰"),
                                fieldWithPath("data.refreshToken").description("회전된 새 리프레시 토큰"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        then(authService).should().refresh(request);
    }

    @Test
    @DisplayName("POST - 로그아웃")
    void logoutRevokesRefreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isNoContent())
                .andDo(document("logout",
                        requestFields(
                                fieldWithPath("refreshToken").description("폐기할 리프레시 토큰")
                        )
                ));

        then(authService).should().logout(request);
    }
}
