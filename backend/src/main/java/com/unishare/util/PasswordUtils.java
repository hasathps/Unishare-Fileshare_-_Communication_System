package com.unishare.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Locale;

/**
 * Utility methods for hashing and verifying passwords using PBKDF2.
 */
public final class PasswordUtils {

    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtils() {
    }

    public static String hashPassword(char[] password) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH);
        clear(password);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);
        return String.format(Locale.ROOT, "%d:%s:%s", ITERATIONS, saltB64, hashB64);
    }

    public static boolean verifyPassword(char[] password, String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return false;
        }

        String[] parts = storedValue.split(":");
        if (parts.length != 3) {
            return false;
        }

        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

        byte[] actualHash = pbkdf2(password, salt, iterations, expectedHash.length * 8);
        clear(password);

        if (actualHash.length != expectedHash.length) {
            return false;
        }

        int diff = 0;
        for (int i = 0; i < actualHash.length; i++) {
            diff |= actualHash[i] ^ expectedHash[i];
        }
        return diff == 0;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Unable to hash password", e);
        }
    }

    private static void clear(char[] value) {
        if (value != null) {
            for (int i = 0; i < value.length; i++) {
                value[i] = 0;
            }
        }
    }
}

