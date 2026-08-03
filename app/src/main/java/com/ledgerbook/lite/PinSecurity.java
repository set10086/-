package com.ledgerbook.lite;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Pure PIN credential and retry-throttling rules. */
public final class PinSecurity {
    public static final int ITERATIONS = 120_000;
    public static final int KEY_BITS = 256;
    public static final int SALT_BYTES = 16;
    public static final int MAX_FAILURES_BEFORE_LOCK = 5;
    public static final long LOCKOUT_MILLIS = 30_000L;

    public static final class Credential {
        public final String encodedSalt;
        public final String encodedHash;
        public final int iterations;

        public Credential(String encodedSalt, String encodedHash, int iterations) {
            if (encodedSalt == null || encodedSalt.isEmpty()
                    || encodedHash == null || encodedHash.isEmpty()
                    || iterations <= 0) {
                throw new IllegalArgumentException("PIN credential is invalid");
            }
            this.encodedSalt = encodedSalt;
            this.encodedHash = encodedHash;
            this.iterations = iterations;
        }
    }

    public static final class AttemptState {
        public final int failures;
        public final long lockedUntil;

        public AttemptState(int failures, long lockedUntil) {
            this.failures = Math.max(0, failures);
            this.lockedUntil = Math.max(0L, lockedUntil);
        }

        public boolean isLocked(long now) {
            return lockedUntil > now;
        }

        public long remainingMillis(long now) {
            return Math.max(0L, lockedUntil - now);
        }
    }

    private PinSecurity() {
    }

    public static Credential create(char[] pin) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return create(pin, salt);
    }

    public static Credential create(char[] pin, byte[] salt) {
        validatePin(pin);
        if (salt == null || salt.length < SALT_BYTES) {
            throw new IllegalArgumentException("PIN salt must contain at least 16 bytes");
        }
        byte[] saltCopy = Arrays.copyOf(salt, salt.length);
        byte[] hash = derive(pin, saltCopy, ITERATIONS);
        try {
            return new Credential(
                    Base64.getEncoder().encodeToString(saltCopy),
                    Base64.getEncoder().encodeToString(hash),
                    ITERATIONS);
        } finally {
            Arrays.fill(hash, (byte) 0);
            Arrays.fill(saltCopy, (byte) 0);
        }
    }

    public static boolean verify(char[] pin, Credential credential) {
        if (credential == null) return false;
        validatePin(pin);
        byte[] salt;
        byte[] expected;
        try {
            salt = Base64.getDecoder().decode(credential.encodedSalt);
            expected = Base64.getDecoder().decode(credential.encodedHash);
        } catch (IllegalArgumentException error) {
            return false;
        }
        byte[] actual = derive(pin, salt, credential.iterations);
        try {
            return MessageDigest.isEqual(expected, actual);
        } finally {
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(expected, (byte) 0);
            Arrays.fill(actual, (byte) 0);
        }
    }

    public static AttemptState afterFailure(int failures, long now) {
        int safeFailures = Math.max(1, failures);
        long lockedUntil = safeFailures >= MAX_FAILURES_BEFORE_LOCK
                ? safeAdd(now, LOCKOUT_MILLIS) : 0L;
        return new AttemptState(safeFailures, lockedUntil);
    }

    public static AttemptState afterSuccess() {
        return new AttemptState(0, 0L);
    }

    public static void validatePin(char[] pin) {
        if (pin == null || pin.length < 4 || pin.length > 8) {
            throw new IllegalArgumentException("PIN 必须为 4 至 8 位数字");
        }
        for (char value : pin) {
            if (value < '0' || value > '9') {
                throw new IllegalArgumentException("PIN 必须为 4 至 8 位数字");
            }
        }
    }

    private static byte[] derive(char[] pin, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(pin, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("无法生成 PIN 摘要", error);
        } finally {
            spec.clearPassword();
        }
    }

    private static long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException error) {
            return Long.MAX_VALUE;
        }
    }
}
