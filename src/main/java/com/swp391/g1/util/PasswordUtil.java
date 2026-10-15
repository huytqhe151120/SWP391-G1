package com.swp391.g1.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing for the account password column.
 *
 * PBKDF2-HMAC-SHA256 is used because it ships with the JDK, so authentication
 * needs no extra dependency. The stored value is self-describing
 * ("pbkdf2-sha256$iterations$salt$hash") so the iteration count can be raised
 * later without a schema change; account.password is VARCHAR(255) and the
 * encoded value is about 90 characters.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String FORMAT_PREFIX = "pbkdf2-sha256";
    private static final String SEPARATOR = "$";

    /** Iteration count used for newly generated hashes. */
    private static final int ITERATIONS = 600_000;

    /**
     * Accepted range for an iteration count read back from a stored credential.
     * The stored value is normally {@link #ITERATIONS}; the bounds stop a
     * tampered row from forcing unbounded (or trivially weak) PBKDF2 work.
     */
    private static final int MIN_ITERATIONS = 100_000;
    private static final int MAX_ITERATIONS = 5_000_000;

    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword, salt, ITERATIONS, KEY_BITS);
        Base64.Encoder encoder = Base64.getEncoder();
        return FORMAT_PREFIX + SEPARATOR + ITERATIONS + SEPARATOR
                + encoder.encodeToString(salt) + SEPARATOR + encoder.encodeToString(hash);
    }

    /**
     * Checks a raw password against a stored hash. Returns false for null input,
     * for any value that is not in the expected format (for example a leftover
     * plaintext development password) and for an out-of-range iteration count,
     * so such rows simply cannot authenticate.
     */
    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || rawPassword.isEmpty() || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split("\\" + SEPARATOR);
        if (parts.length != 4 || !FORMAT_PREFIX.equals(parts[0])) {
            return false;
        }
        int iterations;
        byte[] salt;
        byte[] expected;
        try {
            iterations = Integer.parseInt(parts[1]);
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
            return false;
        }
        if (salt.length == 0 || expected.length == 0) {
            return false;
        }
        byte[] actual = pbkdf2(rawPassword, salt, iterations, expected.length * 8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] pbkdf2(String rawPassword, byte[] salt, int iterations, int keyBits) {
        PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, keyBits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing is not available.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
