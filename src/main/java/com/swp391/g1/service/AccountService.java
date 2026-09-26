package com.swp391.g1.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.swp391.g1.dao.AccountDAO;
import com.swp391.g1.dao.DataAccessException;
import com.swp391.g1.model.Account;
import com.swp391.g1.model.AccountProfile;
import com.swp391.g1.model.AccountTypeRole;

/**
 * Business rules for accounts: validation, type/role compatibility and the
 * status whitelist. DAO failures are translated into AccountServiceException
 * with a user-safe message.
 */
public class AccountService {

    /** Database-supported account status values (CHECK CK_account_status). */
    public static final List<String> ACCOUNT_STATUSES = List.of("ACTIVE", "INACTIVE", "BLOCKED");

    private static final int USERNAME_MAX_LENGTH = 100;
    private static final int PASSWORD_MAX_LENGTH = 255;
    private static final int TYPE_MAX_LENGTH = 50;
    private static final int ROLE_MAX_LENGTH = 50;
    private static final int STATUS_MAX_LENGTH = 20;

    private final AccountDAO accountDAO = new AccountDAO();

    public List<Account> searchAccounts(String search, String type, String role, String status) {
        return accountDAO.findByCriteria(search, type, role, status);
    }

    /** Loads the account without its password; null when it does not exist. */
    public Account getAccountById(int id) {
        return accountDAO.findById(id);
    }

    public List<AccountProfile> findProfiles(int accountId) {
        return accountDAO.findProfilesByAccountId(accountId);
    }

    public List<String> getAccountTypes() {
        return accountDAO.listAccountTypes();
    }

    public List<String> getAccountRoles() {
        return accountDAO.listAccountRoles();
    }

    public List<AccountTypeRole> getAccountTypeRoles() {
        return accountDAO.listAccountTypeRoles();
    }

    /** Maps every allowed type to the list of roles valid for it. */
    public Map<String, List<String>> getRolesByType() {
        Map<String, List<String>> rolesByType = new LinkedHashMap<>();
        for (AccountTypeRole combo : accountDAO.listAccountTypeRoles()) {
            rolesByType.computeIfAbsent(combo.getAccountType(), k -> new ArrayList<>()).add(combo.getAccountRole());
        }
        return rolesByType;
    }

    /**
     * Validates and persists a new account. On success accountId holds the
     * generated id; otherwise fieldErrors contains user-safe messages.
     */
    public AccountOperationResult createAccount(Account account) {
        AccountOperationResult result = new AccountOperationResult();
        try {
            validateCreate(account, result);
        } catch (DataAccessException e) {
            throw new AccountServiceException("Database error. Please try again later.", e);
        }
        if (result.hasFieldErrors()) {
            return result;
        }
        try {
            account.setPassword(preparePasswordForStorage(account.getPassword()));
            int newId = accountDAO.create(account);
            result.setSuccess(true);
            result.setAccountId(newId);
        } catch (DataAccessException e) {
            if (e.isDuplicateKey()) {
                result.addFieldError("username", "Username already exists.");
                return result;
            }
            throw new AccountServiceException("Database error. Please try again later.", e);
        }
        return result;
    }

    /** A blank password keeps the stored one; a non-blank password replaces it. */
    public AccountOperationResult updateAccount(Account account) {
        AccountOperationResult result = new AccountOperationResult();
        try {
            validateUpdate(account, result);
        } catch (DataAccessException e) {
            throw new AccountServiceException("Database error. Please try again later.", e);
        }
        if (result.hasFieldErrors()) {
            return result;
        }
        try {
            boolean newPasswordProvided = account.getPassword() != null && !account.getPassword().isEmpty();
            if (newPasswordProvided) {
                account.setPassword(preparePasswordForStorage(account.getPassword()));
            } else {
                String existingPassword = accountDAO.findPasswordById(account.getId());
                account.setPassword(existingPassword == null ? "" : existingPassword);
            }
            if (!accountDAO.update(account)) {
                result.addFieldError("id", "Account not found.");
                return result;
            }
            result.setSuccess(true);
            result.setAccountId(account.getId());
        } catch (DataAccessException e) {
            if (e.isDuplicateKey()) {
                result.addFieldError("username", "Username already exists.");
                return result;
            }
            throw new AccountServiceException("Database error. Please try again later.", e);
        }
        return result;
    }

    /**
     * Updates only the status. On failure message holds a redirect code:
     * "notFound" or "invalidStatus".
     */
    public AccountOperationResult changeStatus(int id, String status) {
        AccountOperationResult result = new AccountOperationResult();
        try {
            if (accountDAO.findById(id) == null) {
                result.setMessage("notFound");
                return result;
            }
            String normalizedStatus = normalize(status);
            if (!ACCOUNT_STATUSES.contains(normalizedStatus)) {
                result.setMessage("invalidStatus");
                return result;
            }
            if (!accountDAO.updateStatus(id, normalizedStatus)) {
                result.setMessage("notFound");
                return result;
            }
            result.setSuccess(true);
            result.setAccountId(id);
        } catch (DataAccessException e) {
            throw new AccountServiceException("Database error. Please try again later.", e);
        }
        return result;
    }

