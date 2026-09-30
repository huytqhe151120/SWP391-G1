package com.swp391.g1.service;

import java.time.Instant;

import com.swp391.g1.dao.AccountDAO;
import com.swp391.g1.dao.DataAccessException;
import com.swp391.g1.dto.CurrentUser;
import com.swp391.g1.model.Account;
import com.swp391.g1.util.PasswordUtil;

/**
 * Credential verification for login. It owns the "may this account sign in?"
 * decision (password and status) and the user-safe failure contract, and never
 * returns the account row, the password or the stored hash to its caller.
 */
public class AuthenticationService {

    /**
     * One message for unknown username, wrong password and non-active account so
     * that login cannot be used to enumerate accounts.
     */
    private static final String GENERIC_FAILURE = "Invalid username or password.";

    /**
     * Throwaway credential verified when the username does not exist, so that
     * path costs the same PBKDF2 work as a wrong password and response timing
     * cannot reveal whether a username is registered. It is built once when this
     * class is loaded, not per request, and the verify result is discarded: it
     * can never authenticate an account.
     */
    private static final String DUMMY_CREDENTIAL = PasswordUtil.hash("timing-equalizer-not-a-password");

    private final AccountDAO accountDAO = new AccountDAO();

    public AuthenticationResult authenticate(String username, String password) {
        String normalizedUsername = normalize(username);
        String rawPassword = password == null ? "" : password;
        if (normalizedUsername.isEmpty() || rawPassword.isEmpty()) {
            return AuthenticationResult.failure(GENERIC_FAILURE);
        }

        Account credentials;
        try {
            credentials = accountDAO.findByUsernameWithCredentials(normalizedUsername);
        } catch (DataAccessException e) {
            throw new AuthenticationException("Login is temporarily unavailable. Please try again later.", e);
        }
        if (credentials == null) {
            PasswordUtil.verify(rawPassword, DUMMY_CREDENTIAL);
            return AuthenticationResult.failure(GENERIC_FAILURE);
        }
        // Wrong password and non-active status must be indistinguishable, so the
        // password is checked first and both produce the same generic failure.
        if (!PasswordUtil.verify(rawPassword, credentials.getPassword())) {
            return AuthenticationResult.failure(GENERIC_FAILURE);
        }
        if (!AccountService.STATUS_ACTIVE.equals(credentials.getStatus())) {
            return AuthenticationResult.failure(GENERIC_FAILURE);
        }

        CurrentUser currentUser = new CurrentUser(credentials.getId(), credentials.getUsername(),
                credentials.getType(), credentials.getRole(), Instant.now());
        return AuthenticationResult.success(currentUser);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
