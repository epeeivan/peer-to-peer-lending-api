package com.taf.p2plending.loan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record FundLoanRequest(
        @NotNull Long investorId,
        @NotNull @Positive BigDecimal amount) {
}
