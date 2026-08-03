package com.ledgerbook.lite;

import android.content.ContentValues;
import android.database.Cursor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Persists subscriptions and exposes normalized monthly/annual totals. */
public final class SubscriptionRepository {
    public interface Backend {
        List<Subscription> load(long ledgerId);
        Subscription get(long id);
        long insert(Subscription value, long now);
        void setActive(long id, boolean active, long now);
        void setNextCharge(long id, LocalDate nextCharge, long now);
        void delete(long id);
    }

    public static final class Subscription {
        public final long id;
        public final long ledgerId;
        public final String name;
        public final long amountCents;
        public final String category;
        public final long accountId;
        public final SubscriptionRules.Frequency frequency;
        public final int interval;
        public final LocalDate nextChargeDate;
        public final int anchorMonth;
        public final int anchorDay;
        public final boolean active;
        public final String note;
        public final int sortOrder;

        public Subscription(long id, long ledgerId, String name, long amountCents,
                            String category, long accountId,
                            SubscriptionRules.Frequency frequency, int interval,
                            LocalDate nextChargeDate, int anchorMonth, int anchorDay,
                            boolean active, String note, int sortOrder) {
            this.id = id; this.ledgerId = ledgerId; this.name = clean(name);
            this.amountCents = amountCents; this.category = clean(category);
            this.accountId = accountId; this.frequency = frequency; this.interval = interval;
            this.nextChargeDate = nextChargeDate; this.anchorMonth = anchorMonth;
            this.anchorDay = anchorDay; this.active = active; this.note = clean(note);
            this.sortOrder = sortOrder;
        }
        public Subscription withId(long value) { return copy(value, nextChargeDate, active); }
        public Subscription withActive(boolean value) { return copy(id, nextChargeDate, value); }
        public Subscription withNextCharge(LocalDate value) { return copy(id, value, active); }
        private Subscription copy(long newId, LocalDate next, boolean enabled) {
            return new Subscription(newId, ledgerId, name, amountCents, category,
                    accountId, frequency, interval, next, anchorMonth, anchorDay,
                    enabled, note, sortOrder);
        }
    }

    public static final class Snapshot {
        public final List<Subscription> values;
        public final long monthlyCents;
        public final long annualCents;
        public final int dueCount;
        public final int upcomingCount;
        Snapshot(List<Subscription> values, long monthlyCents, long annualCents,
                 int dueCount, int upcomingCount) {
            this.values = values; this.monthlyCents = monthlyCents;
            this.annualCents = annualCents; this.dueCount = dueCount;
            this.upcomingCount = upcomingCount;
        }
    }

    private final Backend backend;
    public SubscriptionRepository(Backend backend) {
        if (backend == null) throw new IllegalArgumentException("backend cannot be null");
        this.backend = backend;
    }
    public SubscriptionRepository(LedgerDb db) { this(new AndroidBackend(db)); }

    public List<Subscription> list(long ledgerId) {
        requireLedger(ledgerId);
        List<Subscription> result = new ArrayList<>(backend.load(ledgerId));
        result.sort(Comparator.comparing((Subscription value) -> !value.active)
                .thenComparing(value -> value.nextChargeDate)
                .thenComparingInt(value -> value.sortOrder)
                .thenComparing(value -> value.name));
        return result;
    }

