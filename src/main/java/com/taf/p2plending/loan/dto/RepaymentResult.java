package com.taf.p2plending.loan.dto;

import com.taf.p2plending.loan.LoanStatus;
import java.math.BigDecimal;
import java.util.List;

public record RepaymentResult(
        Long loanId,
        int installmentNumber,
        BigDecimal amountPaid,
        BigDecimal interestPortion,
        BigDecimal principalPortion,
        BigDecimal platformFee,
        List<InvestorShareResponse> investorPayouts,
        LoanStatus status) {
}
