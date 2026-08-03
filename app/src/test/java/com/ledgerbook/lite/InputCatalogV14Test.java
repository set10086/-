package com.ledgerbook.lite;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class InputCatalogV14Test {
    @Test
    public void expenseCatalogHasBroadGroupedCoverage() {
        List<InputCatalog.Group> groups = InputCatalog.groups(LedgerDb.TYPE_EXPENSE);
        List<InputCatalog.Option> options = InputCatalog.categories(LedgerDb.TYPE_EXPENSE);

        assertTrue("expected at least 16 expense groups", groups.size() >= 16);
        assertTrue("expected at least 120 expense options", options.size() >= 120);
    }

    @Test
    public void everySelectableOptionHasUniqueLabelAndIndependentIcon() {
        List<InputCatalog.Option> options = InputCatalog.categories(LedgerDb.TYPE_EXPENSE);
        Set<String> labels = new HashSet<>();
        for (InputCatalog.Option option : options) {
            if (option.custom) continue;
            assertFalse(option.icon.trim().isEmpty());
            assertFalse(option.label.trim().isEmpty());
            assertFalse(option.name.trim().isEmpty());
            assertFalse(option.group.trim().isEmpty());
            assertTrue("duplicate label: " + option.label, labels.add(option.label));
        }
    }

    @Test
    public void searchFindsCoffeeAcrossAllGroups() {
        List<InputCatalog.Option> result = InputCatalog.search(
                LedgerDb.TYPE_EXPENSE, "咖啡");

        assertFalse(result.isEmpty());
        assertEquals("餐饮/咖啡", result.get(0).label);
        assertEquals("☕", result.get(0).icon);
    }

    @Test
    public void iconLookupSupportsNewAndLegacyCategoryValues() {
        assertEquals("☕", InputCatalog.iconFor(
                LedgerDb.TYPE_EXPENSE, "餐饮/咖啡"));
        String legacy = InputCatalog.iconFor(LedgerDb.TYPE_EXPENSE, "餐饮");
        assertNotNull(legacy);
        assertFalse(legacy.trim().isEmpty());
    }

    @Test
    public void incomeCatalogAlsoUsesGroupedIconBackedOptions() {
        List<InputCatalog.Group> groups = InputCatalog.groups(LedgerDb.TYPE_INCOME);
        List<InputCatalog.Option> options = InputCatalog.categories(LedgerDb.TYPE_INCOME);

        assertTrue(groups.size() >= 4);
        assertTrue(options.size() >= 20);
        for (InputCatalog.Option option : options) {
            assertFalse(option.icon.trim().isEmpty());
        }
    }
}
