package com.taf.p2plending.loan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanInvestmentRepository extends JpaRepository<LoanInvestment, Long> {

    List<LoanInvestment> findByLoanId(Long loanId);
}
