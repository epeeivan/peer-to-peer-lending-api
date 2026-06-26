package com.taf.p2plending.loan.dto;

import java.math.BigDecimal;

public record InvestorShareResponse(
        Long investorId,
        BigDecimal amount) {
}