    // Validation

    private void validateCreate(Account account, AccountOperationResult result) {
        normalizeRequestFields(account);

        String username = account.getUsername();
        String password = account.getPassword();
        String type = account.getType();
        String role = account.getRole();
        String status = account.getStatus();

        if (isBlank(username)) {
            result.addFieldError("username", "Username is required.");
        } else if (username.length() > USERNAME_MAX_LENGTH) {
            result.addFieldError("username", "Username must be at most " + USERNAME_MAX_LENGTH + " characters.");
        }
        if (isBlank(password)) {
            result.addFieldError("password", "Password is required.");
        } else if (password.length() > PASSWORD_MAX_LENGTH) {
            result.addFieldError("password", "Password must be at most " + PASSWORD_MAX_LENGTH + " characters.");
        }
        if (isBlank(type)) {
            result.addFieldError("type", "Type is required.");
        } else if (type.length() > TYPE_MAX_LENGTH) {
            result.addFieldError("type", "Type is invalid.");
        }
        if (isBlank(role)) {
            result.addFieldError("role", "Role is required.");
        } else if (role.length() > ROLE_MAX_LENGTH) {
            result.addFieldError("role", "Role is invalid.");
        }
        if (isBlank(status) || !ACCOUNT_STATUSES.contains(status)) {
            result.addFieldError("status", "Invalid status. Allowed values: ACTIVE, INACTIVE, BLOCKED.");
        }

        validateDomainReferences(username, type, role, -1, result);
    }

    private void validateUpdate(Account account, AccountOperationResult result) {
        if (account == null || account.getId() <= 0) {
            result.addFieldError("id", "Account not found.");
            return;
        }
        if (accountDAO.findById(account.getId()) == null) {
            result.addFieldError("id", "Account not found.");
            return;
        }
        normalizeRequestFields(account);

        String username = account.getUsername();
        String password = account.getPassword();
        String type = account.getType();
        String role = account.getRole();
        String status = account.getStatus();

        if (isBlank(username)) {
            result.addFieldError("username", "Username is required.");
        } else if (username.length() > USERNAME_MAX_LENGTH) {
            result.addFieldError("username", "Username must be at most " + USERNAME_MAX_LENGTH + " characters.");
        }
        if (password != null && password.length() > PASSWORD_MAX_LENGTH) {
            result.addFieldError("password", "Password must be at most " + PASSWORD_MAX_LENGTH + " characters.");
        }
        if (isBlank(type)) {
            result.addFieldError("type", "Type is required.");
        } else if (type.length() > TYPE_MAX_LENGTH) {
            result.addFieldError("type", "Type is invalid.");
        }
        if (isBlank(role)) {
            result.addFieldError("role", "Role is required.");
        } else if (role.length() > ROLE_MAX_LENGTH) {
            result.addFieldError("role", "Role is invalid.");
        }
        if (isBlank(status) || !ACCOUNT_STATUSES.contains(status)) {
            result.addFieldError("status", "Invalid status. Allowed values: ACTIVE, INACTIVE, BLOCKED.");
        }

        validateDomainReferences(username, type, role, account.getId(), result);
    }

    /** excludeId > 0 means the account keeps its own username (update case). */
    private void validateDomainReferences(String username, String type, String role, int excludeId,
            AccountOperationResult result) {
        if (!isBlank(username)
                && (excludeId > 0
                        ? accountDAO.usernameExistsExcluding(username, excludeId)
                        : accountDAO.usernameExists(username))) {
            result.addFieldError("username", "Username already exists.");
        }
        if (!isBlank(type) && type.length() <= TYPE_MAX_LENGTH && !accountDAO.accountTypeExists(type)) {
            result.addFieldError("type", "Invalid type.");
        }
        if (!isBlank(role) && role.length() <= ROLE_MAX_LENGTH && !accountDAO.accountRoleExists(role)) {
            result.addFieldError("role", "Invalid role.");
        }
        if (!isBlank(type) && !isBlank(role)
                && type.length() <= TYPE_MAX_LENGTH && role.length() <= ROLE_MAX_LENGTH
                && !accountDAO.accountTypeRoleExists(type, role)) {
            result.addFieldError("role", "Invalid type/role combination.");
        }
    }

    /** Trims username/type/role/status (never the password). */
    private static void normalizeRequestFields(Account account) {
        account.setUsername(normalize(account.getUsername()));
        account.setType(normalize(account.getType()));
        account.setRole(normalize(account.getRole()));
        account.setStatus(normalize(account.getStatus()));
    }

    /**
     * Password storage seam. The MVP persists the password as provided; hashing
     * belongs to the future Authentication feature.
     */
    private static String preparePasswordForStorage(String rawPassword) {
        return rawPassword;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}