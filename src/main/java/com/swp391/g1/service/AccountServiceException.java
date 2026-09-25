package com.swp391.g1.service;

/**
 * Raised for unexpected runtime failures (database errors, invalid state)
 * that must be reported to the user with a safe message. Technical details
 * are logged at the DAO layer and are never exposed to the user.
 */
public class AccountServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccountServiceException(String message) {
        super(message);
    }

    public AccountServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}