package com.taf.p2plending.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taf.p2plending.common.exception.OverFundingException;
import com.taf.p2plending.loan.dto.FundLoanRequest;
import com.taf.p2plending.loan.dto.LoanResponse;
import com.taf.p2plending.loan.dto.RequestLoanRequest;
import com.taf.p2plending.support.AbstractIntegrationTest;
import com.taf.p2plending.user.SystemRole;
import com.taf.p2plending.user.UserRepository;
import com.taf.p2plending.user.UserService;
import com.taf.p2plending.user.dto.CreateUserRequest;
import com.taf.p2plending.wallet.WalletService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LoanFundingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void two_investors_fully_fund_loan_then_disbursed_to_borrower() {
        Long borrower = userService.createUser(new CreateUserRequest("B", "b.fund@example.com")).id();
        Long investorA = investorWithFunds("a.fund@example.com", "600.00");
        Long investorB = investorWithFunds("b2.fund@example.com", "400.00");
        BigDecimal escrowStart = escrowBalance();

        Long loanId = loanService.requestLoan(new RequestLoanRequest(borrower,
                new BigDecimal("1000.00"), new BigDecimal("0.12"), 12)).id();

        LoanResponse afterA = loanService.fundLoan(loanId, new FundLoanRequest(investorA, new BigDecimal("600.00")));
        assertThat(afterA.status()).isEqualTo(LoanStatus.PENDING);
        assertThat(afterA.fundedAmount()).isEqualByComparingTo("600.00");
        assertThat(escrowBalance()).isEqualByComparingTo(escrowStart.add(new BigDecimal("600.00")));

        LoanResponse afterB = loanService.fundLoan(loanId, new FundLoanRequest(investorB, new BigDecimal("400.00")));
        assertThat(afterB.status()).isEqualTo(LoanStatus.FUNDED);
        assertThat(afterB.fundedAmount()).isEqualByComparingTo("1000.00");
        assertThat(afterB.monthlyPayment()).isEqualByComparingTo("88.85");
        assertThat(afterB.remainingPrincipal()).isEqualByComparingTo("1000.00");

        assertThat(walletService.getBalance(investorA).balance()).isEqualByComparingTo("0.00");
        assertThat(walletService.getBalance(investorB).balance()).isEqualByComparingTo("0.00");
        assertThat(walletService.getBalance(borrower).balance()).isEqualByComparingTo("1000.00");
        assertThat(escrowBalance()).isEqualByComparingTo(escrowStart);
    }

    @Test
    void funding_more_than_remaining_is_rejected() {
        Long borrower = userService.createUser(new CreateUserRequest("B3", "b3.fund@example.com")).id();
        Long investor = investorWithFunds("c.fund@example.com", "2000.00");
        Long loanId = loanService.requestLoan(new RequestLoanRequest(borrower,
                new BigDecimal("1000.00"), new BigDecimal("0.12"), 12)).id();
        loanService.fundLoan(loanId, new FundLoanRequest(investor, new BigDecimal("600.00")));

        assertThatThrownBy(() ->
                loanService.fundLoan(loanId, new FundLoanRequest(investor, new BigDecimal("500.00"))))
                .isInstanceOf(OverFundingException.class);

        assertThat(loanService.getLoan(loanId).fundedAmount()).isEqualByComparingTo("600.00");
    }

    private Long investorWithFunds(String email, String funds) {
        Long id = userService.createUser(new CreateUserRequest("Inv", email)).id();
        walletService.deposit(id, new BigDecimal(funds));
        return id;
    }

    private BigDecimal escrowBalance() {
        Long escrowUserId = userRepository.findBySystemRole(SystemRole.ESCROW).orElseThrow().getId();
        return walletService.getBalance(escrowUserId).balance();
    }
}
