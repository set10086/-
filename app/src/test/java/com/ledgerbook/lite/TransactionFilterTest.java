package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class TransactionFilterTest {
    @Test
    public void constructorNormalizesBlankOptionalValues() {
        TransactionFilter filter = new TransactionFilter(-1L, 10L, 20L, " ", null, 0L, "  ");
        assertEquals(-1L, filter.ledgerId);
        assertNull(filter.type);
        assertNull(filter.category);
        assertNull(filter.bookkeeper);
        assertEquals(0L, filter.accountId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsInvertedRange() {
        new TransactionFilter(1L, 20L, 10L, null, null, 0L, null);
    }

    @Test
    public void equalityIncludesEveryFilterField() {
        TransactionFilter a = new TransactionFilter(1L, 10L, 20L,
                LedgerDb.TYPE_EXPENSE, "餐饮", 7L, "本人");
        TransactionFilter b = new TransactionFilter(1L, 10L, 20L,
                LedgerDb.TYPE_EXPENSE, "餐饮", 7L, "本人");
        TransactionFilter c = new TransactionFilter(1L, 10L, 20L,
                LedgerDb.TYPE_INCOME, "工资", 7L, "本人");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    public void currentLedgerAndAllLedgerScopesAreDistinct() {
        TransactionFilter current = TransactionFilter.forCurrentLedger(9L, 100L, 200L);
        TransactionFilter all = TransactionFilter.forAllLedgers(100L, 200L);
        assertEquals(9L, current.ledgerId);
        assertEquals(-1L, all.ledgerId);
        assertNotEquals(current, all);
    }
}
