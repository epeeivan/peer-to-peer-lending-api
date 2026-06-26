package com.taf.p2plending.loan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "loan_investments")
public class LoanInvestment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LoanInvestment() {
    }

    public LoanInvestment(Long loanId, Long investorId, BigDecimal amount) {
        this.loanId = loanId;
        this.investorId = investorId;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public Long getLoanId() {
        return loanId;
    }

    public Long getInvestorId() {
        return investorId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
