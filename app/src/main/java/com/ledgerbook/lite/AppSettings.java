package com.ledgerbook.lite;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppSettings {
    private static final String PREFS = "ledgerbook_v14_settings";
    private static final String DEFAULT_LEDGER = "default_ledger_id";
    private static final String DEFAULT_ACCOUNT_PREFIX = "default_account_";
    private static final String DEFAULT_BOOKKEEPER = "default_bookkeeper";
    private static final String STARTUP_PAGE = "startup_page";
    private static final String ANIMATIONS = "animations";
    private static final String RETURN_HOME = "return_home_after_save";

    private final SharedPreferences preferences;

    public AppSettings(Context context) {
        if (context == null) throw new IllegalArgumentException("context cannot be null");
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public long defaultLedgerId() {
        return preferences.getLong(DEFAULT_LEDGER, -1L);
    }

    public void setDefaultLedgerId(long ledgerId) {
        preferences.edit().putLong(DEFAULT_LEDGER, ledgerId > 0L ? ledgerId : -1L).apply();
    }

    public long defaultAccountId(long ledgerId) {
        if (ledgerId <= 0L) return -1L;
        return preferences.getLong(DEFAULT_ACCOUNT_PREFIX + ledgerId, -1L);
    }

    public void setDefaultAccountId(long ledgerId, long accountId) {
        if (ledgerId <= 0L) return;
        SharedPreferences.Editor editor = preferences.edit();
        if (accountId > 0L) editor.putLong(DEFAULT_ACCOUNT_PREFIX + ledgerId, accountId);
        else editor.remove(DEFAULT_ACCOUNT_PREFIX + ledgerId);
        editor.apply();
    }

    public String defaultBookkeeper() {
        String value = preferences.getString(DEFAULT_BOOKKEEPER, "本人");
        return value == null || value.trim().isEmpty() ? "本人" : value.trim();
    }

    public void setDefaultBookkeeper(String value) {
        String clean = value == null ? "" : value.trim();
        preferences.edit().putString(DEFAULT_BOOKKEEPER,
                clean.isEmpty() ? "本人" : clean).apply();
    }

    public int startupPage() {
        return clampPage(preferences.getInt(STARTUP_PAGE, 0));
    }

    public void setStartupPage(int page) {
        preferences.edit().putInt(STARTUP_PAGE, clampPage(page)).apply();
    }

    public boolean animationsEnabled() {
        return preferences.getBoolean(ANIMATIONS, true);
    }

    public void setAnimationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(ANIMATIONS, enabled).apply();
    }

    public boolean returnHomeAfterSave() {
        return preferences.getBoolean(RETURN_HOME, false);
    }

    public void setReturnHomeAfterSave(boolean enabled) {
        preferences.edit().putBoolean(RETURN_HOME, enabled).apply();
    }

    private static int clampPage(int page) {
        return Math.max(0, Math.min(4, page));
    }
}
