package com.taf.p2plending.user.dto;

import java.math.BigDecimal;

public record UserResponse(
        Long id,
        String name,
        String email,
        Long walletId,
        BigDecimal balance) {
}
