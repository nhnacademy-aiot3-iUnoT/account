package com.nhnacademy.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountRole;
import com.nhnacademy.account.dto.request.ChangeAccountStatusRequest;
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
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @DisplayName("GET - 전체 회원 조회")
    void viewAllAccounts() throws Exception {
        Account admin = new Account("admin", "admin@test.com", "hashed", AccountRole.ADMIN);
        Account user = new Account("test", "test@test.com", "hashed", AccountRole.USER);

        List<Account> accountList = List.of(user);

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
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].hashedPassword").doesNotExist())
                .andDo(document("admin-list-accounts"));

    }

    @Test
    @DisplayName("PUT - 회원 상태 변경")
    void changeAccountStatus() throws Exception {
        UUID uuid = UUID.randomUUID();
        ChangeAccountStatusRequest request =
                new ChangeAccountStatusRequest(AccountStatusAction.LOCK, "이상 로그인 감지");
        Account account = new Account("test", "test@test.com", "hashed");
        Account admin = new Account("admin", "admin@test.com", "hashed", AccountRole.ADMIN);
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
                .andDo(document("admin-change-account-status"));

        then(accountService).should().changeAccountStatus(uuid, request);
    }

    @Test
    @DisplayName("PUT - 회원 이름 수정")
    void updateAccountName() throws Exception {
        UUID uuid = UUID.randomUUID();
        UpdateAccountNameRequest request = new UpdateAccountNameRequest("updated");
        Account account = new Account("test", "test@test.com", "hashed");
        Account admin = new Account("admin", "admin@test.com", "hashed", AccountRole.ADMIN);

        given(accountService.findAccount(admin.getUuid()))
                .willReturn(admin);
        given(accountService.updateAccountName(uuid, request))
                .willReturn(account);

        authenticate(admin.getUuid());

        mockMvc.perform(put("/api/accounts/admin/{uuid}", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("admin-update-account-name"));

        then(accountService).should().updateAccountName(uuid, request);
    }

    @Test
    @DisplayName("PUT - 회원 비밀번호 수정")
    void updateAccountPassword() throws Exception {
        UUID uuid = UUID.randomUUID();
        UpdateAccountPasswordRequest request = new UpdateAccountPasswordRequest("new-password");
        Account account = new Account("test", "test@test.com", "hashed");
        Account admin = new Account("admin", "admin@test.com", "hashed", AccountRole.ADMIN);

        given(accountService.findAccount(admin.getUuid()))
                .willReturn(admin);
        given(accountService.updateAccountPassword(uuid, request))
                .willReturn(account);

        authenticate(admin.getUuid());

        mockMvc.perform(put("/api/accounts/admin/{uuid}/pwd", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("admin-update-account-password"));

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
                .andDo(document("admin-change-account-status-validation-error"));

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
                .andDo(document("admin-change-account-status-forbidden"));

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
}
