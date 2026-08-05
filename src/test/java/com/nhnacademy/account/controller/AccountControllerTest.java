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
import com.nhnacademy.account.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(
        controllers = {AccountController.class, AccountAdminController.class},
        properties = "nhn.server-host=http://localhost:10404"
)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void setUp() {
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



    void viewAllAccounts() throws Exception {
        Account admin = new Account("admin", "admin@test.com", "hashed", AccountRole.ADMIN);

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
                .andExpect(jsonPath("$.data[0].hashedPassword").doesNotExist());

    }

    @Test
    void createAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(
                "test", "test@test.com", "hashed"
        );

        given(accountService.createAccount(any(CreateAccountRequest.class)))
                .willReturn(accountList.getFirst());

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }


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
                .andExpect(jsonPath("$.data[0].hashedPassword").doesNotExist());
    }


    void getCurrentAccount() throws Exception {
        Account account = accountList.getFirst();

        given(accountService.findAccount(account.getUuid()))
                .willReturn(accountList.getFirst());

        authenticate(account.getUuid());

        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(accountList.getFirst().getUuid().toString()))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.hashedPassword").doesNotExist());
    }

    @Test
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
                .andExpect(jsonPath("$.data.hashedPassword").doesNotExist());
    }

    @Test
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
                .andExpect(jsonPath("$.data.hashedPassword").doesNotExist());

        then(accountService).should().updateAccountPassword(account.getUuid(), request);
    }

    @Test
    void deleteAccount() throws Exception {
        WithdrawAccountRequest request = new WithdrawAccountRequest(
                "hashed"
        );
        Account account = accountList.getFirst();

        authenticate(account.getUuid());

        mockMvc.perform(delete("/api/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void checkEmailAvailability() throws Exception {
        EmailAvailabilityRequest request = new EmailAvailabilityRequest("available@test.com");

        given(accountService.availableEmail(any(EmailAvailabilityRequest.class)))
                .willReturn(true);

        mockMvc.perform(post("/api/accounts/check-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
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
                .andExpect(jsonPath("$.data.sameAsCurrent").doesNotExist());

        then(accountService).should().availablePassword(accountUuid, request);
    }

    @Test
    void issuePasswordResetToken() throws Exception {
        String email = "test@test.com";
        ResetPasswordTokenRequest request = new ResetPasswordTokenRequest(email);

        given(accountService.existsByEmail(email)).willReturn(true);

        mockMvc.perform(post("/api/accounts/pwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

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
                .andExpect(jsonPath("$.success").value(true));

        then(accountService).should().findAccountByEmail(email);
        then(accountService).should().updateAccountPassword(account.getUuid(), request);
        then(redisTemplate).should().delete("pwd-reset:email:" + email);
    }

    @Test
    void rejectInvalidPasswordResetToken() throws Exception {
        String token = "b".repeat(64);
        UpdateAccountPasswordRequest request = new UpdateAccountPasswordRequest("new-password");

        given(valueOperations.getAndDelete("pwd-reset:token:" + token)).willReturn(null);

        mockMvc.perform(post("/api/accounts/pwd/reset/{token}", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A001"));
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
