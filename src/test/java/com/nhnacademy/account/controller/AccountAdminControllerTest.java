package com.nhnacademy.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountRole;
import com.nhnacademy.account.dto.request.ChangeAccountStatusRequest;
import com.nhnacademy.account.dto.request.CreateAdminAccountRequest;
import com.nhnacademy.account.dto.request.UpdateAccountNameRequest;
import com.nhnacademy.account.dto.request.UpdateAccountPasswordRequest;
import com.nhnacademy.account.domain.AccountStatusAction;
import com.nhnacademy.account.service.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
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

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(AccountAdminController.class)
class AccountAdminControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AccountService accountService;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAdminAccountWithoutInviteToken() throws Exception {
        CreateAdminAccountRequest request = new CreateAdminAccountRequest(
                "new-admin",
                "new-admin@test.com",
                "password"
        );
        Account requester = new Account("admin", "admin@test.com", "hashed", AccountRole.ADMIN);
        Account created = new Account(
                request.name(),
                request.email(),
                "hashed",
                AccountRole.ADMIN
        );

        given(accountService.findAccount(requester.getUuid()))
                .willReturn(requester);
        given(accountService.createAdminAccount(request))
                .willReturn(created);
        authenticate(requester.getUuid());

        mockMvc.perform(post("/api/accounts/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(request.email()))
                .andExpect(jsonPath("$.data.accountRole").value("ADMIN"))
                .andDo(document("admin-create-account",
                        requestFields(
                                fieldWithPath("name").description("관리자 이름"),
                                fieldWithPath("email").description("관리자 이메일"),
                                fieldWithPath("password").description("관리자 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("생성된 관리자 정보"),
                                fieldWithPath("data.uuid").description("관리자 UUID"),
                                fieldWithPath("data.name").description("관리자 이름"),
                                fieldWithPath("data.email").description("관리자 이메일"),
                                fieldWithPath("data.accountRole").description("관리자 권한"),
                                fieldWithPath("data.accountStatus").description("관리자 상태"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        then(accountService).should().createAdminAccount(request);
    }

    @Test
    @DisplayName("GET - 전체 회원 조회")
    void viewAllAccounts() throws Exception {
        Account admin = persistedAccount("admin", "admin@test.com", "hashed", AccountRole.ADMIN);
        Account user = persistedAccount("test", "test@test.com", "hashed", AccountRole.USER);
        Account withdrawnUser = persistedAccount("withdrawn", "withdrawn@test.com", "hashed", AccountRole.USER);
        withdrawnUser.withdraw();

        List<Account> accountList = List.of(user, withdrawnUser);

        given(accountService.findAccount(admin.getUuid()))
                .willReturn(admin);
        given(accountService.findAll())
                .willReturn(accountList);

        authenticate(admin.getUuid());

        mockMvc.perform(get("/api/accounts/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].uuid").value(accountList.getFirst().getUuid().toString()))
                .andExpect(jsonPath("$.data[0].name").value("test"))
                .andExpect(jsonPath("$.data[0].email").value("test@test.com"))
                .andExpect(jsonPath("$.data[0].accountRole").value("USER"))
                .andExpect(jsonPath("$.data[0].accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].withdrawnAt").value(nullValue()))
                .andExpect(jsonPath("$.data[1].name").value(nullValue()))
                .andExpect(jsonPath("$.data[1].email").value(nullValue()))
                .andExpect(jsonPath("$.data[1].accountStatus").value("WITHDRAWN"))
                .andExpect(jsonPath("$.data[1].withdrawnAt").exists())
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].hashedPassword").doesNotExist())
                .andDo(document("admin-list-accounts",
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("회원 목록"),
                                fieldWithPath("data[].uuid").description("회원 UUID"),
                                fieldWithPath("data[].name").optional().description("회원 이름(탈퇴 시 null)"),
                                fieldWithPath("data[].email").optional().description("회원 이메일(탈퇴 시 null)"),
                                fieldWithPath("data[].accountRole").description("회원 권한"),
                                fieldWithPath("data[].accountStatus").description("회원 상태"),
                                fieldWithPath("data[].createdAt").description("회원 생성 시각"),
                                fieldWithPath("data[].updatedAt").description("회원 수정 시각"),
                                fieldWithPath("data[].withdrawnAt").optional().description("회원 탈퇴 시각(미탈퇴 시 null)"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

    }

    @Test
    @DisplayName("PUT - 회원 상태 변경")
    void changeAccountStatus() throws Exception {
        UUID uuid = UUID.randomUUID();
        ChangeAccountStatusRequest request =
                new ChangeAccountStatusRequest(AccountStatusAction.LOCK, "이상 로그인 감지");
        Account account = persistedAccount("test", "test@test.com", "hashed", AccountRole.USER);
        Account admin = persistedAccount("admin", "admin@test.com", "hashed", AccountRole.ADMIN);
        account.lock();

        given(accountService.findAccount(admin.getUuid()))
                .willReturn(admin);
        given(accountService.changeAccountStatus(eq(uuid), eq(request)))
                .willReturn(account);

        authenticate(admin.getUuid());

        mockMvc.perform(put("/api/accounts/admin/{uuid}/status", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("LOCKED"))
                .andDo(document("admin-change-account-status",
                        requestFields(
                                fieldWithPath("action").description("상태 변경 작업(LOCK, UNLOCK, DEACTIVATE, REACTIVATE)"),
                                fieldWithPath("reason").description("상태 변경 사유")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("data").description("변경된 회원 정보"),
                                fieldWithPath("data.uuid").description("회원 UUID"),
                                fieldWithPath("data.name").description("회원 이름"),
                                fieldWithPath("data.email").description("회원 이메일"),
                                fieldWithPath("data.accountRole").description("회원 권한"),
                                fieldWithPath("data.accountStatus").description("변경된 회원 상태"),
                                fieldWithPath("data.createdAt").description("회원 생성 시각"),
                                fieldWithPath("data.updatedAt").description("회원 수정 시각"),
                                fieldWithPath("data.withdrawnAt").description("회원 탈퇴 시각(미탈퇴 시 null)"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        then(accountService).should().changeAccountStatus(uuid, request);
    }

    @Test
    @DisplayName("PUT - 회원 이름 수정")
    void updateAccountName() throws Exception {
        UUID uuid = UUID.randomUUID();
        UpdateAccountNameRequest request = new UpdateAccountNameRequest("updated");
        Account account = persistedAccount("test", "test@test.com", "hashed", AccountRole.USER);
        Account admin = persistedAccount("admin", "admin@test.com", "hashed", AccountRole.ADMIN);

        given(accountService.findAccount(admin.getUuid()))
                .willReturn(admin);
        given(accountService.updateAccountName(uuid, request))
                .willReturn(account);

        authenticate(admin.getUuid());

        mockMvc.perform(put("/api/accounts/admin/{uuid}", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("admin-update-account-name",
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
                                fieldWithPath("data.createdAt").description("회원 생성 시각"),
                                fieldWithPath("data.updatedAt").description("회원 수정 시각"),
                                fieldWithPath("data.withdrawnAt").description("회원 탈퇴 시각(미탈퇴 시 null)"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        then(accountService).should().updateAccountName(uuid, request);
    }

    @Test
    @DisplayName("PUT - 회원 비밀번호 수정")
    void updateAccountPassword() throws Exception {
        UUID uuid = UUID.randomUUID();
        UpdateAccountPasswordRequest request = new UpdateAccountPasswordRequest("new-password");
        Account account = persistedAccount("test", "test@test.com", "hashed", AccountRole.USER);
        Account admin = persistedAccount("admin", "admin@test.com", "hashed", AccountRole.ADMIN);

        given(accountService.findAccount(admin.getUuid()))
                .willReturn(admin);
        given(accountService.updateAccountPassword(uuid, request))
                .willReturn(account);

        authenticate(admin.getUuid());

        mockMvc.perform(put("/api/accounts/admin/{uuid}/pwd", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("admin-update-account-password",
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
                                fieldWithPath("data.createdAt").description("회원 생성 시각"),
                                fieldWithPath("data.updatedAt").description("회원 수정 시각"),
                                fieldWithPath("data.withdrawnAt").description("회원 탈퇴 시각(미탈퇴 시 null)"),
                                fieldWithPath("error").description("오류 정보"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        then(accountService).should().updateAccountPassword(uuid, request);
    }

    @Test
    @DisplayName("PUT - 회원 상태 변경 사유 필수 검증")
    void reasonIsRequired() throws Exception {
        UUID uuid = UUID.randomUUID();
        ChangeAccountStatusRequest request =
                new ChangeAccountStatusRequest(AccountStatusAction.LOCK, " ");
        Account admin = new Account("admin", "admin@test.com", "hashed", AccountRole.ADMIN);

        authenticate(admin.getUuid());

        mockMvc.perform(put("/api/accounts/admin/{uuid}/status", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andDo(document("admin-change-account-status-validation-error",
                        requestFields(
                                fieldWithPath("action").description("상태 변경 작업(LOCK, UNLOCK, DEACTIVATE, REACTIVATE)"),
                                fieldWithPath("reason").description("상태 변경 사유")
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
    @DisplayName("PUT - 일반 회원의 관리자 API 접근 거부")
    void rejectsNonAdminAccount() throws Exception {
        UUID targetUuid = UUID.randomUUID();
        Account user = new Account("user", "user@test.com", "hashed");
        ChangeAccountStatusRequest request =
                new ChangeAccountStatusRequest(AccountStatusAction.LOCK, "이상 로그인 감지");

        given(accountService.findAccount(user.getUuid()))
                .willReturn(user);
        authenticate(user.getUuid());

        mockMvc.perform(put("/api/accounts/admin/{uuid}/status", targetUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AU006"))
                .andDo(document("admin-change-account-status-forbidden",
                        requestFields(
                                fieldWithPath("action").description("상태 변경 작업(LOCK, UNLOCK, DEACTIVATE, REACTIVATE)"),
                                fieldWithPath("reason").description("상태 변경 사유")
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

        then(accountService).should().findAccount(user.getUuid());
        then(accountService).shouldHaveNoMoreInteractions();
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

    private Account persistedAccount(
            String name,
            String email,
            String hashedPassword,
            AccountRole role
    ) {
        Account account = new Account(name, email, hashedPassword, role);
        LocalDateTime persistedAt = LocalDateTime.of(2026, 8, 13, 12, 0);
        ReflectionTestUtils.setField(account, "createdAt", persistedAt);
        ReflectionTestUtils.setField(account, "updatedAt", persistedAt);
        return account;
    }
}
