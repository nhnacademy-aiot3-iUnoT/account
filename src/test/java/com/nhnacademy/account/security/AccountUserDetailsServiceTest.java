package com.nhnacademy.account.security;

import com.nhnacademy.account.domain.Account;
import com.nhnacademy.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AccountUserDetailsServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountUserDetailsService userDetailsService;

    @Test
    void loadsActiveAccount() {
        Account account = new Account("test", "test@test.com", "hashed-password");
        given(accountRepository.findByEmail(account.getEmail()))
                .willReturn(Optional.of(account));

        UserDetails userDetails = userDetailsService.loadUserByUsername(account.getEmail());

        assertEquals(account.getEmail(), userDetails.getUsername());
        assertEquals(account.getHashedPassword(), userDetails.getPassword());
    }

    @Test
    void rejectsNonActiveAccount() {
        Account account = new Account("test", "test@test.com", "hashed-password");
        account.lock();
        given(accountRepository.findByEmail(account.getEmail()))
                .willReturn(Optional.of(account));

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(account.getEmail())
        );
    }
}
