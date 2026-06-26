package com.taf.p2plending.wallet.dto;

import java.math.BigDecimal;

public record WalletResponse(
        Long walletId,
        Long userId,
        BigDecimal balance) {
}
