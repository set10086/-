package com.ledgerbook.lite;

import org.junit.Test;

import java.time.YearMonth;

import static org.junit.Assert.assertEquals;

public final class YearMonthPickerRulesTest {
    @Test
    public void supportedYearRangeIsThirtyYearsBackAndTenYearsForward() {
        assertEquals(1996, YearMonthPickerRules.minYear(2026));
        assertEquals(2036, YearMonthPickerRules.maxYear(2026));
    }

    @Test
    public void clampKeepsMonthAndRestrictsYear() {
        assertEquals(YearMonth.of(1996, 7),
                YearMonthPickerRules.clamp(YearMonth.of(1980, 7), 2026));
        assertEquals(YearMonth.of(2036, 11),
                YearMonthPickerRules.clamp(YearMonth.of(2050, 11), 2026));
        assertEquals(YearMonth.of(2024, 2),
                YearMonthPickerRules.clamp(YearMonth.of(2024, 2), 2026));
    }

    @Test
    public void previousMonthCrossesYearBoundary() {
        assertEquals(YearMonth.of(2025, 12),
                YearMonthPickerRules.previousMonth(YearMonth.of(2026, 1)));
    }

    @Test
    public void yearShortcutsPointToJanuary() {
        assertEquals(YearMonth.of(2026, 1),
                YearMonthPickerRules.currentYearJanuary(2026));
        assertEquals(YearMonth.of(2025, 1),
                YearMonthPickerRules.previousYearJanuary(2026));
    }
}
