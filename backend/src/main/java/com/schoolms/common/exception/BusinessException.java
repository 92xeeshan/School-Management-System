package com.schoolms.common.exception;

/**
 * A business/validation failure that should be surfaced to the client with a
 * localized message. The {@code code} is the i18n key in messages_*.properties.
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final Object[] args;

    public BusinessException(String code, Object... args) {
        super(code);
        this.code = code;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }
}
