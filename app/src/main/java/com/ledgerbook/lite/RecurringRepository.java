package com.ledgerbook.lite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Persists recurrence rules and materializes idempotent, user-confirmed pending items. */
public final class RecurringRepository {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_POSTED = "POSTED";
    public static final String STATUS_SKIPPED = "SKIPPED";
    private static final int MAX_MATERIALIZE_PER_RUN = 512;

    public interface Backend {
        List<Rule> loadRules(long ledgerId);
        long insertRule(Rule value, long now);
        void setEnabled(long id, boolean enabled, long now);
        void deleteRule(long id);
        int nextOccurrenceIndex(long ruleId);
        boolean insertPending(long ruleId, int occurrenceIndex,
                              LocalDate dueDate, long now);
        List<Pending> loadPending(long ledgerId);
        void resolvePending(long id, String status, long transactionId, long now);
    }

    public static final class Rule {
        public final long id;
        public final long ledgerId;
        public final String name;
        public final String type;
        public final String category;
        public final long amountCents;
        public final long discountCents;
        public final long accountId;
        public final Long toAccountId;
        public final String bookkeeper;
        public final String tags;
        public final boolean reimbursable;
        public final boolean includeBudget;
        public final String note;
        public final RecurringRules.Frequency frequency;
        public final int interval;
        public final LocalDate startDate;
        public final LocalDate endDate;
        public final int maxOccurrences;
        public final boolean enabled;
        public final int sortOrder;

        public Rule(long id, long ledgerId, String name, String type,
                    String category, long amountCents, long discountCents,
                    long accountId, Long toAccountId, String bookkeeper,
                    String tags, boolean reimbursable, boolean includeBudget,
                    String note, RecurringRules.Frequency frequency,
                    int interval, LocalDate startDate, LocalDate endDate,
                    int maxOccurrences, boolean enabled, int sortOrder) {
            this.id = id;
            this.ledgerId = ledgerId;
            this.name = clean(name);
            this.type = clean(type);
            this.category = clean(category);
            this.amountCents = amountCents;
            this.discountCents = discountCents;
            this.accountId = accountId;
            this.toAccountId = toAccountId;
            this.bookkeeper = defaultText(bookkeeper, "本人");
            this.tags = clean(tags);
            this.reimbursable = reimbursable;
            this.includeBudget = includeBudget;
            this.note = clean(note);
            this.frequency = frequency;
            this.interval = interval;
            this.startDate = startDate;
            this.endDate = endDate;
            this.maxOccurrences = maxOccurrences;
            this.enabled = enabled;
            this.sortOrder = sortOrder;
        }

        public Rule withId(long value) {
            return new Rule(value, ledgerId, name, type, category,
                    amountCents, discountCents, accountId, toAccountId,
                    bookkeeper, tags, reimbursable, includeBudget, note,
                    frequency, interval, startDate, endDate,
                    maxOccurrences, enabled, sortOrder);
        }

        public Rule withEnabled(boolean value) {
            return new Rule(id, ledgerId, name, type, category,
                    amountCents, discountCents, accountId, toAccountId,
                    bookkeeper, tags, reimbursable, includeBudget, note,
                    frequency, interval, startDate, endDate,
                    maxOccurrences, value, sortOrder);
        }
    }

    public static final class Pending {
        public final long id;
        public final long ruleId;
        public final int occurrenceIndex;
        public final LocalDate dueDate;
        public final String status;
        public final long transactionId;

        public Pending(long id, long ruleId, int occurrenceIndex,
                       LocalDate dueDate, String status, long transactionId) {
            this.id = id;
            this.ruleId = ruleId;
            this.occurrenceIndex = occurrenceIndex;
            this.dueDate = dueDate;
            this.status = clean(status);
            this.transactionId = transactionId;
        }
    }

    private final Backend backend;

    public RecurringRepository(Backend backend) {
        if (backend == null) throw new IllegalArgumentException("backend cannot be null");
        this.backend = backend;
    }

    public RecurringRepository(LedgerDb db) {
        this(new AndroidBackend(db));
    }

