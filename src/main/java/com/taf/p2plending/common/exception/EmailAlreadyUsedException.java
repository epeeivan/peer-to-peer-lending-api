package com.taf.p2plending.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Email already used: " + email);
    }
}
