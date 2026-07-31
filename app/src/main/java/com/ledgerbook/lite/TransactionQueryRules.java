package com.ledgerbook.lite;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TransactionQueryRules {
    public static final class Range {
        public final long fromInclusive;
        public final long toExclusive;

        public Range(long fromInclusive, long toExclusive) {
            if (fromInclusive >= toExclusive) {
                throw new IllegalArgumentException("结束时间必须晚于开始时间");
            }
            this.fromInclusive = fromInclusive;
            this.toExclusive = toExclusive;
        }
    }

    private TransactionQueryRules() {
    }

    public static String escapeLike(String value) {
        String safe = value == null ? "" : value;
        return safe.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    public static Range dayBounds(LocalDate date, ZoneId zoneId) {
        if (date == null || zoneId == null) {
            throw new IllegalArgumentException("日期和时区不能为空");
        }
        long from = date.atStartOfDay(zoneId).toInstant().toEpochMilli();
        long to = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli();
        return new Range(from, to);
    }

    public static Range monthBounds(YearMonth month, ZoneId zoneId) {
        if (month == null || zoneId == null) {
            throw new IllegalArgumentException("月份和时区不能为空");
        }
        LocalDate first = month.atDay(1);
        long from = first.atStartOfDay(zoneId).toInstant().toEpochMilli();
        long to = first.plusMonths(1).atStartOfDay(zoneId).toInstant().toEpochMilli();
        return new Range(from, to);
    }

    public static Range previousMonthBounds(YearMonth month, ZoneId zoneId) {
        if (month == null) {
            throw new IllegalArgumentException("月份不能为空");
        }
        return monthBounds(month.minusMonths(1), zoneId);
    }

    public static Range yearBounds(int year, ZoneId zoneId) {
        if (zoneId == null) {
            throw new IllegalArgumentException("时区不能为空");
        }
        LocalDate first = LocalDate.of(year, 1, 1);
        long from = first.atStartOfDay(zoneId).toInstant().toEpochMilli();
        long to = first.plusYears(1).atStartOfDay(zoneId).toInstant().toEpochMilli();
        return new Range(from, to);
    }

    public static List<LocalDate> monthGrid(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("月份不能为空");
        }
        LocalDate first = month.atDay(1);
        LocalDate gridStart = first.minusDays(first.getDayOfWeek().getValue() - 1L);
        List<LocalDate> dates = new ArrayList<>(42);
        for (int index = 0; index < 42; index++) {
            dates.add(gridStart.plusDays(index));
        }
        return Collections.unmodifiableList(dates);
    }

    public static String compactAmount(long cents) {
        long absolute = cents == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(cents);
        if (absolute >= 1_000_000L) {
            BigDecimal wan = BigDecimal.valueOf(cents)
                    .divide(BigDecimal.valueOf(1_000_000L), 1, RoundingMode.HALF_UP)
                    .stripTrailingZeros();
            return wan.toPlainString() + "万";
        }
        return BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString();
    }
}
