package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.Test;

public class InputCatalogTest {
    @Test
    public void expenseCategoriesHaveIconsAndCustomOption() {
        List<InputCatalog.Option> options = InputCatalog.categories(LedgerDb.TYPE_EXPENSE);
        assertFalse(options.isEmpty());
        assertTrue(options.stream().allMatch(option -> !option.icon.trim().isEmpty()));
        assertTrue(options.stream().anyMatch(option -> option.label.equals("餐饮/早餐")));
        assertTrue(options.stream().anyMatch(option -> option.custom));
    }

    @Test
    public void incomeCategoriesUseIncomeCatalog() {
        List<InputCatalog.Option> options = InputCatalog.categories(LedgerDb.TYPE_INCOME);
        assertFalse(options.isEmpty());
        assertTrue(options.stream().anyMatch(option -> option.icon.equals("💰")
                && option.label.equals("工资收入/工资")));
    }

    @Test
    public void transferCategoryIsFixed() {
        List<InputCatalog.Option> options = InputCatalog.categories(LedgerDb.TYPE_TRANSFER);
        assertEquals(1, options.size());
        assertEquals("账户转账", options.get(0).label);
    }

    @Test
    public void bookkeepersMergePresetsAndHistoryWithoutDuplicates() {
        LinkedHashSet<String> history = new LinkedHashSet<>(Arrays.asList("家人", "妈妈", "本人"));
        List<String> result = InputCatalog.mergeBookkeepers(history);
        assertEquals(Arrays.asList("本人", "家人", "伴侣", "孩子", "妈妈"), result);
    }
}
