package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class QuickEntryRulesTest {
    @Test
    public void recentCategoriesAreUniqueNewestFirstAndTypeScoped() {
        List<LedgerDb.Txn> rows = Arrays.asList(
                txn(5, LedgerDb.TYPE_EXPENSE, "餐饮/午餐", 3500, 2, 5000),
                txn(4, LedgerDb.TYPE_INCOME, "职业收入/工资", 800000, 2, 4000),
                txn(3, LedgerDb.TYPE_EXPENSE, "交通/地铁", 600, 1, 3000),
                txn(2, LedgerDb.TYPE_EXPENSE, "餐饮/午餐", 3200, 1, 2000),
                txn(1, LedgerDb.TYPE_EXPENSE, "购物/日用品", 1800, 1, 1000));

        List<QuickEntryRules.CategoryChoice> result = QuickEntryRules.recentCategories(
                rows, LedgerDb.TYPE_EXPENSE, 3);

        assertEquals(3, result.size());
        assertEquals("餐饮/午餐", result.get(0).label);
        assertEquals("交通/地铁", result.get(1).label);
        assertEquals("购物/日用品", result.get(2).label);
        assertFalse(result.get(0).icon.trim().isEmpty());
    }

    @Test
    public void copyPreservesBookkeepingFieldsButUsesRequestedTime() {
        LedgerDb.Txn source = new LedgerDb.Txn(9, LedgerDb.TYPE_EXPENSE,
                "餐饮/咖啡", 2800, 300, 7, "微信", null, null,
                "家人", "下午茶", true, false, "少糖", 1000);

        QuickEntryRules.Snapshot copy = QuickEntryRules.copyOf(source, 99_000L);

        assertEquals(LedgerDb.TYPE_EXPENSE, copy.type);
        assertEquals("餐饮/咖啡", copy.category);
        assertEquals(2800L, copy.amountCents);
        assertEquals(300L, copy.discountCents);
        assertEquals(7L, copy.accountId);
        assertNull(copy.toAccountId);
        assertEquals("家人", copy.bookkeeper);
        assertEquals("下午茶", copy.tags);
        assertEquals(true, copy.reimbursable);
        assertEquals(false, copy.includeBudget);
        assertEquals("少糖", copy.note);
        assertEquals(99_000L, copy.occurredAt);
    }

    @Test
    public void preferredAccountFallsBackToFirstAvailableAccount() {
        List<LedgerDb.Account> accounts = Arrays.asList(
                new LedgerDb.Account(3, 1, "现金", "现金账户", "常用", 0, ""),
                new LedgerDb.Account(4, 1, "工资卡", "储蓄账户", "银行卡", 0, ""));
        assertEquals(4L, QuickEntryRules.preferredAccount(accounts, 4).id);
        assertEquals(3L, QuickEntryRules.preferredAccount(accounts, 99).id);
    }

    private static LedgerDb.Txn txn(long id, String type, String category,
                                    long amount, long accountId, long time) {
        return new LedgerDb.Txn(id, type, category, amount, 0, accountId, "账户",
                null, null, "本人", "", false, true, "", time);
    }
}