    public List<Rule> listRules(long ledgerId) {
        requireLedger(ledgerId);
        List<Rule> values = new ArrayList<>(backend.loadRules(ledgerId));
        values.sort(Comparator.comparingInt((Rule value) -> value.sortOrder)
                .thenComparing(value -> value.name));
        return values;
    }

    public Rule getRule(long ledgerId, long ruleId) {
        for (Rule rule : listRules(ledgerId)) if (rule.id == ruleId) return rule;
        throw new IllegalArgumentException("周期规则不存在");
    }

    public long createFromTransaction(long ledgerId, String name,
                                      LedgerDb.Txn transaction,
                                      RecurringRules.Frequency frequency,
                                      int interval, LocalDate startDate,
                                      LocalDate endDate, int maxOccurrences) {
        requireLedger(ledgerId);
        if (transaction == null) throw new IllegalArgumentException("没有可用账单");
        String cleanName = requireText(name, "周期名称不能为空");
        if (frequency == null) throw new IllegalArgumentException("请选择周期");
        if (interval <= 0) throw new IllegalArgumentException("周期间隔必须大于 0");
        if (startDate == null) throw new IllegalArgumentException("开始日期不能为空");
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        if (maxOccurrences < 0) throw new IllegalArgumentException("执行次数不能为负数");
        List<Rule> existing = listRules(ledgerId);
        int order = existing.isEmpty() ? 0
                : existing.get(existing.size() - 1).sortOrder + 10;
        Rule rule = new Rule(-1L, ledgerId, cleanName,
                transaction.type, transaction.category, transaction.amountCents,
                transaction.discountCents, transaction.accountId,
                transaction.toAccountId, transaction.bookkeeper, transaction.tags,
                transaction.reimbursable, transaction.includeBudget,
                transaction.note, frequency, interval, startDate, endDate,
                maxOccurrences, true, order);
        validate(rule);
        return backend.insertRule(rule, System.currentTimeMillis());
    }

    public void setEnabled(long id, boolean enabled) {
        if (id <= 0L) throw new IllegalArgumentException("周期规则无效");
        backend.setEnabled(id, enabled, System.currentTimeMillis());
    }

    public void deleteRule(long id) {
        if (id <= 0L) throw new IllegalArgumentException("周期规则无效");
        backend.deleteRule(id);
    }

    /** Creates due PENDING rows only. No transaction or account balance is changed here. */
    public int materializeDue(long ledgerId, LocalDate today) {
        requireLedger(ledgerId);
        if (today == null) throw new IllegalArgumentException("当前日期不能为空");
        int created = 0;
        int work = 0;
        for (Rule rule : listRules(ledgerId)) {
            if (!rule.enabled) continue;
            int index = backend.nextOccurrenceIndex(rule.id);
            while (work < MAX_MATERIALIZE_PER_RUN) {
                LocalDate due = RecurringRules.dueDate(
                        rule.startDate, rule.frequency, rule.interval, index);
                if (!RecurringRules.isOccurrenceAllowed(rule, index, due)
                        || due.isAfter(today)) {
                    break;
                }
                if (backend.insertPending(rule.id, index, due,
                        System.currentTimeMillis())) {
                    created++;
                }
                index++;
                work++;
            }
            if (work >= MAX_MATERIALIZE_PER_RUN) break;
        }
        return created;
    }

    public List<Pending> listPending(long ledgerId) {
        requireLedger(ledgerId);
        List<Pending> values = new ArrayList<>(backend.loadPending(ledgerId));
        values.sort(Comparator.comparing((Pending value) -> value.dueDate)
                .thenComparingLong(value -> value.id));
        return values;
    }

    public void markPosted(long pendingId, long transactionId) {
        if (pendingId <= 0L) throw new IllegalArgumentException("待确认项目无效");
        if (transactionId <= 0L) throw new IllegalArgumentException("账单编号无效");
        backend.resolvePending(pendingId, STATUS_POSTED,
                transactionId, System.currentTimeMillis());
    }

