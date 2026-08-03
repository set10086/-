package com.ledgerbook.lite;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Reads six-month statistics through the existing LedgerDb aggregation API. */
public final class StatisticsRepository {
    public static final int TREND_MONTHS = 6;

    public static final class Snapshot {
        public final YearMonth month;
        public final StatisticsRules.MonthTotals current;
        public final StatisticsRules.MonthTotals previous;
        public final StatisticsRules.Comparison incomeComparison;
        public final StatisticsRules.Comparison expenseComparison;
        public final List<StatisticsRules.MonthTotals> trend;
        public final List<StatisticsRules.CategoryShare> categories;
        public final long netAssetsCents;

        Snapshot(YearMonth month,
                 StatisticsRules.MonthTotals current,
                 StatisticsRules.MonthTotals previous,
                 StatisticsRules.Comparison incomeComparison,
                 StatisticsRules.Comparison expenseComparison,
                 List<StatisticsRules.MonthTotals> trend,
                 List<StatisticsRules.CategoryShare> categories,
                 long netAssetsCents) {
            this.month = month;
            this.current = current;
            this.previous = previous;
            this.incomeComparison = incomeComparison;
            this.expenseComparison = expenseComparison;
            this.trend = trend;
            this.categories = categories;
            this.netAssetsCents = netAssetsCents;
        }
    }

    private final LedgerDb db;
    private final ZoneId zoneId;

    public StatisticsRepository(LedgerDb db) {
        this(db, ZoneId.systemDefault());
    }

    StatisticsRepository(LedgerDb db, ZoneId zoneId) {
        if (db == null || zoneId == null) {
            throw new IllegalArgumentException("db and zone are required");
        }
        this.db = db;
        this.zoneId = zoneId;
    }

    public Snapshot load(long ledgerId, YearMonth month) {
        if (ledgerId <= 0L) throw new IllegalArgumentException("账本无效");
        if (month == null) throw new IllegalArgumentException("月份不能为空");

        List<StatisticsRules.MonthTotals> raw = new ArrayList<>();
        YearMonth first = month.minusMonths(TREND_MONTHS - 1L);
        for (int index = 0; index < TREND_MONTHS; index++) {
            YearMonth value = first.plusMonths(index);
            long[] bounds = bounds(value);
            LedgerDb.Summary summary = db.getSummary(ledgerId, bounds[0], bounds[1]);
            raw.add(new StatisticsRules.MonthTotals(value.toString(),
                    summary.incomeCents, summary.expenseCents));
        }
        List<StatisticsRules.MonthTotals> trend =
                StatisticsRules.completeTrend(month, TREND_MONTHS, raw);
        StatisticsRules.MonthTotals current = trend.get(trend.size() - 1);
        StatisticsRules.MonthTotals previous = trend.get(trend.size() - 2);

        long[] currentBounds = bounds(month);
        List<StatisticsRules.CategoryAmount> categoryAmounts = new ArrayList<>();
        for (LedgerDb.CategoryTotal total : db.getExpenseCategories(
                ledgerId, currentBounds[0], currentBounds[1])) {
            categoryAmounts.add(new StatisticsRules.CategoryAmount(
                    total.category, total.totalCents));
        }

        return new Snapshot(month, current, previous,
                StatisticsRules.compare(current.incomeCents, previous.incomeCents),
                StatisticsRules.compare(current.expenseCents, previous.expenseCents),
                trend, StatisticsRules.rankCategories(categoryAmounts, 5),
                db.getNetAssets(ledgerId));
    }

    private long[] bounds(YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.plusMonths(1).atDay(1);
        return new long[]{
                startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                endDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        };
    }
}
