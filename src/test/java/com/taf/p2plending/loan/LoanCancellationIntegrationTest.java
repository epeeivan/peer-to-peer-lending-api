package com.taf.p2plending.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taf.p2plending.loan.dto.FundLoanRequest;
import com.taf.p2plending.loan.dto.RequestLoanRequest;
import com.taf.p2plending.support.AbstractIntegrationTest;
import com.taf.p2plending.user.SystemRole;
import com.taf.p2plending.user.UserRepository;
import com.taf.p2plending.user.UserService;
import com.taf.p2plending.user.dto.CreateUserRequest;
import com.taf.p2plending.wallet.WalletService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class LoanCancellationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mvc;

    @Test
    void cancelling_a_pending_loan_refunds_investors_from_escrow() {
        Long borrower = userService.createUser(new CreateUserRequest("B", "b.cancel@example.com")).id();
        Long investor = investorWithFunds("inv.cancel@example.com", "600.00");
        BigDecimal escrowStart = escrowBalance();
        Long loanId = loanService.requestLoan(new RequestLoanRequest(borrower,
                new BigDecimal("1000.00"), new BigDecimal("0.12"), 12)).id();
        loanService.fundLoan(loanId, new FundLoanRequest(investor, new BigDecimal("600.00")));
        assertThat(walletService.getBalance(investor).balance()).isEqualByComparingTo("0.00");

        loanService.cancelLoan(loanId);

        assertThat(loanService.getLoan(loanId).status()).isEqualTo(LoanStatus.CANCELLED);
        assertThat(walletService.getBalance(investor).balance()).isEqualByComparingTo("600.00");
        assertThat(escrowBalance()).isEqualByComparingTo(escrowStart);
    }

    @Test
    void cancelling_a_funded_loan_returns_409() throws Exception {
        Long borrower = userService.createUser(new CreateUserRequest("B", "b2.cancel@example.com")).id();
        Long investor = investorWithFunds("inv2.cancel@example.com", "1000.00");
        Long loanId = loanService.requestLoan(new RequestLoanRequest(borrower,
                new BigDecimal("1000.00"), new BigDecimal("0.12"), 12)).id();
        loanService.fundLoan(loanId, new FundLoanRequest(investor, new BigDecimal("1000.00")));

        mvc.perform(post("/api/loans/{id}/cancel", loanId))
                .andExpect(status().isConflict());
    }

    @Test
    void an_expired_pending_loan_is_cancelled_and_refunded() {
        Long borrower = userService.createUser(new CreateUserRequest("B", "b3.cancel@example.com")).id();
        Long investor = investorWithFunds("inv3.cancel@example.com", "600.00");
        BigDecimal escrowStart = escrowBalance();
        Long loanId = loanService.requestLoan(new RequestLoanRequest(borrower,
                new BigDecimal("1000.00"), new BigDecimal("0.12"), 12)).id();
        loanService.fundLoan(loanId, new FundLoanRequest(investor, new BigDecimal("600.00")));

        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setFundingDeadline(OffsetDateTime.now().minusDays(1));
        loanRepository.save(loan);

        loanService.expireLoan(loanId);

        assertThat(loanService.getLoan(loanId).status()).isEqualTo(LoanStatus.CANCELLED);
        assertThat(walletService.getBalance(investor).balance()).isEqualByComparingTo("600.00");
        assertThat(escrowBalance()).isEqualByComparingTo(escrowStart);
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
