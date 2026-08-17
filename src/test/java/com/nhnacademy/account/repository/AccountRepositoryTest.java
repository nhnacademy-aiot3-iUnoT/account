package com.nhnacademy.account.repository;


import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
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

    @Test
    @DisplayName("전체 이메일 조회는 정확히 일치하는 계정만 반환한다")
    void findByFullEmailUsesExactMatch() {
        Account exactAccount = new Account("exact", "admin@gmail.com", "hashed-password");
        Account prefixedAccount = new Account("prefixed", "admin@gmail.com.example", "hashed-password");
        accountRepository.saveAllAndFlush(List.of(exactAccount, prefixedAccount));
        entityManager.clear();

        Account result = accountRepository
                .findByEmailAndAccountStatusNot(
                        "admin@gmail.com",
                        AccountStatus.WITHDRAWN
                )
                .orElseThrow();

        assertThat(result.getUuid()).isEqualTo(exactAccount.getUuid());
    }

    @Test
    @DisplayName("탈퇴 계정의 이메일과 비밀번호 해시는 null로 저장한다")
    void withdrawnAccountCanPersistNullCredentials() {
        Account account = new Account("tester", "tester@example.com", "hashed-password");
        accountRepository.saveAndFlush(account);

        account.withdraw();
        accountRepository.flush();
        entityManager.clear();

        Account withdrawnAccount = accountRepository.findByUuid(account.getUuid())
                .orElseThrow();

        assertThat(withdrawnAccount.getEmail()).isNull();
        assertThat(withdrawnAccount.getHashedPassword()).isNull();
        assertThat(withdrawnAccount.isWithdrawn()).isTrue();
    }

    @Test
    @DisplayName("내부 UUID 조회에서 탈퇴 계정만 제외한다")
    void findAccountsByUuidsExcludesOnlyWithdrawnAccounts() {
        Account activeAccount = new Account("active", "active@example.com", "hashed-password");
        Account lockedAccount = new Account("locked", "locked@example.com", "hashed-password");
        Account inactiveAccount = new Account("inactive", "inactive@example.com", "hashed-password");
        Account withdrawnAccount = new Account("withdrawn", "withdrawn@example.com", "hashed-password");

        lockedAccount.lock();
        inactiveAccount.deactivate();
        withdrawnAccount.withdraw();
        accountRepository.saveAllAndFlush(List.of(
                activeAccount,
                lockedAccount,
                inactiveAccount,
                withdrawnAccount
        ));
        entityManager.clear();

        List<Account> result = accountRepository
                .findAccountsByUuidIsInAndAccountStatusNot(
                        List.of(
                                activeAccount.getUuid(),
                                lockedAccount.getUuid(),
                                inactiveAccount.getUuid(),
                                withdrawnAccount.getUuid()
                        ),
                        AccountStatus.WITHDRAWN
                );

        assertThat(result)
                .extracting(Account::getUuid)
                .containsExactlyInAnyOrder(
                        activeAccount.getUuid(),
                        lockedAccount.getUuid(),
                        inactiveAccount.getUuid()
                );
    }
}
