package com.nhnacademy.account.controller;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.global.error.ErrorCode;
import com.nhnacademy.account.global.error.exception.BadRequestException;
import com.nhnacademy.account.service.AccountService;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(AccountsInternalController.class)
class AccountsInternalControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AccountService accountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    @DisplayName("GET - 이메일로 회원 검색")
    void searchAccountsByEmail() throws Exception {
        String email = "member1";
        List<Account> accounts = List.of(
                new Account("member1", "member1@test.com", "hashed-password"),
                new Account("member2", "member1@example.com", "hashed-password")
        );
        given(accountService.findAllByEmail(email)).willReturn(accounts);

        mockMvc.perform(get("/api/accounts/internal/search")
                        .queryParam("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountUuid").value(accounts.get(0).getUuid().toString()))
                .andExpect(jsonPath("$[0].email").value("member1@test.com"))
                .andExpect(jsonPath("$[1].accountUuid").value(accounts.get(1).getUuid().toString()))
                .andExpect(jsonPath("$[1].email").value("member1@example.com"))
                .andExpect(jsonPath("$[0].accountId").doesNotExist())
                .andExpect(jsonPath("$[0].hashedPassword").doesNotExist())
                .andExpect(jsonPath("$[0].name").doesNotExist());

        then(accountService).should().findAllByEmail(email);
    }

    @Test
    @DisplayName("GET - UUID 목록으로 회원 검색")
    void searchAccountsByUuids() throws Exception {
        List<Account> accounts = List.of(
                new Account("member1", "member1@test.com", "hashed-password"),
                new Account("member2", "member2@test.com", "hashed-password")
        );
        List<String> uuids = accounts.stream()
                .map(account -> account.getUuid().toString())
                .toList();
        given(accountService.findByUuids(uuids)).willReturn(accounts);

        mockMvc.perform(get("/api/accounts/internal")
                        .queryParam("uuids", uuids.toArray(String[]::new)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountUuid").value(uuids.get(0)))
                .andExpect(jsonPath("$[0].email").value("member1@test.com"))
                .andExpect(jsonPath("$[1].accountUuid").value(uuids.get(1)))
                .andExpect(jsonPath("$[1].email").value("member2@test.com"))
                .andExpect(jsonPath("$[0].accountId").doesNotExist())
                .andExpect(jsonPath("$[0].hashedPassword").doesNotExist())
                .andExpect(jsonPath("$[0].name").doesNotExist());

        then(accountService).should().findByUuids(uuids);
    }

    @Test
    @DisplayName("GET - 잘못된 UUID는 400 응답")
    void searchAccountsByInvalidUuid() throws Exception {
        String invalidUuid = "invalid-uuid";
        willThrow(new BadRequestException(ErrorCode.INVALID_INPUT))
                .given(accountService)
                .findByUuids(List.of(invalidUuid));

        mockMvc.perform(get("/api/accounts/internal")
                        .queryParam("uuids", invalidUuid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("G001"));

        then(accountService).should().findByUuids(List.of(invalidUuid));
    }
}
