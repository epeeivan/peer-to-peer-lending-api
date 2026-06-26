package com.taf.p2plending.loan;

import com.taf.p2plending.loan.dto.FundLoanRequest;
import com.taf.p2plending.loan.dto.LoanResponse;
import com.taf.p2plending.loan.dto.RepaymentResult;
import com.taf.p2plending.loan.dto.RequestLoanRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;
    private final RepaymentService repaymentService;

    public LoanController(LoanService loanService, RepaymentService repaymentService) {
        this.loanService = loanService;
        this.repaymentService = repaymentService;
    }

    @PostMapping
    public ResponseEntity<LoanResponse> request(@Valid @RequestBody RequestLoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.requestLoan(request));
    }

    @PostMapping("/{id}/fund")
    public LoanResponse fund(@PathVariable Long id, @Valid @RequestBody FundLoanRequest request) {
        return loanService.fundLoan(id, request);
    }

    @PostMapping("/{id}/repay")
    public RepaymentResult repay(@PathVariable Long id) {
        return repaymentService.repay(id);
    }

    @PostMapping("/{id}/cancel")
    public LoanResponse cancel(@PathVariable Long id) {
        return loanService.cancelLoan(id);
    }

    @GetMapping("/{id}")
    public LoanResponse get(@PathVariable Long id) {
        return loanService.getLoan(id);
    }
}
