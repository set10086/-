package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public final class ModuleCatalogTest {
    @Test
    public void defaultsMatchProgressiveProductDesign() {
        List<LedgerV2Migration.ModuleSeed> modules = LedgerV2Migration.defaultModules();
        assertEquals(7, modules.size());

        assertModule(modules, "quick_entry", true, 0);
        assertModule(modules, "budget", true, 10);
        assertModule(modules, "data_management", true, 20);
        assertModule(modules, "templates", false, 30);
        assertModule(modules, "recurring", false, 40);
        assertModule(modules, "subscriptions", false, 50);
        assertModule(modules, "privacy_lock", false, 60);
    }

    @Test
    public void moduleKeysAreUniqueAndSortOrderIsStrictlyIncreasing() {
        List<LedgerV2Migration.ModuleSeed> modules = LedgerV2Migration.defaultModules();
        java.util.HashSet<String> keys = new java.util.HashSet<>();
        int previous = Integer.MIN_VALUE;
        for (LedgerV2Migration.ModuleSeed module : modules) {
            assertTrue(keys.add(module.key));
            assertFalse(module.key.trim().isEmpty());
            assertTrue(module.sortOrder > previous);
            previous = module.sortOrder;
        }
    }

    private static void assertModule(List<LedgerV2Migration.ModuleSeed> modules,
                                     String key, boolean enabled, int sortOrder) {
        for (LedgerV2Migration.ModuleSeed module : modules) {
            if (key.equals(module.key)) {
                assertEquals(enabled, module.enabled);
                assertEquals(sortOrder, module.sortOrder);
                return;
            }
        }
        throw new AssertionError("Missing module: " + key);
    }
}