    public void skip(long pendingId) {
        if (pendingId <= 0L) throw new IllegalArgumentException("待确认项目无效");
        backend.resolvePending(pendingId, STATUS_SKIPPED,
                0L, System.currentTimeMillis());
    }

    private static void validate(Rule value) {
        requireLedger(value.ledgerId);
        requireText(value.name, "周期名称不能为空");
        requireText(value.category, "周期分类不能为空");
        if (!LedgerDb.TYPE_EXPENSE.equals(value.type)
                && !LedgerDb.TYPE_INCOME.equals(value.type)
                && !LedgerDb.TYPE_TRANSFER.equals(value.type)) {
            throw new IllegalArgumentException("周期账单类型无效");
        }
        if (value.amountCents <= 0L) throw new IllegalArgumentException("周期金额必须大于 0");
        if (value.discountCents < 0L) throw new IllegalArgumentException("周期优惠不能为负数");
        if (value.accountId <= 0L) throw new IllegalArgumentException("周期账户无效");
        if (LedgerDb.TYPE_TRANSFER.equals(value.type) && value.toAccountId == null) {
            throw new IllegalArgumentException("转账周期必须有转入账户");
        }
        RecurringRules.dueDate(value.startDate, value.frequency, value.interval, 0);
    }

    private static void requireLedger(long ledgerId) {
        if (ledgerId <= 0L) throw new IllegalArgumentException("账本无效");
    }

    private static String requireText(String value, String message) {
        String clean = clean(value);
        if (clean.isEmpty()) throw new IllegalArgumentException(message);
        return clean;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String defaultText(String value, String fallback) {
        String clean = clean(value);
        return clean.isEmpty() ? fallback : clean;
    }

    private static final class AndroidBackend implements Backend {
        private final LedgerDb helper;

        AndroidBackend(LedgerDb helper) {
            if (helper == null) throw new IllegalArgumentException("db cannot be null");
            this.helper = helper;
        }

        @Override public List<Rule> loadRules(long ledgerId) {
            List<Rule> result = new ArrayList<>();
            try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                    "SELECT id,ledger_id,name,type,category,amount_cents,discount_cents,"
                            + "account_id,to_account_id,bookkeeper,tags,reimbursable,"
                            + "include_budget,note,frequency,interval_count,start_date,"
                            + "end_date,max_occurrences,enabled,sort_order FROM recurring_rules "
                            + "WHERE ledger_id=? ORDER BY sort_order,name",
                    new String[]{String.valueOf(ledgerId)})) {
                while (cursor.moveToNext()) {
                    result.add(new Rule(cursor.getLong(0), cursor.getLong(1),
                            cursor.getString(2), cursor.getString(3), cursor.getString(4),
                            cursor.getLong(5), cursor.getLong(6), cursor.getLong(7),
                            cursor.isNull(8) ? null : cursor.getLong(8), cursor.getString(9),
                            cursor.getString(10), cursor.getInt(11) == 1,
                            cursor.getInt(12) == 1, cursor.getString(13),
                            RecurringRules.Frequency.valueOf(cursor.getString(14)),
                            cursor.getInt(15), LocalDate.parse(cursor.getString(16)),
                            cursor.isNull(17) ? null : LocalDate.parse(cursor.getString(17)),
                            cursor.getInt(18), cursor.getInt(19) == 1,
                            cursor.getInt(20)));
                }
            }
            return result;
        }

