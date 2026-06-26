package com.taf.p2plending.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "loan_id")
    private Long loanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LedgerEntryType type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(Long walletId, Long loanId, LedgerEntryType type,
                       BigDecimal amount, BigDecimal balanceAfter, UUID correlationId) {
        this.walletId = walletId;
        this.loanId = loanId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.correlationId = correlationId;
    }

    public Long getId() {
        return id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public LedgerEntryType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
