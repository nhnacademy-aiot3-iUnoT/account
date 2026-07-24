package com.nhnacademy.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.ChangeAccountStatusRequest;
import com.nhnacademy.account.domain.AccountStatusAction;
import com.nhnacademy.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountAdminController.class)
class AccountAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AccountService accountService;


    void changeAccountStatus() throws Exception {
        UUID uuid = UUID.randomUUID();
        ChangeAccountStatusRequest request =
                new ChangeAccountStatusRequest(AccountStatusAction.LOCK, "이상 로그인 감지");
        Account account = new Account("test", "test@test.com", "hashed");
        account.lock();

        given(accountService.changeAccountStatus(eq(uuid), eq(request)))
                .willReturn(account);

        mockMvc.perform(patch("/api/admin/accounts/{uuid}/status", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("LOCKED"));

        then(accountService).should().changeAccountStatus(uuid, request);
    }

    @Test
    void reasonIsRequired() throws Exception {
        UUID uuid = UUID.randomUUID();
        ChangeAccountStatusRequest request =
                new ChangeAccountStatusRequest(AccountStatusAction.LOCK, " ");

        mockMvc.perform(patch("/api/admin/accounts/{uuid}/status", uuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        then(accountService).shouldHaveNoInteractions();
    }
}
