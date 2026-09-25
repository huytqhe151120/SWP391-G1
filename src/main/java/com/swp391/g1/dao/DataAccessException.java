package com.swp391.g1.dao;

/**
 * Wraps SQL / connectivity failures raised by the DAO layer so the service
 * layer does not leak java.sql details to the presentation layer.
 *
 * <p>duplicateKey is true when the underlying SQL exception is a
 * unique-constraint violation (e.g. duplicate username).
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final boolean duplicateKey;

    public DataAccessException(String message) {
        super(message);
        this.duplicateKey = false;
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
        this.duplicateKey = false;
    }

    public DataAccessException(String message, Throwable cause, boolean duplicateKey) {
        super(message, cause);
        this.duplicateKey = duplicateKey;
    }

    public boolean isDuplicateKey() {
        return duplicateKey;
    }
}