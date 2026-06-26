package com.taf.p2plending.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taf.p2plending.common.exception.InsufficientFundsException;
import com.taf.p2plending.loan.dto.FundLoanRequest;
import com.taf.p2plending.loan.dto.RepaymentResult;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class RepaymentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private RepaymentService repaymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mvc;

    @Test
    void first_repayment_distributes_principal_interest_and_fee() {
        FundedLoan f = fundedLoan("r1");
        BigDecimal platformStart = platformBalance();

        RepaymentResult result = repaymentService.repay(f.loanId);

        assertThat(result.amountPaid()).isEqualByComparingTo("88.85");
        assertThat(result.interestPortion()).isEqualByComparingTo("10.00");
        assertThat(result.principalPortion()).isEqualByComparingTo("78.85");
        assertThat(result.platformFee()).isEqualByComparingTo("0.10");
        assertThat(result.installmentNumber()).isEqualTo(1);

        assertThat(walletService.getBalance(f.borrower).balance()).isEqualByComparingTo("911.15");
        assertThat(walletService.getBalance(f.investorA).balance()).isEqualByComparingTo("53.25");
        assertThat(walletService.getBalance(f.investorB).balance()).isEqualByComparingTo("35.50");
        assertThat(platformBalance()).isEqualByComparingTo(platformStart.add(new BigDecimal("0.10")));
        assertThat(loanService.getLoan(f.loanId).remainingPrincipal()).isEqualByComparingTo("921.15");
    }

    @Test
    void full_repayment_marks_loan_repaid_and_conserves_money() {
        FundedLoan f = fundedLoan("r2");
        walletService.deposit(f.borrower, new BigDecimal("200.00"));
        BigDecimal totalBefore = totalOf(f);

        RepaymentResult last = null;
        for (int month = 0; month < 12; month++) {
            last = repaymentService.repay(f.loanId);
        }

        assertThat(last.status()).isEqualTo(LoanStatus.REPAID);
        assertThat(loanService.getLoan(f.loanId).remainingPrincipal()).isEqualByComparingTo("0.00");
        assertThat(totalOf(f)).isEqualByComparingTo(totalBefore);
    }

    @Test
    void repayment_with_insufficient_funds_rolls_back_everything() {
        FundedLoan f = fundedLoan("r3");
        walletService.withdraw(f.borrower, new BigDecimal("1000.00"));
        BigDecimal investorABefore = walletService.getBalance(f.investorA).balance();

        assertThatThrownBy(() -> repaymentService.repay(f.loanId))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(walletService.getBalance(f.borrower).balance()).isEqualByComparingTo("0.00");
        assertThat(walletService.getBalance(f.investorA).balance()).isEqualByComparingTo(investorABefore);
        assertThat(loanService.getLoan(f.loanId).installmentsPaid()).isEqualTo(0);
    }

    @Test
    void repay_a_non_funded_loan_returns_409() throws Exception {
        Long borrower = userService.createUser(new CreateUserRequest("B", "borrower.r4@example.com")).id();
        Long loanId = loanService.requestLoan(new RequestLoanRequest(borrower,
                new BigDecimal("1000.00"), new BigDecimal("0.12"), 12)).id();

        mvc.perform(post("/api/loans/{id}/repay", loanId))
                .andExpect(status().isConflict());
    }

    private FundedLoan fundedLoan(String tag) {
        Long borrower = userService.createUser(new CreateUserRequest("B", "borrower." + tag + "@example.com")).id();
        Long investorA = investorWithFunds("a." + tag + "@example.com", "600.00");
        Long investorB = investorWithFunds("b." + tag + "@example.com", "400.00");
        Long loanId = loanService.requestLoan(new RequestLoanRequest(borrower,
                new BigDecimal("1000.00"), new BigDecimal("0.12"), 12)).id();
        loanService.fundLoan(loanId, new FundLoanRequest(investorA, new BigDecimal("600.00")));
        loanService.fundLoan(loanId, new FundLoanRequest(investorB, new BigDecimal("400.00")));
        return new FundedLoan(loanId, borrower, investorA, investorB);
    }

    private Long investorWithFunds(String email, String funds) {
        Long id = userService.createUser(new CreateUserRequest("Inv", email)).id();
        walletService.deposit(id, new BigDecimal(funds));
        return id;
    }

    private BigDecimal platformBalance() {
        Long platformUserId = userRepository.findBySystemRole(SystemRole.PLATFORM).orElseThrow().getId();
        return walletService.getBalance(platformUserId).balance();
    }

    private BigDecimal totalOf(FundedLoan f) {
        return walletService.getBalance(f.borrower).balance()
                .add(walletService.getBalance(f.investorA).balance())
                .add(walletService.getBalance(f.investorB).balance())
                .add(platformBalance());
    }

    private record FundedLoan(Long loanId, Long borrower, Long investorA, Long investorB) {
    }
}
