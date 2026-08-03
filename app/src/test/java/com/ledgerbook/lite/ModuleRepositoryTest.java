package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class ModuleRepositoryTest {
    @Test
    public void enableDisableAndMovePersistThroughBackend() {
        FakeBackend backend = new FakeBackend();
        backend.values.add(new ModuleRepository.Module("quick_entry", true, 0));
        backend.values.add(new ModuleRepository.Module("budget", true, 10));
        backend.values.add(new ModuleRepository.Module("templates", false, 20));
        ModuleRepository repository = new ModuleRepository(backend);

        repository.setEnabled("templates", true);
        assertTrue(repository.isEnabled("templates"));

        repository.move("templates", -1);
        List<ModuleRepository.Module> modules = repository.list();
        assertEquals("quick_entry", modules.get(0).key);
        assertEquals("templates", modules.get(1).key);
        assertEquals("budget", modules.get(2).key);

        repository.setEnabled("budget", false);
        assertFalse(repository.isEnabled("budget"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownModuleCannotBeMutated() {
        FakeBackend backend = new FakeBackend();
        backend.values.add(new ModuleRepository.Module("budget", true, 0));
        new ModuleRepository(backend).setEnabled("missing", true);
    }

    private static final class FakeBackend implements ModuleRepository.Backend {
        private final List<ModuleRepository.Module> values = new ArrayList<>();

        @Override public List<ModuleRepository.Module> load() {
            List<ModuleRepository.Module> copy = new ArrayList<>(values);
            copy.sort(java.util.Comparator.comparingInt(value -> value.sortOrder));
            return copy;
        }

        @Override public void setEnabled(String key, boolean enabled, long now) {
            for (int index = 0; index < values.size(); index++) {
                ModuleRepository.Module value = values.get(index);
                if (value.key.equals(key)) {
                    values.set(index, new ModuleRepository.Module(key, enabled, value.sortOrder));
                    return;
                }
            }
            throw new IllegalArgumentException("missing module");
        }

        @Override public void swapOrder(String firstKey, int firstOrder,
                                        String secondKey, int secondOrder, long now) {
            for (int index = 0; index < values.size(); index++) {
                ModuleRepository.Module value = values.get(index);
                if (value.key.equals(firstKey)) {
                    values.set(index, new ModuleRepository.Module(value.key, value.enabled, secondOrder));
                } else if (value.key.equals(secondKey)) {
                    values.set(index, new ModuleRepository.Module(value.key, value.enabled, firstOrder));
                }
            }
        }
    }
}
