package com.nhnacademy.account.controller;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.dto.response.InternalAccountInfoResponse;
import com.nhnacademy.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts/internal")
@RequiredArgsConstructor
public class AccountsInternalController {
    private final AccountService accountService;

    @GetMapping("/search")
    public ResponseEntity<List<InternalAccountInfoResponse>> searchAccounts(@RequestParam String email) {

        List<Account> accounts = accountService.findAllByEmail(email);
        List<InternalAccountInfoResponse> responseList = accounts.stream().map(InternalAccountInfoResponse::from).toList();

        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<InternalAccountInfoResponse>> searchAccounts(@RequestParam List<String> uuids) {

        List<Account> accounts = accountService.findByUuids(uuids);
        List<InternalAccountInfoResponse> responseList = accounts.stream().map(InternalAccountInfoResponse::from).toList();

        return ResponseEntity.ok(responseList);
    }

    @DeleteMapping("accounts")
    public ResponseEntity<InternalAccountInfoResponse> deleteAccounts(@RequestParam(name = "account-uuid") String accountUuid) {
        UUID uuid = UUID.fromString(accountUuid);

        InternalAccountInfoResponse response = accountService.withdrawAccount(uuid);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts/bulk-delete")
    public ResponseEntity<Void> bulkDeleteAccounts(@RequestBody List<String> uuids) {
        List<UUID> uuidList = uuids.stream().map(UUID::fromString).toList();

        accountService.withdrawAccountBulk(uuidList);

        return ResponseEntity.noContent().build();
    }
}
