package com.ledgerbook.lite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Persists monthly total/category budgets and calculates their live usage. */
public final class BudgetRepository {
    public interface Backend {
        List<Budget> load(long ledgerId, String monthKey);
        void upsert(long ledgerId, String monthKey, String categoryKey,
                    long amountCents, long now);
        void remove(long ledgerId, String monthKey, String categoryKey);
        long spent(long ledgerId, String monthKey, String categoryKey);
    }

    public static final class Budget {
        public final long id;
        public final long ledgerId;
        public final String monthKey;
        public final String categoryKey;
        public final long amountCents;

        public Budget(long id, long ledgerId, String monthKey,
                      String categoryKey, long amountCents) {
            this.id = id;
            this.ledgerId = ledgerId;
            this.monthKey = monthKey;
            this.categoryKey = categoryKey == null ? "" : categoryKey;
            this.amountCents = amountCents;
        }

        public boolean isTotal() {
            return categoryKey.isEmpty();
        }
    }

    public static final class Progress {
        public final Budget budget;
        public final BudgetRules.Status status;

        Progress(Budget budget, BudgetRules.Status status) {
            this.budget = budget;
            this.status = status;
        }
    }

    public static final class MonthSnapshot {
        public final String monthKey;
        public final Progress total;
        public final List<Progress> categories;

        MonthSnapshot(String monthKey, Progress total, List<Progress> categories) {
            this.monthKey = monthKey;
            this.total = total;
            this.categories = categories;
        }
    }

    private final Backend backend;

    public BudgetRepository(Backend backend) {
        if (backend == null) throw new IllegalArgumentException("backend cannot be null");
        this.backend = backend;
    }

    public BudgetRepository(LedgerDb db) {
        this(new AndroidBackend(db));
    }

    public void setTotalBudget(long ledgerId, String monthKey, long amountCents) {
        upsert(ledgerId, monthKey, "", amountCents);
    }

    public void setCategoryBudget(long ledgerId, String monthKey,
                                  String categoryKey, long amountCents) {
        String category = clean(categoryKey);
        if (category.isEmpty()) throw new IllegalArgumentException("请选择预算分类");
        upsert(ledgerId, monthKey, category, amountCents);
    }

    public void removeTotalBudget(long ledgerId, String monthKey) {
        backend.remove(requireLedger(ledgerId), requireMonth(monthKey), "");
    }

    public void removeCategoryBudget(long ledgerId, String monthKey, String categoryKey) {
        String category = clean(categoryKey);
        if (category.isEmpty()) throw new IllegalArgumentException("预算分类不能为空");
        backend.remove(requireLedger(ledgerId), requireMonth(monthKey), category);
    }

    public MonthSnapshot snapshot(long ledgerId, String monthKey,
                                  int dayOfMonth, int daysInMonth) {
        long ledger = requireLedger(ledgerId);
        String month = requireMonth(monthKey);
        List<Budget> loaded = backend.load(ledger, month);
        Budget totalBudget = null;
        List<Budget> categoryBudgets = new ArrayList<>();
        for (Budget budget : loaded) {
            if (budget == null) continue;
            if (budget.isTotal()) totalBudget = budget;
            else categoryBudgets.add(budget);
        }
        if (totalBudget == null) {
            totalBudget = new Budget(-1L, ledger, month, "", 0L);
        }
        Progress total = progress(totalBudget,
                backend.spent(ledger, month, ""), dayOfMonth, daysInMonth);
        categoryBudgets.sort(Comparator
                .comparingLong((Budget value) -> value.amountCents).reversed()
                .thenComparing(value -> value.categoryKey));
        List<Progress> categories = new ArrayList<>();
        for (Budget budget : categoryBudgets) {
            categories.add(progress(budget,
                    backend.spent(ledger, month, budget.categoryKey),
                    dayOfMonth, daysInMonth));
        }
        return new MonthSnapshot(month, total, categories);
    }

    private void upsert(long ledgerId, String monthKey,
                        String categoryKey, long amountCents) {
        long ledger = requireLedger(ledgerId);
        String month = requireMonth(monthKey);
        if (amountCents <= 0L) throw new IllegalArgumentException("预算金额必须大于 0");
        backend.upsert(ledger, month, categoryKey, amountCents, System.currentTimeMillis());
    }

