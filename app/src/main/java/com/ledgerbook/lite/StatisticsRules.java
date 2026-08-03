package com.ledgerbook.lite;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure monthly trend, comparison, and category-ranking rules. */
public final class StatisticsRules {
    public enum Direction {
        UP,
        DOWN,
        FLAT,
        NEW
    }

    public static final class MonthTotals {
        public final String monthKey;
        public final long incomeCents;
        public final long expenseCents;

        public MonthTotals(String monthKey, long incomeCents, long expenseCents) {
            this.monthKey = clean(monthKey);
            this.incomeCents = Math.max(0L, incomeCents);
            this.expenseCents = Math.max(0L, expenseCents);
        }

        public long balanceCents() {
            return incomeCents - expenseCents;
        }
    }

    public static final class Comparison {
        public final long currentCents;
        public final long previousCents;
        public final long deltaCents;
        public final int percentChange;
        public final boolean hasPercentage;
        public final Direction direction;

        Comparison(long currentCents, long previousCents, long deltaCents,
                   int percentChange, boolean hasPercentage, Direction direction) {
            this.currentCents = currentCents;
            this.previousCents = previousCents;
            this.deltaCents = deltaCents;
            this.percentChange = percentChange;
            this.hasPercentage = hasPercentage;
            this.direction = direction;
        }
    }

    public static final class CategoryAmount {
        public final String category;
        public final long totalCents;

        public CategoryAmount(String category, long totalCents) {
            this.category = clean(category);
            this.totalCents = totalCents;
        }
    }

    public static final class CategoryShare {
        public final String category;
        public final long totalCents;
        public final int percent;

        CategoryShare(String category, long totalCents, int percent) {
            this.category = category;
            this.totalCents = totalCents;
            this.percent = percent;
        }
    }

    private StatisticsRules() {
    }

    public static List<MonthTotals> completeTrend(YearMonth endMonth, int months,
                                                   List<MonthTotals> raw) {
        if (endMonth == null) throw new IllegalArgumentException("end month is required");
        if (months <= 0 || months > 24) {
            throw new IllegalArgumentException("months must be between 1 and 24");
        }

        Map<String, long[]> indexed = new LinkedHashMap<>();
        if (raw != null) {
            for (MonthTotals value : raw) {
                if (value == null || value.monthKey.isEmpty()) continue;
                try {
                    YearMonth.parse(value.monthKey);
                } catch (DateTimeParseException ignored) {
                    continue;
                }
                long[] totals = indexed.computeIfAbsent(value.monthKey, key -> new long[2]);
                totals[0] = safeAdd(totals[0], value.incomeCents);
                totals[1] = safeAdd(totals[1], value.expenseCents);
            }
        }

        List<MonthTotals> result = new ArrayList<>(months);
        YearMonth first = endMonth.minusMonths(months - 1L);
        for (int index = 0; index < months; index++) {
            String key = first.plusMonths(index).toString();
            long[] totals = indexed.get(key);
            result.add(totals == null
                    ? new MonthTotals(key, 0L, 0L)
                    : new MonthTotals(key, totals[0], totals[1]));
        }
        return result;
    }

    public static Comparison compare(long currentCents, long previousCents) {
        long current = Math.max(0L, currentCents);
        long previous = Math.max(0L, previousCents);
        long delta = current - previous;
        if (previous == 0L && current > 0L) {
            return new Comparison(current, previous, delta, 0, false, Direction.NEW);
        }
        int percent = previous == 0L ? 0
                : clampInt(Math.round(delta * 100d / previous));
        Direction direction = delta > 0L ? Direction.UP
                : delta < 0L ? Direction.DOWN : Direction.FLAT;
        return new Comparison(current, previous, delta, percent, true, direction);
    }

    public static List<CategoryShare> rankCategories(List<CategoryAmount> values, int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        Map<String, Long> totals = new LinkedHashMap<>();
        if (values != null) {
            for (CategoryAmount value : values) {
                if (value == null || value.category.isEmpty() || value.totalCents <= 0L) continue;
                long previous = totals.getOrDefault(value.category, 0L);
                totals.put(value.category, safeAdd(previous, value.totalCents));
            }
        }

        List<CategoryAmount> sorted = new ArrayList<>();
        long grandTotal = 0L;
        for (Map.Entry<String, Long> entry : totals.entrySet()) {
            sorted.add(new CategoryAmount(entry.getKey(), entry.getValue()));
            grandTotal = safeAdd(grandTotal, entry.getValue());
        }
        sorted.sort(Comparator.comparingLong((CategoryAmount value) -> value.totalCents)
                .reversed().thenComparing(value -> value.category));
        if (grandTotal <= 0L) return new ArrayList<>();

        List<CategoryShare> result = new ArrayList<>();
        long other = 0L;
        for (int index = 0; index < sorted.size(); index++) {
            CategoryAmount value = sorted.get(index);
            if (index < limit) {
                result.add(new CategoryShare(value.category, value.totalCents,
                        percent(value.totalCents, grandTotal)));
            } else {
                other = safeAdd(other, value.totalCents);
            }
        }
        if (other > 0L) {
            result.add(new CategoryShare("其他", other, percent(other, grandTotal)));
        }
        return result;
    }

    private static int percent(long value, long total) {
        return total <= 0L ? 0 : Math.max(0, Math.min(100,
                clampInt(Math.round(value * 100d / total))));
    }

    private static int clampInt(long value) {
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) value;
    }

    private static long safeAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
