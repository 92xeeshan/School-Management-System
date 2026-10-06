package com.schoolms.common.exception;

public class RateLimitException extends RuntimeException {

    public RateLimitException() {
        super("error.rate_limited");
    }
}
