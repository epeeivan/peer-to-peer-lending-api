package com.taf.p2plending.loan;

import static org.assertj.core.api.Assertions.assertThat;

import com.taf.p2plending.loan.dto.FundLoanRequest;
import com.taf.p2plending.loan.dto.RequestLoanRequest;
import com.taf.p2plending.support.AbstractIntegrationTest;
import com.taf.p2plending.user.UserService;
import com.taf.p2plending.user.dto.CreateUserRequest;
import com.taf.p2plending.wallet.WalletService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LoanFundingConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private LoanService loanService;

    @Test
    void concurrent_full_funding_never_over_funds() throws Exception {
        int investors = 8;
        Long borrower = userService.createUser(new CreateUserRequest("BC", "bc.conc@example.com")).id();
        Long loanId = loanService.requestLoan(new RequestLoanRequest(borrower,
                new BigDecimal("100.00"), new BigDecimal("0.12"), 12)).id();

        List<Long> investorIds = new ArrayList<>();
        for (int k = 0; k < investors; k++) {
            Long id = userService.createUser(new CreateUserRequest("I" + k, "i" + k + ".conc@example.com")).id();
            walletService.deposit(id, new BigDecimal("100.00"));
            investorIds.add(id);
        }

        ExecutorService pool = Executors.newFixedThreadPool(investors);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (Long investorId : investorIds) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    loanService.fundLoan(loanId, new FundLoanRequest(investorId, new BigDecimal("100.00")));
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            }));
        }
        start.countDown();

        int successes = 0;
        for (Future<Boolean> future : futures) {
            if (Boolean.TRUE.equals(future.get())) {
                successes++;
            }
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(loanService.getLoan(loanId).fundedAmount()).isEqualByComparingTo("100.00");
        assertThat(loanService.getLoan(loanId).status()).isEqualTo(LoanStatus.FUNDED);
    }
}
