package com.taf.p2plending.loan;

import com.taf.p2plending.common.exception.IllegalLoanStateException;
import com.taf.p2plending.common.exception.NotFoundException;
import com.taf.p2plending.finance.Money;
import com.taf.p2plending.finance.MoneyMath;
import com.taf.p2plending.ledger.LedgerEntryType;
import com.taf.p2plending.ledger.LedgerService;
import com.taf.p2plending.loan.dto.InvestorShareResponse;
import com.taf.p2plending.loan.dto.RepaymentResult;
import com.taf.p2plending.user.SystemRole;
import com.taf.p2plending.user.UserRepository;
import com.taf.p2plending.wallet.WalletRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepaymentService {

    private final LoanRepository loans;
    private final LoanInvestmentRepository investments;
    private final WalletRepository wallets;
    private final UserRepository users;
    private final LedgerService ledger;

    public RepaymentService(LoanRepository loans, LoanInvestmentRepository investments,
                            WalletRepository wallets, UserRepository users, LedgerService ledger) {
        this.loans = loans;
        this.investments = investments;
        this.wallets = wallets;
        this.users = users;
        this.ledger = ledger;
    }

    @Transactional
    public RepaymentResult repay(Long loanId) {
        Loan loan = loans.findByIdForUpdate(loanId)
                .orElseThrow(() -> new NotFoundException("loan " + loanId));
        if (loan.getStatus() != LoanStatus.FUNDED) {
            throw new IllegalLoanStateException("loan " + loanId + " is not FUNDED");
        }

        boolean lastInstallment = loan.getInstallmentsPaid() + 1 == loan.getTermMonths();
        BigDecimal interest = MoneyMath.interestPortion(loan.getRemainingPrincipal(), loan.getAnnualInterestRate());
        BigDecimal principalPortion = lastInstallment
                ? loan.getRemainingPrincipal()
                : Money.scale2(loan.getMonthlyPayment().subtract(interest));
        BigDecimal payment = Money.scale2(principalPortion.add(interest));
        BigDecimal fee = MoneyMath.platformFee(interest);
        BigDecimal distributable = Money.scale2(principalPortion.add(interest.subtract(fee)));

        Map<Long, BigDecimal> byInvestor = new LinkedHashMap<>();
        for (LoanInvestment investment : investments.findByLoanId(loanId)) {
            byInvestor.merge(investment.getInvestorId(), investment.getAmount(), BigDecimal::add);
        }
        List<Long> investorIds = new ArrayList<>(byInvestor.keySet());
        List<BigDecimal> shares = MoneyMath.distribute(distributable,
                new ArrayList<>(byInvestor.values()), loan.getPrincipal());

        UUID correlationId = UUID.randomUUID();
        List<Leg> legs = new ArrayList<>();
        legs.add(new Leg(walletId(loan.getBorrowerId()), payment.negate(), LedgerEntryType.REPAYMENT_DEBIT));
        if (fee.signum() != 0) {
            legs.add(new Leg(platformWalletId(), fee, LedgerEntryType.PLATFORM_FEE));
        }
        List<InvestorShareResponse> payouts = new ArrayList<>();
        for (int k = 0; k < investorIds.size(); k++) {
            BigDecimal share = shares.get(k);
            payouts.add(new InvestorShareResponse(investorIds.get(k), share));
            if (share.signum() != 0) {
                legs.add(new Leg(walletId(investorIds.get(k)), share, LedgerEntryType.REPAYMENT_CREDIT));
            }
        }

        legs.sort(Comparator.comparing(Leg::walletId));
        for (Leg leg : legs) {
            ledger.post(leg.walletId(), leg.amount(), leg.type(), loanId, correlationId);
        }

        loan.setRemainingPrincipal(Money.scale2(loan.getRemainingPrincipal().subtract(principalPortion)));
        loan.setInstallmentsPaid(loan.getInstallmentsPaid() + 1);
        if (loan.getInstallmentsPaid() == loan.getTermMonths()) {
            loan.setStatus(LoanStatus.REPAID);
        }

        return new RepaymentResult(loanId, loan.getInstallmentsPaid(), payment, interest,
                principalPortion, fee, payouts, loan.getStatus());
    }

    private Long walletId(Long userId) {
        return wallets.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("wallet for user " + userId)).getId();
    }

    private Long platformWalletId() {
        Long platformUserId = users.findBySystemRole(SystemRole.PLATFORM)
                .orElseThrow(() -> new NotFoundException("platform account")).getId();
        return walletId(platformUserId);
    }

    private record Leg(Long walletId, BigDecimal amount, LedgerEntryType type) {
    }
}
