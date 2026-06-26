package com.taf.p2plending.loan;

import com.taf.p2plending.common.exception.IllegalLoanStateException;
import com.taf.p2plending.common.exception.NotFoundException;
import com.taf.p2plending.common.exception.OverFundingException;
import com.taf.p2plending.config.FundingProperties;
import com.taf.p2plending.finance.Money;
import com.taf.p2plending.finance.MoneyMath;
import com.taf.p2plending.ledger.LedgerEntryType;
import com.taf.p2plending.ledger.LedgerService;
import com.taf.p2plending.loan.dto.FundLoanRequest;
import com.taf.p2plending.loan.dto.LoanResponse;
import com.taf.p2plending.loan.dto.RequestLoanRequest;
import com.taf.p2plending.user.SystemRole;
import com.taf.p2plending.user.User;
import com.taf.p2plending.user.UserRepository;
import com.taf.p2plending.wallet.Wallet;
import com.taf.p2plending.wallet.WalletRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private final LoanRepository loans;
    private final UserRepository users;
    private final WalletRepository wallets;
    private final LoanInvestmentRepository investments;
    private final LedgerService ledger;
    private final FundingProperties fundingProperties;

    public LoanService(LoanRepository loans, UserRepository users, WalletRepository wallets,
                       LoanInvestmentRepository investments, LedgerService ledger,
                       FundingProperties fundingProperties) {
        this.loans = loans;
        this.users = users;
        this.wallets = wallets;
        this.investments = investments;
        this.ledger = ledger;
        this.fundingProperties = fundingProperties;
    }

    @Transactional
    public LoanResponse requestLoan(RequestLoanRequest request) {
        if (!users.existsById(request.borrowerId())) {
            throw new NotFoundException("user " + request.borrowerId());
        }
        OffsetDateTime deadline = OffsetDateTime.now().plusDays(fundingProperties.windowDays());
        Loan loan = loans.save(new Loan(request.borrowerId(), request.principal(),
                request.annualInterestRate(), request.termMonths(), deadline));
        return LoanResponse.from(loan);
    }

    @Transactional
    public LoanResponse fundLoan(Long loanId, FundLoanRequest request) {
        Loan loan = loans.findByIdForUpdate(loanId)
                .orElseThrow(() -> new NotFoundException("loan " + loanId));
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalLoanStateException("loan " + loanId + " is not PENDING");
        }
        if (!loan.getFundingDeadline().isAfter(OffsetDateTime.now())) {
            throw new IllegalLoanStateException("funding window for loan " + loanId + " has expired");
        }
        if (request.investorId().equals(loan.getBorrowerId())) {
            throw new IllegalLoanStateException("a borrower cannot fund their own loan");
        }
        BigDecimal amount = Money.scale2(request.amount());
        BigDecimal remaining = loan.getPrincipal().subtract(loan.getFundedAmount());
        if (amount.compareTo(remaining) > 0) {
            throw new OverFundingException(loanId);
        }

        Wallet investorWallet = wallets.findByUserId(request.investorId())
                .orElseThrow(() -> new NotFoundException("wallet for user " + request.investorId()));
        Wallet escrow = escrowWallet();
        boolean willDisburse = Money.scale2(loan.getFundedAmount().add(amount))
                .compareTo(loan.getPrincipal()) == 0;
        Wallet borrowerWallet = willDisburse
                ? wallets.findByUserId(loan.getBorrowerId())
                        .orElseThrow(() -> new NotFoundException("wallet for user " + loan.getBorrowerId()))
                : null;

        List<Long> walletsToLock = new ArrayList<>(List.of(investorWallet.getId(), escrow.getId()));
        if (borrowerWallet != null) {
            walletsToLock.add(borrowerWallet.getId());
        }
        ledger.lockWallets(walletsToLock);

        UUID correlationId = UUID.randomUUID();
        ledger.transfer(investorWallet.getId(), escrow.getId(), amount,
                LedgerEntryType.LOAN_FUNDING, loanId, correlationId);
        investments.save(new LoanInvestment(loanId, request.investorId(), amount));
        loan.setFundedAmount(Money.scale2(loan.getFundedAmount().add(amount)));

        if (borrowerWallet != null) {
            disburse(loan, escrow, borrowerWallet, correlationId);
        }
        return LoanResponse.from(loan);
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoan(Long id) {
        return LoanResponse.from(requireLoan(id));
    }

    @Transactional
    public LoanResponse cancelLoan(Long loanId) {
        Loan loan = loans.findByIdForUpdate(loanId)
                .orElseThrow(() -> new NotFoundException("loan " + loanId));
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalLoanStateException("loan " + loanId + " is not PENDING");
        }
        refundAndCancel(loan);
        return LoanResponse.from(loan);
    }

    @Transactional
    public void expireLoan(Long loanId) {
        Loan loan = loans.findByIdForUpdate(loanId)
                .orElseThrow(() -> new NotFoundException("loan " + loanId));
        if (loan.getStatus() != LoanStatus.PENDING
                || loan.getFundingDeadline().isAfter(OffsetDateTime.now())) {
            return;
        }
        refundAndCancel(loan);
    }

    private void refundAndCancel(Loan loan) {
        Map<Long, BigDecimal> byInvestor = new LinkedHashMap<>();
        for (LoanInvestment investment : investments.findByLoanId(loan.getId())) {
            byInvestor.merge(investment.getInvestorId(), investment.getAmount(), BigDecimal::add);
        }

        Wallet escrow = escrowWallet();
        Map<Long, Wallet> investorWallets = new LinkedHashMap<>();
        List<Long> walletsToLock = new ArrayList<>();
        walletsToLock.add(escrow.getId());
        for (Long investorId : byInvestor.keySet()) {
            Wallet wallet = wallets.findByUserId(investorId)
                    .orElseThrow(() -> new NotFoundException("wallet for user " + investorId));
            investorWallets.put(investorId, wallet);
            walletsToLock.add(wallet.getId());
        }
        ledger.lockWallets(walletsToLock);

        UUID correlationId = UUID.randomUUID();
        for (Map.Entry<Long, BigDecimal> entry : byInvestor.entrySet()) {
            ledger.transfer(escrow.getId(), investorWallets.get(entry.getKey()).getId(),
                    entry.getValue(), LedgerEntryType.FUNDING_REFUND, loan.getId(), correlationId);
        }
        loan.setStatus(LoanStatus.CANCELLED);
    }

    private void disburse(Loan loan, Wallet escrow, Wallet borrowerWallet, UUID correlationId) {
        loan.setMonthlyPayment(MoneyMath.monthlyPayment(loan.getPrincipal(),
                loan.getAnnualInterestRate(), loan.getTermMonths()));
        loan.setRemainingPrincipal(loan.getPrincipal());
        ledger.transfer(escrow.getId(), borrowerWallet.getId(), loan.getPrincipal(),
                LedgerEntryType.LOAN_DISBURSEMENT, loan.getId(), correlationId);
        loan.setStatus(LoanStatus.FUNDED);
        loan.setFundedAt(OffsetDateTime.now());
    }

    private Wallet escrowWallet() {
        User escrow = users.findBySystemRole(SystemRole.ESCROW)
                .orElseThrow(() -> new NotFoundException("escrow account"));
        return wallets.findByUserId(escrow.getId())
                .orElseThrow(() -> new NotFoundException("escrow wallet"));
    }

    private Loan requireLoan(Long id) {
        return loans.findById(id).orElseThrow(() -> new NotFoundException("loan " + id));
    }
}
