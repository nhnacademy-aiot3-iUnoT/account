package com.nhnacademy.account.repository;


import com.nhnacademy.account.domain.Account;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AccountRepositoryTest {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("UUID를 BINARY(16)으로 저장 및 조회")
    void saveAndSelectWithUUID() {
        Account account = new Account("tester", "tester@example.com", "hashed-password");

        Account savedAccount = accountRepository.saveAndFlush(account);
        UUID expectedUuid = savedAccount.getUuid();
        Long accountId = savedAccount.getAccountId();

        entityManager.clear();

        Account found = accountRepository.findByUuid(expectedUuid)
                .orElseThrow();

        assertThat(found.getAccountId()).isEqualTo(accountId);
        assertThat(found.getUuid()).isEqualTo(expectedUuid);

        Integer byteLength = jdbcTemplate.queryForObject(
                "SELECT OCTET_LENGTH(uuid) FROM accounts WHERE account_id = ?",
                Integer.class,
                accountId
        );

        assertThat(byteLength).isEqualTo(16);
    }

    @Test
    void noUuidReturnsEmpty() {
        assertThat(accountRepository.findByUuid(UUID.randomUUID())).isEmpty();
    }

    @Test
    void existsAccountWithEmail() {
        Account account =
                new Account("tester", "tester@example.com", "hashed-password");

        accountRepository.saveAndFlush(account);
        entityManager.clear();

        assertThat(accountRepository.findByEmail("tester@example.com"))
                .isPresent();

        assertThat(accountRepository.existsByEmail("tester@example.com"))
                .isTrue();

        assertThat(accountRepository.existsByEmail("missing@example.com"))
                .isFalse();
    }
}
