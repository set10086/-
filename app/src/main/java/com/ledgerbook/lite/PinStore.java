package com.ledgerbook.lite;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists only a salted PIN digest and retry/timeout metadata. */
public final class PinStore {
    private static final String PREFS = "ledgerbook_v15_privacy";
    private static final String SALT = "pin_salt";
    private static final String HASH = "pin_hash";
    private static final String ITERATIONS = "pin_iterations";
    private static final String FAILURES = "pin_failures";
    private static final String LOCKED_UNTIL = "pin_locked_until";
    private static final String LAST_BACKGROUND = "last_background";
    private static final String TIMEOUT_SECONDS = "timeout_seconds";
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    public enum Status {
        SUCCESS,
        WRONG,
        LOCKED,
        NOT_CONFIGURED
    }

    public static final class Verification {
        public final Status status;
        public final long remainingMillis;

        Verification(Status status, long remainingMillis) {
            this.status = status;
            this.remainingMillis = Math.max(0L, remainingMillis);
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }
    }

    private final SharedPreferences preferences;
    private boolean sessionUnlocked;

    public PinStore(Context context) {
        if (context == null) throw new IllegalArgumentException("context cannot be null");
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isConfigured() {
        return !preferences.getString(SALT, "").isEmpty()
                && !preferences.getString(HASH, "").isEmpty()
                && preferences.getInt(ITERATIONS, 0) > 0;
    }

    public void savePin(char[] pin) {
        PinSecurity.Credential credential = PinSecurity.create(pin);
        preferences.edit()
                .putString(SALT, credential.encodedSalt)
                .putString(HASH, credential.encodedHash)
                .putInt(ITERATIONS, credential.iterations)
                .putInt(FAILURES, 0)
                .putLong(LOCKED_UNTIL, 0L)
                .remove(LAST_BACKGROUND)
                .apply();
        sessionUnlocked = true;
    }

    public void removePin() {
        preferences.edit()
                .remove(SALT)
                .remove(HASH)
                .remove(ITERATIONS)
                .remove(FAILURES)
                .remove(LOCKED_UNTIL)
                .remove(LAST_BACKGROUND)
                .apply();
        sessionUnlocked = false;
    }

    public Verification verify(char[] pin) {
        long now = System.currentTimeMillis();
        if (!isConfigured()) return new Verification(Status.NOT_CONFIGURED, 0L);
        PinSecurity.AttemptState state = currentAttemptState(now);
        if (state.isLocked(now)) {
            return new Verification(Status.LOCKED, state.remainingMillis(now));
        }

        PinSecurity.Credential credential = new PinSecurity.Credential(
                preferences.getString(SALT, ""),
                preferences.getString(HASH, ""),
                preferences.getInt(ITERATIONS, PinSecurity.ITERATIONS));
        if (PinSecurity.verify(pin, credential)) {
            preferences.edit()
                    .putInt(FAILURES, 0)
                    .putLong(LOCKED_UNTIL, 0L)
                    .remove(LAST_BACKGROUND)
                    .apply();
            sessionUnlocked = true;
            return new Verification(Status.SUCCESS, 0L);
        }

        int failures = state.failures + 1;
        PinSecurity.AttemptState failed = PinSecurity.afterFailure(failures, now);
        preferences.edit()
                .putInt(FAILURES, failed.failures)
                .putLong(LOCKED_UNTIL, failed.lockedUntil)
                .apply();
        return new Verification(failed.isLocked(now) ? Status.LOCKED : Status.WRONG,
                failed.remainingMillis(now));
    }

    public void markUnlocked() {
        sessionUnlocked = true;
        preferences.edit().remove(LAST_BACKGROUND).apply();
    }

    public void markLocked() {
        sessionUnlocked = false;
    }

    public void markBackground(long now) {
        if (!isConfigured() || !sessionUnlocked) return;
        preferences.edit().putLong(LAST_BACKGROUND, Math.max(0L, now)).apply();
    }

    public boolean shouldLock(long now) {
        if (!isConfigured()) return false;
        if (!sessionUnlocked) return true;
        long background = preferences.getLong(LAST_BACKGROUND, 0L);
        if (background <= 0L) return false;
        long timeout = getTimeoutSeconds() * 1000L;
        return timeout == 0L || now - background >= timeout;
    }

    public int getTimeoutSeconds() {
        int value = preferences.getInt(TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
        return isSupportedTimeout(value) ? value : DEFAULT_TIMEOUT_SECONDS;
    }

    public void setTimeoutSeconds(int seconds) {
        if (!isSupportedTimeout(seconds)) {
            throw new IllegalArgumentException("不支持的自动锁定时间");
        }
        preferences.edit().putInt(TIMEOUT_SECONDS, seconds).apply();
    }

    private PinSecurity.AttemptState currentAttemptState(long now) {
        int failures = Math.max(0, preferences.getInt(FAILURES, 0));
        long lockedUntil = Math.max(0L, preferences.getLong(LOCKED_UNTIL, 0L));
        if (lockedUntil > 0L && lockedUntil <= now) {
            failures = 0;
            lockedUntil = 0L;
            preferences.edit()
                    .putInt(FAILURES, 0)
                    .putLong(LOCKED_UNTIL, 0L)
                    .apply();
        }
        return new PinSecurity.AttemptState(failures, lockedUntil);
    }

    private static boolean isSupportedTimeout(int seconds) {
        return seconds == 0 || seconds == 30 || seconds == 60
                || seconds == 300 || seconds == 900;
    }
}
