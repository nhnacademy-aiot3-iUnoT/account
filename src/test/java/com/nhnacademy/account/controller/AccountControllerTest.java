package com.nhnacademy.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountRole;
import com.nhnacademy.account.dto.request.CreateAccountRequest;
import com.nhnacademy.account.dto.request.EmailAvailabilityRequest;
import com.nhnacademy.account.dto.request.PasswordReuseCheckRequest;
import com.nhnacademy.account.dto.request.ResetPasswordTokenRequest;
import com.nhnacademy.account.dto.request.UpdateAccountNameRequest;
import com.nhnacademy.account.dto.request.UpdateAccountPasswordRequest;
import com.nhnacademy.account.dto.request.WithdrawAccountRequest;
import com.nhnacademy.account.dto.response.CreateAccountResponse;
import com.nhnacademy.account.service.AccountService;
import com.nhnacademy.account.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(
        controllers = {AccountController.class, AccountAdminController.class},
        properties = "nhn.server-host=http://localhost:10404"
)
class AccountControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper = new ObjectMapper();

    private List<Account> accountList;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation))
                .build();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        accountList = List.of(
                new Account("test", "test@test.com", "hashed"),
                new Account("test2", "test2@test2.com", "hashed2")
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    @Test
    @DisplayName("POST - 회원가입")
    void createAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(
                UUID.randomUUID(), "test", "test@test.com", "hashed"
        );

        given(accountService.createAccount(any(CreateAccountRequest.class)))
                .willReturn(new CreateAccountResponse(true));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("create-account",
                        requestFields(
                                fieldWithPath("inviteToken").description("초대 토큰"),
                                fieldWithPath("name").description("회원 이름"),
                                fieldWithPath("email").description("회원 이메일"),
                                fieldWithPath("password").description("회원 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("응답 데이터"),
                                fieldWithPath("data.isOwner").description("초대 대상 공간의 소유자 여부"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isOwner").value(true));
    }

    @Test
    void createAccountRejectsMalformedInviteToken() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteToken": "inviteToken",
                                  "name": "test",
                                  "email": "test@test.com",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("G001"))
                .andDo(document("create-account-invalid-invite-token",
                        requestFields(
                                fieldWithPath("inviteToken").description("유효하지 않은 형식의 초대 토큰"),
                                fieldWithPath("name").description("회원 이름"),
                                fieldWithPath("email").description("회원 이메일"),
                                fieldWithPath("password").description("회원 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("응답 데이터"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("error.code").description("오류 코드"),
                                fieldWithPath("error.message").description("오류 메시지"),
                                fieldWithPath("error.fieldErrors").description("필드 단위 오류 목록"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        then(accountService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET - 탈퇴 회원 개인정보 미노출")
    void withdrawnAccountDoesNotExposeRemovedPersonalInformation() throws Exception {
        Account admin = new Account("admin", "admin@test.com", "hashed", AccountRole.ADMIN);
        Account withdrawnAccount = accountList.getFirst();
        withdrawnAccount.withdraw();

        given(accountService.findAccount(admin.getUuid()))
                .willReturn(admin);
        given(accountService.findAll())
                .willReturn(List.of(withdrawnAccount));

        authenticate(admin.getUuid());

        mockMvc.perform(get("/api/accounts/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].uuid").value(withdrawnAccount.getUuid().toString()))
                .andExpect(jsonPath("$.data[0].accountStatus").value("WITHDRAWN"))
                .andExpect(jsonPath("$.data[0].name").doesNotExist())
                .andExpect(jsonPath("$.data[0].email").doesNotExist())
                .andExpect(jsonPath("$.data[0].hashedPassword").doesNotExist())
                .andDo(document("admin-list-withdrawn-accounts",
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("탈퇴 회원 목록"),
                                fieldWithPath("data[].uuid").description("회원 UUID"),
                                fieldWithPath("data[].accountRole").description("회원 권한"),
                                fieldWithPath("data[].accountStatus").description("회원 상태"),
                                fieldWithPath("data[].withdrawnAt").description("회원 탈퇴 시각"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));
    }

    @Test
    @DisplayName("GET - 본인 계정 조회")
    void getCurrentAccount() throws Exception {
        Account account = accountList.getFirst();

        given(accountService.findAccount(account.getUuid()))
                .willReturn(accountList.getFirst());

        authenticate(account.getUuid());

        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(accountList.getFirst().getUuid().toString()))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.hashedPassword").doesNotExist())
                .andDo(document("get-current-account",
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("회원 정보"),
                                fieldWithPath("data.uuid").description("회원 UUID"),
                                fieldWithPath("data.name").description("회원 이름"),
                                fieldWithPath("data.email").description("회원 이메일"),
                                fieldWithPath("data.accountRole").description("회원 권한"),
                                fieldWithPath("data.accountStatus").description("회원 상태"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));
    }

    @Test
    @DisplayName("PUT - 본인 이름 수정")
    void updateAccountName() throws Exception {
        UpdateAccountNameRequest request = new UpdateAccountNameRequest("test");
        Account account = accountList.getFirst();

        given(accountService.updateAccountName(account.getUuid(), request))
                .willReturn(accountList.getFirst());

        authenticate(account.getUuid());

        mockMvc.perform(put("/api/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(accountList.getFirst().getUuid().toString()))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.hashedPassword").doesNotExist())
                .andDo(document("update-account-name",
                        requestFields(
                                fieldWithPath("name").description("변경할 회원 이름")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("변경된 회원 정보"),
                                fieldWithPath("data.uuid").description("회원 UUID"),
                                fieldWithPath("data.name").description("회원 이름"),
                                fieldWithPath("data.email").description("회원 이메일"),
                                fieldWithPath("data.accountRole").description("회원 권한"),
                                fieldWithPath("data.accountStatus").description("회원 상태"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));
    }

    @Test
    @DisplayName("PUT - 비밀번호 수정")
    void updateAccountPassword() throws Exception {
        UpdateAccountPasswordRequest request = new UpdateAccountPasswordRequest("new-password");
        Account account = accountList.getFirst();

        given(accountService.updateAccountPassword(account.getUuid(), request))
                .willReturn(account);

        authenticate(account.getUuid());

        mockMvc.perform(put("/api/accounts/me/pwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(account.getUuid().toString()))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.hashedPassword").doesNotExist())
                .andDo(document("update-account-password",
                        requestFields(
                                fieldWithPath("password").description("변경할 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("변경된 회원 정보"),
                                fieldWithPath("data.uuid").description("회원 UUID"),
                                fieldWithPath("data.name").description("회원 이름"),
                                fieldWithPath("data.email").description("회원 이메일"),
                                fieldWithPath("data.accountRole").description("회원 권한"),
                                fieldWithPath("data.accountStatus").description("회원 상태"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        then(accountService).should().updateAccountPassword(account.getUuid(), request);
    }

    @Test
    @DisplayName("DELETE - 회원 탈퇴")
    void deleteAccount() throws Exception {
        WithdrawAccountRequest request = new WithdrawAccountRequest(
                "hashed"
        );
        Account account = accountList.getFirst();

        authenticate(account.getUuid());

        mockMvc.perform(delete("/api/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("withdraw-account",
                        requestFields(
                                fieldWithPath("password").description("본인 확인용 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("응답 데이터"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));
    }

    @Test
    @DisplayName("POST - 이메일 사용 가능 여부 확인")
    void checkEmailAvailability() throws Exception {
        EmailAvailabilityRequest request = new EmailAvailabilityRequest("available@test.com");

        given(accountService.availableEmail(any(EmailAvailabilityRequest.class)))
                .willReturn(true);

        mockMvc.perform(post("/api/accounts/check-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andDo(document("check-email-availability",
                        requestFields(
                                fieldWithPath("email").description("사용 가능 여부를 확인할 이메일")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("이메일 확인 결과"),
                                fieldWithPath("data.available").description("이메일 사용 가능 여부"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));
    }

    @Test
    @DisplayName("POST - 비밀번호 재사용 가능 여부 확인")
    void checkPasswordAvailability() throws Exception {
        PasswordReuseCheckRequest request = new PasswordReuseCheckRequest("new-password");
        UUID accountUuid = UUID.randomUUID();

        given(accountService.availablePassword(accountUuid, request))
                .willReturn(true);

        authenticate(accountUuid);

        mockMvc.perform(post("/api/accounts/check-pwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.sameAsCurrent").doesNotExist())
                .andDo(document("check-password-availability",
                        requestFields(
                                fieldWithPath("newPassword").description("재사용 여부를 확인할 새 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("비밀번호 확인 결과"),
                                fieldWithPath("data.available").description("비밀번호 사용 가능 여부"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        then(accountService).should().availablePassword(accountUuid, request);
    }

    @Test
    @DisplayName("POST - 비밀번호 재설정 토큰 발급")
    void issuePasswordResetToken() throws Exception {
        String email = "test@test.com";
        ResetPasswordTokenRequest request = new ResetPasswordTokenRequest(email);

        given(accountService.existsByEmail(email)).willReturn(true);

        mockMvc.perform(post("/api/accounts/pwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("issue-password-reset-token",
                        requestFields(
                                fieldWithPath("email").description("비밀번호를 초기화할 회원 이메일")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("응답 데이터"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        then(emailService).should().sendText(
                eq(email),
                eq("비밀번호 초기화 메일"),
                contentCaptor.capture()
        );

        String resetUrl = contentCaptor.getValue();
        assertTrue(resetUrl.matches("http://localhost:10404/pwd/[0-9a-f]{64}"));

        String token = resetUrl.substring(resetUrl.lastIndexOf('/') + 1);
        Duration ttl = Duration.ofMinutes(5);
        then(valueOperations).should().set("pwd-reset:token:" + token, email, ttl);
        then(valueOperations).should().set("pwd-reset:email:" + email, token, ttl);
    }

    @Test
    @DisplayName("POST - 비밀번호 재설정")
    void resetPasswordWithValidToken() throws Exception {
        String token = "a".repeat(64);
        String email = "test@test.com";
        Account account = accountList.getFirst();
        UpdateAccountPasswordRequest request = new UpdateAccountPasswordRequest("new-password");

        given(valueOperations.getAndDelete("pwd-reset:token:" + token)).willReturn(email);
        given(accountService.findAccountByEmail(email)).willReturn(account);
        given(accountService.updateAccountPassword(account.getUuid(), request)).willReturn(account);

        mockMvc.perform(post("/api/accounts/pwd/reset/{token}", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("reset-password",
                        requestFields(
                                fieldWithPath("password").description("변경할 새 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("응답 데이터"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        then(accountService).should().findAccountByEmail(email);
        then(accountService).should().updateAccountPassword(account.getUuid(), request);
        then(redisTemplate).should().delete("pwd-reset:email:" + email);
    }

    @Test
    @DisplayName("POST - 유효하지 않은 비밀번호 재설정 토큰 거부")
    void rejectInvalidPasswordResetToken() throws Exception {
        String token = "b".repeat(64);
        UpdateAccountPasswordRequest request = new UpdateAccountPasswordRequest("new-password");

        given(valueOperations.getAndDelete("pwd-reset:token:" + token)).willReturn(null);

        mockMvc.perform(post("/api/accounts/pwd/reset/{token}", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A001"))
                .andDo(document("reset-password-invalid-token",
                        requestFields(
                                fieldWithPath("password").description("변경할 새 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("응답 데이터"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("error.code").description("오류 코드"),
                                fieldWithPath("error.message").description("오류 메시지"),
                                fieldWithPath("error.fieldErrors").description("필드 단위 오류 목록"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));
    }

    private void authenticate(UUID accountUuid) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(accountUuid.toString())
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
