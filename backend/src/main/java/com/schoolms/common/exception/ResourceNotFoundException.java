package com.schoolms.common.exception;

import java.util.UUID;

/**
 * Thrown when a requested resource does not exist. Renders as HTTP 404 with a
 * localized message.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String entity, Object id) {
        super("resource.not_found:" + entity + ":" + id);
    }

    public ResourceNotFoundException(String entity, String field, Object value) {
        super("resource.not_found:" + entity + ":" + field + ":" + value);
    }

    public static ResourceNotFoundException of(String entity, UUID id) {
        return new ResourceNotFoundException(entity, id);
    }
}
