package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public final class PreferenceRepositoryTest {
    @Test
    public void typedValuesRoundTripAndFallbackSafely() {
        FakeBackend backend = new FakeBackend();
        PreferenceRepository repository = new PreferenceRepository(backend);

        assertEquals("fallback", repository.getString("missing", "fallback"));
        assertTrue(repository.getBoolean("missing_bool", true));
        assertEquals(42L, repository.getLong("missing_long", 42L));

        repository.putString("home_order", "budget,recent");
        repository.putBoolean("quick_more_expanded", false);
        repository.putLong("last_backup", 1234L);

        assertEquals("budget,recent", repository.getString("home_order", ""));
        assertFalse(repository.getBoolean("quick_more_expanded", true));
        assertEquals(1234L, repository.getLong("last_backup", 0L));

        backend.values.put("broken_bool", "not-a-boolean");
        backend.values.put("broken_long", "not-a-number");
        assertTrue(repository.getBoolean("broken_bool", true));
        assertEquals(9L, repository.getLong("broken_long", 9L));

        repository.remove("home_order");
        assertEquals("reset", repository.getString("home_order", "reset"));
    }

    private static final class FakeBackend implements PreferenceRepository.Backend {
        private final Map<String, String> values = new HashMap<>();

        @Override public String get(String key) {
            return values.get(key);
        }

        @Override public void put(String key, String value, long now) {
            values.put(key, value);
        }

        @Override public void remove(String key) {
            values.remove(key);
        }
    }
}
