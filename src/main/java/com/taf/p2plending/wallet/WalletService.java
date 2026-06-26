package com.taf.p2plending.wallet;

import com.taf.p2plending.common.exception.NotFoundException;
import com.taf.p2plending.ledger.LedgerEntry;
import com.taf.p2plending.ledger.LedgerService;
import com.taf.p2plending.wallet.dto.WalletResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final WalletRepository wallets;
    private final LedgerService ledger;

    public WalletService(WalletRepository wallets, LedgerService ledger) {
        this.wallets = wallets;
        this.ledger = ledger;
    }

    @Transactional(readOnly = true)
    public WalletResponse getBalance(Long userId) {
        Wallet wallet = requireWallet(userId);
        return new WalletResponse(wallet.getId(), wallet.getUserId(), wallet.getBalance());
    }

    @Transactional
    public WalletResponse deposit(Long userId, BigDecimal amount) {
        Wallet wallet = requireWallet(userId);
        LedgerEntry entry = ledger.deposit(wallet.getId(), amount, UUID.randomUUID());
        return new WalletResponse(wallet.getId(), userId, entry.getBalanceAfter());
    }

    @Transactional
    public WalletResponse withdraw(Long userId, BigDecimal amount) {
        Wallet wallet = requireWallet(userId);
        LedgerEntry entry = ledger.withdraw(wallet.getId(), amount, UUID.randomUUID());
        return new WalletResponse(wallet.getId(), userId, entry.getBalanceAfter());
    }

    private Wallet requireWallet(Long userId) {
        return wallets.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("wallet for user " + userId));
    }
}
