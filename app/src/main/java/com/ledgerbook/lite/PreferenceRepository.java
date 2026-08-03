package com.ledgerbook.lite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Stores typed V1.5 preferences in SQLite without coupling callers to SQL. */
public final class PreferenceRepository {
    public interface Backend {
        String get(String key);
        void put(String key, String value, long now);
        void remove(String key);
    }

    private final Backend backend;

    public PreferenceRepository(Backend backend) {
        if (backend == null) throw new IllegalArgumentException("backend cannot be null");
        this.backend = backend;
    }

    public PreferenceRepository(LedgerDb db) {
        this(new AndroidBackend(db));
    }

    public String getString(String key, String fallback) {
        String value = backend.get(requireKey(key));
        return value == null ? fallback : value;
    }

    public void putString(String key, String value) {
        backend.put(requireKey(key), value == null ? "" : value, System.currentTimeMillis());
    }

    public boolean getBoolean(String key, boolean fallback) {
        String value = backend.get(requireKey(key));
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return fallback;
    }

    public void putBoolean(String key, boolean value) {
        putString(key, Boolean.toString(value));
    }

    public long getLong(String key, long fallback) {
        String value = backend.get(requireKey(key));
        if (value == null) return fallback;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public void putLong(String key, long value) {
        putString(key, Long.toString(value));
    }

    public void remove(String key) {
        backend.remove(requireKey(key));
    }

    private static String requireKey(String key) {
        String clean = key == null ? "" : key.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("preference key cannot be empty");
        return clean;
    }

    private static final class AndroidBackend implements Backend {
        private final LedgerDb helper;

        AndroidBackend(LedgerDb helper) {
            if (helper == null) throw new IllegalArgumentException("db cannot be null");
            this.helper = helper;
        }

        @Override public String get(String key) {
            try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                    "SELECT pref_value FROM user_preferences WHERE pref_key=?",
                    new String[]{key})) {
                return cursor.moveToFirst() ? cursor.getString(0) : null;
            }
        }

        @Override public void put(String key, String value, long now) {
            ContentValues values = new ContentValues();
            values.put("pref_key", key);
            values.put("pref_value", value);
            values.put("updated_at", now);
            long result = helper.getWritableDatabase().insertWithOnConflict(
                    "user_preferences", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            if (result < 0L) throw new IllegalStateException("偏好保存失败");
        }

        @Override public void remove(String key) {
            helper.getWritableDatabase().delete(
                    "user_preferences", "pref_key=?", new String[]{key});
        }
    }
}