    private static Progress progress(Budget budget, long spent,
                                     int dayOfMonth, int daysInMonth) {
        return new Progress(budget, BudgetRules.calculate(
                budget.amountCents, spent, dayOfMonth, daysInMonth));
    }

    private static long requireLedger(long ledgerId) {
        if (ledgerId <= 0L) throw new IllegalArgumentException("账本无效");
        return ledgerId;
    }

    public static String requireMonth(String monthKey) {
        String value = clean(monthKey);
        if (!value.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("月份格式必须为 yyyy-MM");
        }
        try {
            YearMonth parsed = YearMonth.parse(value);
            if (!parsed.toString().equals(value)) {
                throw new IllegalArgumentException("月份格式必须为 yyyy-MM");
            }
            return value;
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("月份无效");
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class AndroidBackend implements Backend {
        private final LedgerDb helper;

        AndroidBackend(LedgerDb helper) {
            if (helper == null) throw new IllegalArgumentException("db cannot be null");
            this.helper = helper;
        }

        @Override public List<Budget> load(long ledgerId, String monthKey) {
            List<Budget> result = new ArrayList<>();
            try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                    "SELECT id,ledger_id,month_key,category_key,amount_cents "
                            + "FROM budgets WHERE ledger_id=? AND month_key=? "
                            + "ORDER BY category_key",
                    new String[]{String.valueOf(ledgerId), monthKey})) {
                while (cursor.moveToNext()) {
                    result.add(new Budget(cursor.getLong(0), cursor.getLong(1),
                            cursor.getString(2), cursor.getString(3), cursor.getLong(4)));
                }
            }
            return result;
        }

        @Override public void upsert(long ledgerId, String monthKey, String categoryKey,
                                     long amountCents, long now) {
            ContentValues values = new ContentValues();
            values.put("ledger_id", ledgerId);
            values.put("month_key", monthKey);
            values.put("category_key", categoryKey);
            values.put("amount_cents", amountCents);
            values.put("created_at", now);
            values.put("updated_at", now);
            long result = helper.getWritableDatabase().insertWithOnConflict(
                    "budgets", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            if (result < 0L) throw new IllegalStateException("预算保存失败");
        }

        @Override public void remove(long ledgerId, String monthKey, String categoryKey) {
            helper.getWritableDatabase().delete("budgets",
                    "ledger_id=? AND month_key=? AND category_key=?",
                    new String[]{String.valueOf(ledgerId), monthKey, categoryKey});
        }

        @Override public long spent(long ledgerId, String monthKey, String categoryKey) {
            YearMonth month = YearMonth.parse(monthKey);
            ZoneId zone = ZoneId.systemDefault();
            long from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();
            long to = month.plusMonths(1).atDay(1).atStartOfDay(zone)
                    .toInstant().toEpochMilli();
            StringBuilder sql = new StringBuilder(
                    "SELECT COALESCE(SUM(amount_cents),0) FROM transactions "
                            + "WHERE ledger_id=? AND type=? AND include_budget=1 "
                            + "AND occurred_at>=? AND occurred_at<?");
            List<String> args = new ArrayList<>();
            args.add(String.valueOf(ledgerId));
            args.add(LedgerDb.TYPE_EXPENSE);
            args.add(String.valueOf(from));
            args.add(String.valueOf(to));
            if (!categoryKey.isEmpty()) {
                if (categoryKey.indexOf('/') >= 0) {
                    sql.append(" AND category=?");
                    args.add(categoryKey);
                } else {
                    sql.append(" AND (category=? OR category LIKE ? ESCAPE '\\')");
                    args.add(categoryKey);
                    args.add(escapeLike(categoryKey) + "/%");
                }
            }
            try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                    sql.toString(), args.toArray(new String[0]))) {
                return cursor.moveToFirst() ? cursor.getLong(0) : 0L;
            }
        }

        private static String escapeLike(String value) {
            return value.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
        }
    }
}
