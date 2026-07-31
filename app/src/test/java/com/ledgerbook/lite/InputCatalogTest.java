package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.Test;

public class InputCatalogTest {
    @Test
    public void expenseCategoriesHaveIconsAndCustomOption() {
        List<InputCatalog.Option> options = InputCatalog.categories(LedgerDb.TYPE_EXPENSE);
        assertEquals("🍜", options.get(0).icon);
        assertEquals("餐饮", options.get(0).label);
        assertTrue(options.stream().anyMatch(option -> option.custom));
    }

    @Test
    public void incomeCategoriesUseIncomeCatalog() {
        List<InputCatalog.Option> options = InputCatalog.categories(LedgerDb.TYPE_INCOME);
        assertEquals("💰", options.get(0).icon);
        assertEquals("工资", options.get(0).label);
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
