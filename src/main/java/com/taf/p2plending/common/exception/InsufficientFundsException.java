package com.taf.p2plending.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Long walletId) {
        super("Insufficient funds in wallet " + walletId);
    }
}
