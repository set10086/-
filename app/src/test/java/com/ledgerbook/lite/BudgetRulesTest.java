package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class BudgetRulesTest {
    @Test
    public void safeBudgetComputesRemainingAndDailyAllowance() {
        BudgetRules.Status status = BudgetRules.calculate(100_000L, 50_000L, 11, 30);
        assertEquals(100_000L, status.limitCents);
        assertEquals(50_000L, status.spentCents);
        assertEquals(50_000L, status.remainingCents);
        assertEquals(0L, status.overCents);
        assertEquals(50, status.progressPercent);
        assertEquals(20, status.remainingDays);
        assertEquals(2_500L, status.dailyAvailableCents);
        assertEquals(BudgetRules.Level.SAFE, status.level);
    }

    @Test
    public void warningBeginsAtEightyPercent() {
        assertEquals(BudgetRules.Level.WARNING,
                BudgetRules.calculate(100_000L, 80_000L, 15, 30).level);
        assertEquals(BudgetRules.Level.WARNING,
                BudgetRules.calculate(100_000L, 99_999L, 15, 30).level);
    }

    @Test
    public void reachingOrExceedingLimitIsOver() {
        BudgetRules.Status exact = BudgetRules.calculate(100_000L, 100_000L, 30, 30);
        assertEquals(BudgetRules.Level.OVER, exact.level);
        assertEquals(0L, exact.remainingCents);
        assertEquals(0L, exact.dailyAvailableCents);

        BudgetRules.Status exceeded = BudgetRules.calculate(100_000L, 125_000L, 20, 31);
        assertEquals(25_000L, exceeded.overCents);
        assertEquals(125, exceeded.progressPercent);
        assertEquals(BudgetRules.Level.OVER, exceeded.level);
    }

    @Test
    public void absentBudgetHasNoneState() {
        BudgetRules.Status status = BudgetRules.calculate(0L, 12_000L, 1, 31);
        assertEquals(BudgetRules.Level.NONE, status.level);
        assertEquals(0, status.progressPercent);
        assertEquals(0L, status.dailyAvailableCents);
    }

    @Test
    public void firstLevelCategoryMatchesDescendantsButLeafIsExact() {
        assertEquals(true, BudgetRules.matchesCategory("餐饮", "餐饮"));
        assertEquals(true, BudgetRules.matchesCategory("餐饮", "餐饮/午餐"));
        assertEquals(false, BudgetRules.matchesCategory("餐饮", "购物/餐具"));
        assertEquals(true, BudgetRules.matchesCategory("餐饮/午餐", "餐饮/午餐"));
        assertEquals(false, BudgetRules.matchesCategory("餐饮/午餐", "餐饮/晚餐"));
    }
}
