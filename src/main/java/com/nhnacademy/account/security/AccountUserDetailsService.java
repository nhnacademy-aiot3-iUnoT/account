package com.nhnacademy.account.security;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @NullMarked
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

        return new AccountPrincipal(
                account.getUuid(),
                account.getEmail(),
                account.getHashedPassword()
        );
    }
}