    public long createFromTransaction(long ledgerId, String name, LedgerDb.Txn transaction,
                                      SubscriptionRules.Frequency frequency, int interval,
                                      LocalDate nextChargeDate, String note) {
        requireLedger(ledgerId);
        if (transaction == null) throw new IllegalArgumentException("没有可用账单");
        if (!LedgerDb.TYPE_EXPENSE.equals(transaction.type)) throw new IllegalArgumentException("只有支出账单可设为订阅");
        String cleanName = requireText(name, "订阅名称不能为空");
        if (transaction.amountCents <= 0L) throw new IllegalArgumentException("订阅金额必须大于 0");
        if (frequency == null || interval <= 0 || nextChargeDate == null) throw new IllegalArgumentException("订阅周期无效");
        List<Subscription> existing = list(ledgerId);
        int order = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).sortOrder + 10;
        Subscription value = new Subscription(-1L, ledgerId, cleanName,
                transaction.amountCents, requireText(transaction.category, "订阅分类不能为空"),
                transaction.accountId, frequency, interval, nextChargeDate,
                nextChargeDate.getMonthValue(), nextChargeDate.getDayOfMonth(),
                true, note, order);
        return backend.insert(value, System.currentTimeMillis());
    }

    public Snapshot snapshot(long ledgerId, LocalDate today, int upcomingDays) {
        if (today == null || upcomingDays < 0) throw new IllegalArgumentException("提醒范围无效");
        List<Subscription> values = list(ledgerId);
        long monthly = 0L, annual = 0L; int due = 0, upcoming = 0;
        for (Subscription value : values) {
            SubscriptionRules.Reminder reminder = SubscriptionRules.reminder(
                    value.nextChargeDate, value.active, today, upcomingDays);
            if (!value.active) continue;
            SubscriptionRules.Cost cost = SubscriptionRules.cost(
                    value.amountCents, value.frequency, value.interval);
            monthly = safeAdd(monthly, cost.monthlyCents);
            annual = safeAdd(annual, cost.annualCents);
            if (reminder == SubscriptionRules.Reminder.DUE) due++;
            else if (reminder == SubscriptionRules.Reminder.UPCOMING) upcoming++;
        }
        return new Snapshot(values, monthly, annual, due, upcoming);
    }

    public void advanceAfterPosting(long id) {
        Subscription value = backend.get(requireId(id));
        backend.setNextCharge(id, SubscriptionRules.nextCharge(value), System.currentTimeMillis());
    }
    public void setActive(long id, boolean active) {
        backend.setActive(requireId(id), active, System.currentTimeMillis());
    }
    public void delete(long id) { backend.delete(requireId(id)); }

    private static long requireId(long id) { if (id <= 0L) throw new IllegalArgumentException("订阅无效"); return id; }
    private static void requireLedger(long id) { if (id <= 0L) throw new IllegalArgumentException("账本无效"); }
    private static String requireText(String value, String message) {
        String result = clean(value); if (result.isEmpty()) throw new IllegalArgumentException(message); return result;
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static long safeAdd(long a, long b) { try { return Math.addExact(a, b); } catch (ArithmeticException e) { return Long.MAX_VALUE; } }

    private static final class AndroidBackend implements Backend {
        private final LedgerDb helper;
        AndroidBackend(LedgerDb helper) { if (helper == null) throw new IllegalArgumentException("db cannot be null"); this.helper = helper; }
        @Override public List<Subscription> load(long ledgerId) {
            List<Subscription> result = new ArrayList<>();
            try (Cursor c = helper.getReadableDatabase().rawQuery(
                    "SELECT id,ledger_id,name,amount_cents,category,account_id,frequency,interval_count,next_charge_date,anchor_month,anchor_day,active,note,sort_order FROM subscriptions WHERE ledger_id=? ORDER BY sort_order,name",
                    new String[]{String.valueOf(ledgerId)})) {
                while (c.moveToNext()) result.add(read(c));
            }
            return result;
        }
        @Override public Subscription get(long id) {
            try (Cursor c = helper.getReadableDatabase().rawQuery(
                    "SELECT id,ledger_id,name,amount_cents,category,account_id,frequency,interval_count,next_charge_date,anchor_month,anchor_day,active,note,sort_order FROM subscriptions WHERE id=?",
                    new String[]{String.valueOf(id)})) {
                if (!c.moveToFirst()) throw new IllegalArgumentException("订阅不存在");
                return read(c);
            }
        }
        @Override public long insert(Subscription value, long now) {
            ContentValues v = values(value); v.put("created_at", now); v.put("updated_at", now);
            long id = helper.getWritableDatabase().insertOrThrow("subscriptions", null, v);
            if (id <= 0L) throw new IllegalStateException("订阅保存失败"); return id;
        }
        @Override public void setActive(long id, boolean active, long now) {
            ContentValues v = new ContentValues(); v.put("active", active ? 1 : 0); v.put("updated_at", now);
            if (helper.getWritableDatabase().update("subscriptions", v, "id=?", new String[]{String.valueOf(id)}) != 1) throw new IllegalArgumentException("订阅不存在");
        }
        @Override public void setNextCharge(long id, LocalDate nextCharge, long now) {
            ContentValues v = new ContentValues(); v.put("next_charge_date", nextCharge.toString()); v.put("updated_at", now);
            if (helper.getWritableDatabase().update("subscriptions", v, "id=?", new String[]{String.valueOf(id)}) != 1) throw new IllegalArgumentException("订阅不存在");
        }
        @Override public void delete(long id) {
            if (helper.getWritableDatabase().delete("subscriptions", "id=?", new String[]{String.valueOf(id)}) != 1) throw new IllegalArgumentException("订阅不存在");
        }
        private static Subscription read(Cursor c) {
            return new Subscription(c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3), c.getString(4), c.getLong(5), SubscriptionRules.Frequency.valueOf(c.getString(6)), c.getInt(7), LocalDate.parse(c.getString(8)), c.getInt(9), c.getInt(10), c.getInt(11) == 1, c.getString(12), c.getInt(13));
        }
        private static ContentValues values(Subscription s) {
            ContentValues v = new ContentValues(); v.put("ledger_id", s.ledgerId); v.put("name", s.name); v.put("amount_cents", s.amountCents); v.put("category", s.category); v.put("account_id", s.accountId); v.put("frequency", s.frequency.name()); v.put("interval_count", s.interval); v.put("next_charge_date", s.nextChargeDate.toString()); v.put("anchor_month", s.anchorMonth); v.put("anchor_day", s.anchorDay); v.put("active", s.active ? 1 : 0); v.put("note", s.note); v.put("sort_order", s.sortOrder); return v;
        }
    }
}
