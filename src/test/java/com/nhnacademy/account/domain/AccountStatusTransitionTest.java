package com.nhnacademy.account.domain;

import com.nhnacademy.account.global.error.exception.ConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountStatusTransitionTest {

    @Test
    void activeAccountCanBeLocked() {
        Account account = account();

        account.changeStatus(AccountStatusAction.LOCK);

        assertEquals(AccountStatus.LOCKED, account.getAccountStatus());
    }

    @Test
    void lockedAccountCanBeUnlocked() {
        Account account = account();
        account.lock();

        account.changeStatus(AccountStatusAction.UNLOCK);

        assertEquals(AccountStatus.ACTIVE, account.getAccountStatus());
    }

    @Test
    void activeAccountCanBeDeactivated() {
        Account account = account();

        account.changeStatus(AccountStatusAction.DEACTIVATE);

        assertEquals(AccountStatus.INACTIVE, account.getAccountStatus());
    }

    @Test
    void inactiveAccountCanBeReactivated() {
        Account account = account();
        account.deactivate();

        account.changeStatus(AccountStatusAction.REACTIVATE);

        assertEquals(AccountStatus.ACTIVE, account.getAccountStatus());
    }

    @Test
    void transitionsOtherThanTheFourAllowedOnesAreRejected() {
        Account lockedAccount = account();
        lockedAccount.lock();
        Account inactiveAccount = account();
        inactiveAccount.deactivate();

        assertThrows(ConflictException.class,
                () -> account().changeStatus(AccountStatusAction.UNLOCK));
        assertThrows(ConflictException.class,
                () -> account().changeStatus(AccountStatusAction.REACTIVATE));
        assertThrows(ConflictException.class,
                () -> lockedAccount.changeStatus(AccountStatusAction.DEACTIVATE));
        assertThrows(ConflictException.class,
                () -> inactiveAccount.changeStatus(AccountStatusAction.LOCK));
    }

    @Test
    void withdrawnAccountRejectsEveryStatusTransition() {
        for (AccountStatusAction action : AccountStatusAction.values()) {
            Account account = account();
            account.withdraw();

            assertThrows(ConflictException.class,
                    () -> account.changeStatus(action));
            assertEquals(AccountStatus.WITHDRAWN, account.getAccountStatus());
        }
    }

    private Account account() {
        return new Account("test", "test@test.com", "hashed");
    }
}
