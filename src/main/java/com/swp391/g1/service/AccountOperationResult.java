package com.swp391.g1.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of an account create/update/status operation.
 *
 * <p>success=true means the operation was persisted. fieldErrors maps a form
 * field name to a user-safe validation message. For status changes, message
 * carries a redirect code: "notFound" or "invalidStatus".
 */
public class AccountOperationResult {

    private boolean success;
    private final Map<String, String> fieldErrors = new LinkedHashMap<>();
    private String message;
    private Integer accountId;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void addFieldError(String field, String message) {
        fieldErrors.put(field, message);
    }

    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }
}