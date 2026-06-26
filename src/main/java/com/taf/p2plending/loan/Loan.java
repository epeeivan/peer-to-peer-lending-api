package com.taf.p2plending.loan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;

    @Column(nullable = false)
    private BigDecimal principal;

    @Column(name = "annual_interest_rate", nullable = false)
    private BigDecimal annualInterestRate;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.PENDING;

    @Column(name = "funded_amount", nullable = false)
    private BigDecimal fundedAmount = BigDecimal.ZERO;

    @Column(name = "funding_deadline", nullable = false)
    private OffsetDateTime fundingDeadline;

    @Column(name = "monthly_payment")
    private BigDecimal monthlyPayment;

    @Column(name = "remaining_principal")
    private BigDecimal remainingPrincipal;

    @Column(name = "installments_paid", nullable = false)
    private int installmentsPaid;

    @Version
    private Long version;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "funded_at")
    private OffsetDateTime fundedAt;

    protected Loan() {
    }

    public Loan(Long borrowerId, BigDecimal principal, BigDecimal annualInterestRate,
                int termMonths, OffsetDateTime fundingDeadline) {
        this.borrowerId = borrowerId;
        this.principal = principal;
        this.annualInterestRate = annualInterestRate;
        this.termMonths = termMonths;
        this.fundingDeadline = fundingDeadline;
    }

    public Long getId() {
        return id;
    }

    public Long getBorrowerId() {
        return borrowerId;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public BigDecimal getFundedAmount() {
        return fundedAmount;
    }

    public OffsetDateTime getFundingDeadline() {
        return fundingDeadline;
    }

    public void setFundingDeadline(OffsetDateTime fundingDeadline) {
        this.fundingDeadline = fundingDeadline;
    }

    public void setFundedAmount(BigDecimal fundedAmount) {
        this.fundedAmount = fundedAmount;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(BigDecimal monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public BigDecimal getRemainingPrincipal() {
        return remainingPrincipal;
    }

    public void setRemainingPrincipal(BigDecimal remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    public int getInstallmentsPaid() {
        return installmentsPaid;
    }

    public void setInstallmentsPaid(int installmentsPaid) {
        this.installmentsPaid = installmentsPaid;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getFundedAt() {
        return fundedAt;
    }

    public void setFundedAt(OffsetDateTime fundedAt) {
        this.fundedAt = fundedAt;
    }
}
