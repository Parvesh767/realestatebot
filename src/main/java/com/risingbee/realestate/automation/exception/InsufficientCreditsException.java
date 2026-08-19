package com.risingbee.realestate.automation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class InsufficientCreditsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	public InsufficientCreditsException(String message) {
        super(message);
    }
}