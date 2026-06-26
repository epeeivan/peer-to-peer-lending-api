package com.taf.p2plending.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OverFundingException extends RuntimeException {

    public OverFundingException(Long loanId) {
        super("Funding would exceed the requested principal for loan " + loanId);
    }
}