        @Override public long insertRule(Rule value, long now) {
            ContentValues values = new ContentValues();
            values.put("ledger_id", value.ledgerId);
            values.put("name", value.name);
            values.put("type", value.type);
            values.put("category", value.category);
            values.put("amount_cents", value.amountCents);
            values.put("discount_cents", value.discountCents);
            values.put("account_id", value.accountId);
            if (value.toAccountId == null) values.putNull("to_account_id");
            else values.put("to_account_id", value.toAccountId);
            values.put("bookkeeper", value.bookkeeper);
            values.put("tags", value.tags);
            values.put("reimbursable", value.reimbursable ? 1 : 0);
            values.put("include_budget", value.includeBudget ? 1 : 0);
            values.put("note", value.note);
            values.put("frequency", value.frequency.name());
            values.put("interval_count", value.interval);
            values.put("start_date", value.startDate.toString());
            if (value.endDate == null) values.putNull("end_date");
            else values.put("end_date", value.endDate.toString());
            values.put("max_occurrences", value.maxOccurrences);
            values.put("enabled", value.enabled ? 1 : 0);
            values.put("sort_order", value.sortOrder);
            values.put("created_at", now);
            values.put("updated_at", now);
            long id = helper.getWritableDatabase().insertOrThrow(
                    "recurring_rules", null, values);
            if (id <= 0L) throw new IllegalStateException("周期规则保存失败");
            return id;
        }

        @Override public void setEnabled(long id, boolean enabled, long now) {
            ContentValues values = new ContentValues();
            values.put("enabled", enabled ? 1 : 0);
            values.put("updated_at", now);
            int changed = helper.getWritableDatabase().update(
                    "recurring_rules", values, "id=?",
                    new String[]{String.valueOf(id)});
            if (changed != 1) throw new IllegalArgumentException("周期规则不存在");
        }

        @Override public void deleteRule(long id) {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                db.delete("recurring_pending", "rule_id=?",
                        new String[]{String.valueOf(id)});
                if (db.delete("recurring_rules", "id=?",
                        new String[]{String.valueOf(id)}) != 1) {
                    throw new IllegalArgumentException("周期规则不存在");
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        @Override public int nextOccurrenceIndex(long ruleId) {
            try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                    "SELECT COALESCE(MAX(occurrence_index),-1)+1 "
                            + "FROM recurring_pending WHERE rule_id=?",
                    new String[]{String.valueOf(ruleId)})) {
                return cursor.moveToFirst() ? cursor.getInt(0) : 0;
            }
        }

        @Override public boolean insertPending(long ruleId, int occurrenceIndex,
                                               LocalDate dueDate, long now) {
            ContentValues values = new ContentValues();
            values.put("rule_id", ruleId);
            values.put("occurrence_index", occurrenceIndex);
            values.put("due_date", dueDate.toString());
            values.put("status", STATUS_PENDING);
            values.putNull("transaction_id");
            values.put("created_at", now);
            values.putNull("resolved_at");
            return helper.getWritableDatabase().insertWithOnConflict(
                    "recurring_pending", null, values,
                    SQLiteDatabase.CONFLICT_IGNORE) >= 0L;
        }

        @Override public List<Pending> loadPending(long ledgerId) {
            List<Pending> result = new ArrayList<>();
            try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                    "SELECT p.id,p.rule_id,p.occurrence_index,p.due_date,p.status,"
                            + "p.transaction_id FROM recurring_pending p "
                            + "JOIN recurring_rules r ON r.id=p.rule_id "
                            + "WHERE r.ledger_id=? AND p.status=? "
                            + "ORDER BY p.due_date,p.id",
                    new String[]{String.valueOf(ledgerId), STATUS_PENDING})) {
                while (cursor.moveToNext()) {
                    result.add(new Pending(cursor.getLong(0), cursor.getLong(1),
                            cursor.getInt(2), LocalDate.parse(cursor.getString(3)),
                            cursor.getString(4), cursor.isNull(5) ? 0L : cursor.getLong(5)));
                }
            }
            return result;
        }

        @Override public void resolvePending(long id, String status,
                                             long transactionId, long now) {
            ContentValues values = new ContentValues();
            values.put("status", status);
            if (transactionId > 0L) values.put("transaction_id", transactionId);
            else values.putNull("transaction_id");
            values.put("resolved_at", now);
            int changed = helper.getWritableDatabase().update(
                    "recurring_pending", values, "id=? AND status=?",
                    new String[]{String.valueOf(id), STATUS_PENDING});
            if (changed != 1) throw new IllegalArgumentException("待确认项目不存在或已处理");
        }
    }
}
