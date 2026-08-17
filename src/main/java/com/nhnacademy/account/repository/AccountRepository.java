package com.nhnacademy.account.repository;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.domain.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUuid(UUID uuid);
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Account> findAllByEmailStartingWithAndAccountStatusNot(
            String email,
            AccountStatus accountStatus
    );
    List<Account> findAccountsByUuidIsInAndAccountStatusNot(
            List<UUID> uuids,
            AccountStatus accountStatus
    );
}
