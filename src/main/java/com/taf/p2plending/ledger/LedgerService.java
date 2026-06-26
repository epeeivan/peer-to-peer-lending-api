package com.taf.p2plending.ledger;

import com.taf.p2plending.common.exception.InsufficientFundsException;
import com.taf.p2plending.common.exception.NotFoundException;
import com.taf.p2plending.finance.Money;
import com.taf.p2plending.wallet.Wallet;
import com.taf.p2plending.wallet.WalletRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

    private final WalletRepository wallets;
    private final LedgerRepository ledger;

    public LedgerService(WalletRepository wallets, LedgerRepository ledger) {
        this.wallets = wallets;
        this.ledger = ledger;
    }

    @Transactional
    public LedgerEntry deposit(Long walletId, BigDecimal amount, UUID correlationId) {
        return post(walletId, amount, LedgerEntryType.DEPOSIT, null, correlationId);
    }

    @Transactional
    public LedgerEntry withdraw(Long walletId, BigDecimal amount, UUID correlationId) {
        return post(walletId, amount.negate(), LedgerEntryType.WITHDRAWAL, null, correlationId);
    }

    @Transactional
    public void transfer(Long fromWalletId, Long toWalletId, BigDecimal amount,
                         LedgerEntryType type, Long loanId, UUID correlationId) {
        if (fromWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }
        if (fromWalletId.compareTo(toWalletId) < 0) {
            post(fromWalletId, amount.negate(), type, loanId, correlationId);
            post(toWalletId, amount, type, loanId, correlationId);
        } else {
            post(toWalletId, amount, type, loanId, correlationId);
            post(fromWalletId, amount.negate(), type, loanId, correlationId);
        }
    }

    @Transactional
    public void lockWallets(Collection<Long> walletIds) {
        walletIds.stream().distinct().sorted().forEach(id ->
                wallets.findByIdForUpdate(id)
                        .orElseThrow(() -> new NotFoundException("wallet " + id)));
    }

    @Transactional
    public LedgerEntry post(Long walletId, BigDecimal signedAmount, LedgerEntryType type,
                            Long loanId, UUID correlationId) {
        BigDecimal delta = Money.scale2(signedAmount);
        if (delta.signum() == 0) {
            throw new IllegalArgumentException("Ledger movement amount cannot be zero");
        }
        Wallet wallet = wallets.findByIdForUpdate(walletId)
                .orElseThrow(() -> new NotFoundException("wallet " + walletId));
        BigDecimal newBalance = Money.scale2(wallet.getBalance().add(delta));
        if (newBalance.signum() < 0) {
            throw new InsufficientFundsException(walletId);
        }
        wallet.setBalance(newBalance);
        return ledger.save(new LedgerEntry(walletId, loanId, type, delta, newBalance, correlationId));
    }
}
