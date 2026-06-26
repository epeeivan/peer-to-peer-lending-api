package com.taf.p2plending.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taf.p2plending.common.exception.EmailAlreadyUsedException;
import com.taf.p2plending.common.exception.InsufficientFundsException;
import com.taf.p2plending.ledger.LedgerRepository;
import com.taf.p2plending.support.AbstractIntegrationTest;
import com.taf.p2plending.user.UserService;
import com.taf.p2plending.user.dto.CreateUserRequest;
import com.taf.p2plending.user.dto.UserResponse;
import com.taf.p2plending.wallet.dto.WalletResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WalletServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Test
    void deposit_then_withdraw_updates_balance_and_ledger() {
        UserResponse user = userService.createUser(new CreateUserRequest("Alice", "alice@example.com"));

        WalletResponse afterDeposit = walletService.deposit(user.id(), new BigDecimal("1000.00"));
        assertThat(afterDeposit.balance()).isEqualByComparingTo("1000.00");

        WalletResponse afterWithdraw = walletService.withdraw(user.id(), new BigDecimal("250.50"));
        assertThat(afterWithdraw.balance()).isEqualByComparingTo("749.50");

        assertThat(walletService.getBalance(user.id()).balance()).isEqualByComparingTo("749.50");
        assertThat(ledgerRepository.findByWalletIdOrderByIdAsc(user.walletId())).hasSize(2);
    }

    @Test
    void withdraw_more_than_balance_is_rejected_and_balance_unchanged() {
        UserResponse user = userService.createUser(new CreateUserRequest("Bob", "bob@example.com"));
        walletService.deposit(user.id(), new BigDecimal("100.00"));

        assertThatThrownBy(() -> walletService.withdraw(user.id(), new BigDecimal("100.01")))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(walletService.getBalance(user.id()).balance()).isEqualByComparingTo("100.00");
    }

    @Test
    void duplicate_email_is_rejected() {
        userService.createUser(new CreateUserRequest("Carol", "carol@example.com"));

        assertThatThrownBy(() ->
                userService.createUser(new CreateUserRequest("Carol Two", "carol@example.com")))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }
}
