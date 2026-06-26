package com.taf.p2plending.loan.dto;

import com.taf.p2plending.loan.Loan;
import com.taf.p2plending.loan.LoanStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LoanResponse(
        Long id,
        Long borrowerId,
        BigDecimal principal,
        BigDecimal annualInterestRate,
        int termMonths,
        LoanStatus status,
        BigDecimal fundedAmount,
        OffsetDateTime fundingDeadline,
        BigDecimal monthlyPayment,
        BigDecimal remainingPrincipal,
        int installmentsPaid) {

    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getBorrowerId(),
                loan.getPrincipal(),
                loan.getAnnualInterestRate(),
                loan.getTermMonths(),
                loan.getStatus(),
                loan.getFundedAmount(),
                loan.getFundingDeadline(),
                loan.getMonthlyPayment(),
                loan.getRemainingPrincipal(),
                loan.getInstallmentsPaid());
    }
}
