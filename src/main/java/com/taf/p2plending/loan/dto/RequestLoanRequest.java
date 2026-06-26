package com.taf.p2plending.loan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record RequestLoanRequest(
        @NotNull Long borrowerId,
        @NotNull @Positive BigDecimal principal,
        @NotNull @PositiveOrZero BigDecimal annualInterestRate,
        @NotNull @Positive Integer termMonths) {
}
