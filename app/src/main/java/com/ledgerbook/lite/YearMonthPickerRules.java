package com.ledgerbook.lite;

import java.time.YearMonth;

public final class YearMonthPickerRules {
    private YearMonthPickerRules() {
    }

    public static int minYear(int currentYear) {
        return currentYear - 30;
    }

    public static int maxYear(int currentYear) {
        return currentYear + 10;
    }

    public static YearMonth clamp(YearMonth value, int currentYear) {
        if (value == null) return YearMonth.of(currentYear, 1);
        int year = Math.max(minYear(currentYear), Math.min(maxYear(currentYear), value.getYear()));
        return YearMonth.of(year, value.getMonthValue());
    }

    public static YearMonth previousMonth(YearMonth value) {
        if (value == null) throw new IllegalArgumentException("月份不能为空");
        return value.minusMonths(1);
    }

    public static YearMonth currentYearJanuary(int currentYear) {
        return YearMonth.of(currentYear, 1);
    }

    public static YearMonth previousYearJanuary(int currentYear) {
        return YearMonth.of(currentYear - 1, 1);
    }
}
