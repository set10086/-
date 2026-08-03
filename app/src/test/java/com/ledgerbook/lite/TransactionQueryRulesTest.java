package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

public final class TransactionQueryRulesTest {
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    @Test
    public void escapeLikeTreatsWildcardsAndEscapeAsPlainText() {
        assertEquals("100\\%\\_\\\\", TransactionQueryRules.escapeLike("100%_\\"));
    }

    @Test
    public void monthGridAlwaysContainsFortyTwoMondayFirstDates() {
        List<LocalDate> cells = TransactionQueryRules.monthGrid(YearMonth.of(2026, 8));
        assertEquals(42, cells.size());
        assertEquals(DayOfWeek.MONDAY, cells.get(0).getDayOfWeek());
        assertEquals(LocalDate.of(2026, 7, 27), cells.get(0));
        assertEquals(LocalDate.of(2026, 9, 6), cells.get(41));
    }

    @Test
    public void monthBoundsUseHalfOpenNaturalMonthRange() {
        TransactionQueryRules.Range range = TransactionQueryRules.monthBounds(YearMonth.of(2026, 2), TOKYO);
        assertEquals(LocalDate.of(2026, 2, 1).atStartOfDay(TOKYO).toInstant().toEpochMilli(), range.fromInclusive);
        assertEquals(LocalDate.of(2026, 3, 1).atStartOfDay(TOKYO).toInstant().toEpochMilli(), range.toExclusive);
    }

    @Test
    public void previousMonthAcrossJanuaryUsesPreviousYear() {
        TransactionQueryRules.Range range = TransactionQueryRules.previousMonthBounds(YearMonth.of(2026, 1), TOKYO);
        assertEquals(LocalDate.of(2025, 12, 1).atStartOfDay(TOKYO).toInstant().toEpochMilli(), range.fromInclusive);
        assertEquals(LocalDate.of(2026, 1, 1).atStartOfDay(TOKYO).toInstant().toEpochMilli(), range.toExclusive);
    }

    @Test
    public void compactAmountUsesWanSuffixOnlyWhenNeeded() {
        assertEquals("0", TransactionQueryRules.compactAmount(0));
        assertEquals("99.99", TransactionQueryRules.compactAmount(9999));
        assertEquals("100", TransactionQueryRules.compactAmount(10000));
        assertEquals("1.2万", TransactionQueryRules.compactAmount(1_200_000));
        assertEquals("-1.2万", TransactionQueryRules.compactAmount(-1_200_000));
    }

    @Test
    public void dayBoundsCoverExactlyOneLocalDay() {
        TransactionQueryRules.Range range = TransactionQueryRules.dayBounds(LocalDate.of(2026, 7, 31), TOKYO);
        long hours = (range.toExclusive - range.fromInclusive) / (60L * 60L * 1000L);
        assertTrue(hours >= 23 && hours <= 25);
    }
}
