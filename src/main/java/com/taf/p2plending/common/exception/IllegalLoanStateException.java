package com.taf.p2plending.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class IllegalLoanStateException extends RuntimeException {

    public IllegalLoanStateException(String message) {
        super(message);
    }
}
