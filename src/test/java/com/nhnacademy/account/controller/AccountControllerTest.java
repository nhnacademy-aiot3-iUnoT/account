package com.nhnacademy.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.CreateAccountRequest;
import com.nhnacademy.account.dto.UpdateAccountRequest;
import com.nhnacademy.account.dto.WithdrawAccountRequest;
import com.nhnacademy.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
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


    @Test
    void viewAllAccounts() throws Exception {

        given(accountService.findAll())
                .willReturn(accountList);

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk());

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
                .andExpect(status().isOk());
    }

    @Test
    void getCurrentAccount() throws Exception {
        given(accountService.findAccount(any()))
                .willReturn(accountList.getFirst());

        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isOk());
    }

    @Test
    void updateAccount() throws Exception {
        UpdateAccountRequest request = new UpdateAccountRequest(
                UUID.randomUUID(),"test", "test@test.com", "hashed"
        );

        given(accountService.updateAccount(any(UpdateAccountRequest.class)))
                .willReturn(accountList.getFirst());

        mockMvc.perform(post("/api/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteAccount() throws Exception {
        WithdrawAccountRequest request = new WithdrawAccountRequest(
                UUID.randomUUID()
        );

        mockMvc.perform(delete("/api/accounts/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}