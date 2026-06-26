package com.taf.p2plending.loan;

import com.taf.p2plending.common.exception.NotFoundException;
import com.taf.p2plending.config.FundingProperties;
import com.taf.p2plending.loan.dto.LoanResponse;
import com.taf.p2plending.loan.dto.RequestLoanRequest;
import com.taf.p2plending.user.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private final LoanRepository loans;
    private final UserRepository users;
    private final FundingProperties fundingProperties;

    public LoanService(LoanRepository loans, UserRepository users, FundingProperties fundingProperties) {
        this.loans = loans;
        this.users = users;
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

    @Transactional(readOnly = true)
    public LoanResponse getLoan(Long id) {
        return LoanResponse.from(requireLoan(id));
    }

    private Loan requireLoan(Long id) {
        return loans.findById(id).orElseThrow(() -> new NotFoundException("loan " + id));
    }
}
