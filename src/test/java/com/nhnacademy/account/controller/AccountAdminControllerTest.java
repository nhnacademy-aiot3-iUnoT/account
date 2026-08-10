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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountAdminController.class)
class AccountAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AccountService accountService;

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
                .andExpect(jsonPath("$.data.accountRole").value("ADMIN"));

        then(accountService).should().createAdminAccount(request);
    }

    @Test
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
                .andExpect(jsonPath("$.data.accountStatus").value("LOCKED"));

        then(accountService).should().changeAccountStatus(uuid, request);
    }

    @Test
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
                .andExpect(status().isOk());

        then(accountService).should().updateAccountName(uuid, request);
    }

    @Test
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
                .andExpect(status().isOk());

        then(accountService).should().updateAccountPassword(uuid, request);
    }

    @Test
    void reasonIsRequired() throws Exception {
        UUID uuid = UUID.randomUUID();
        ChangeAccountStatusRequest request =
                new ChangeAccountStatusRequest(AccountStatusAction.LOCK, " ");
        Account admin = new Account("admin", "admin@test.com", "hashed", AccountRole.ADMIN);

        authenticate(admin.getUuid());

        mockMvc.perform(put("/api/accounts/admin/{uuid}/status", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        then(accountService).shouldHaveNoInteractions();
    }

    @Test
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
                .andExpect(jsonPath("$.error.code").value("AU006"));

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
