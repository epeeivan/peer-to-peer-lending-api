package com.taf.p2plending.loan;

import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LoanExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(LoanExpiryScheduler.class);

    private final LoanRepository loans;
    private final LoanService loanService;

    public LoanExpiryScheduler(LoanRepository loans, LoanService loanService) {
        this.loans = loans;
        this.loanService = loanService;
    }

    @Scheduled(cron = "${p2p.funding.expiry-check-cron}")
    public void expireOverdueLoans() {
        for (Long loanId : loans.findExpiredPendingIds(OffsetDateTime.now())) {
            try {
                loanService.expireLoan(loanId);
            } catch (RuntimeException ex) {
                log.warn("Failed to expire loan {}: {}", loanId, ex.getMessage());
            }
        }
    }
}
