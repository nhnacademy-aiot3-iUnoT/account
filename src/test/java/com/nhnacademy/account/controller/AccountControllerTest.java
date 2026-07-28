package com.nhnacademy.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.auth.jwt.AccountJwtWebMvcConfiguration;
import com.nhnacademy.auth.jwt.AccountUuidArgumentResolver;
import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountRole;
import com.nhnacademy.account.dto.AccountResponse;
import com.nhnacademy.account.dto.crud.CreateAccountRequest;
import com.nhnacademy.account.dto.crud.UpdateAccountRequest;
import com.nhnacademy.account.dto.crud.WithdrawAccountRequest;
import com.nhnacademy.account.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest({AccountController.class, AccountAdminController.class})
@Import({AccountUuidArgumentResolver.class, AccountJwtWebMvcConfiguration.class})
@TestPropertySource(properties = "nhn.auth.jwt.enabled=true")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private List<Account> accountList;

    @BeforeEach
    void setUp() {
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

    @Test
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

    @Test
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
    void updateAccount() throws Exception {
        UpdateAccountRequest request = new UpdateAccountRequest(
                "test", "hashed"
        );
        Account account = accountList.getFirst();

        given(accountService.updateAccount(account.getUuid(), request))
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
