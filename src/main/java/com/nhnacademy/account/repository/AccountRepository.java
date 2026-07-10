package com.nhnacademy.account.repository;

import com.nhnacademy.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUuid(UUID uuid);
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);
}
