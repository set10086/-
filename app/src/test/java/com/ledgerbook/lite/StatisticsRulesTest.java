package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class StatisticsRulesTest {
    @Test
    public void sixMonthTrendIsOldestFirstAndFillsMissingMonths() {
        List<StatisticsRules.MonthTotals> raw = Arrays.asList(
                new StatisticsRules.MonthTotals("2026-02", 50_000L, 30_000L),
                new StatisticsRules.MonthTotals("2026-04", 60_000L, 45_000L),
                new StatisticsRules.MonthTotals("2026-06", 80_000L, 55_000L));

        List<StatisticsRules.MonthTotals> result = StatisticsRules.completeTrend(
                YearMonth.of(2026, 6), 6, raw);

        assertEquals(6, result.size());
        assertEquals("2026-01", result.get(0).monthKey);
        assertEquals(0L, result.get(0).incomeCents);
        assertEquals(0L, result.get(0).expenseCents);
        assertEquals("2026-02", result.get(1).monthKey);
        assertEquals(50_000L, result.get(1).incomeCents);
        assertEquals("2026-05", result.get(4).monthKey);
        assertEquals(0L, result.get(4).expenseCents);
        assertEquals("2026-06", result.get(5).monthKey);
        assertEquals(55_000L, result.get(5).expenseCents);
    }

    @Test
    public void comparisonHandlesIncreaseDecreaseFlatAndNoPreviousBase() {
        StatisticsRules.Comparison increase = StatisticsRules.compare(12_500L, 10_000L);
        assertEquals(2_500L, increase.deltaCents);
        assertEquals(25, increase.percentChange);
        assertTrue(increase.hasPercentage);
        assertEquals(StatisticsRules.Direction.UP, increase.direction);

        StatisticsRules.Comparison decrease = StatisticsRules.compare(7_500L, 10_000L);
        assertEquals(-2_500L, decrease.deltaCents);
        assertEquals(-25, decrease.percentChange);
        assertEquals(StatisticsRules.Direction.DOWN, decrease.direction);

        StatisticsRules.Comparison flat = StatisticsRules.compare(0L, 0L);
        assertEquals(StatisticsRules.Direction.FLAT, flat.direction);
        assertTrue(flat.hasPercentage);
        assertEquals(0, flat.percentChange);

        StatisticsRules.Comparison newValue = StatisticsRules.compare(5_000L, 0L);
        assertEquals(StatisticsRules.Direction.NEW, newValue.direction);
        assertFalse(newValue.hasPercentage);
    }

    @Test
    public void categoryRankingKeepsTopFiveAndAggregatesTheRest() {
        List<StatisticsRules.CategoryAmount> values = Arrays.asList(
                new StatisticsRules.CategoryAmount("餐饮", 4_000L),
                new StatisticsRules.CategoryAmount("交通", 2_000L),
                new StatisticsRules.CategoryAmount("购物", 1_500L),
                new StatisticsRules.CategoryAmount("居住", 1_000L),
                new StatisticsRules.CategoryAmount("医疗", 700L),
                new StatisticsRules.CategoryAmount("娱乐", 500L),
                new StatisticsRules.CategoryAmount("学习", 300L));

        List<StatisticsRules.CategoryShare> ranking = StatisticsRules.rankCategories(values, 5);

        assertEquals(6, ranking.size());
        assertEquals("餐饮", ranking.get(0).category);
        assertEquals(4_000L, ranking.get(0).totalCents);
        assertEquals(40, ranking.get(0).percent);
        assertEquals("其他", ranking.get(5).category);
        assertEquals(800L, ranking.get(5).totalCents);
        assertEquals(8, ranking.get(5).percent);
    }

    @Test
    public void invalidAndZeroCategoryRowsAreIgnored() {
        List<StatisticsRules.CategoryShare> result = StatisticsRules.rankCategories(
                Arrays.asList(
                        new StatisticsRules.CategoryAmount("", 1_000L),
                        new StatisticsRules.CategoryAmount("交通", 0L),
                        new StatisticsRules.CategoryAmount("餐饮", 2_000L)),
                5);
        assertEquals(1, result.size());
        assertEquals("餐饮", result.get(0).category);
        assertEquals(100, result.get(0).percent);
    }
}
