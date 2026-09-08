package com.schoolms.common.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AuthException(HttpStatus status, String code) {
        super(code);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static AuthException badCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "auth.bad_credentials");
    }

    public static AuthException inactive() {
        return new AuthException(HttpStatus.FORBIDDEN, "auth.user_inactive");
    }

    public static AuthException accessDenied() {
        return new AuthException(HttpStatus.FORBIDDEN, "auth.access_denied");
    }
}
