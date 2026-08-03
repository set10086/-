package com.ledgerbook.lite;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Persists reusable transaction templates per ledger. */
public final class TemplateRepository {
    public interface Backend {
        List<Template> load(long ledgerId);
        long insert(Template value, long now);
        void delete(long id);
    }

    public static final class Template {
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
        public final int sortOrder;

        public Template(long id, long ledgerId, String name, String type,
                        String category, long amountCents, long discountCents,
                        long accountId, Long toAccountId, String bookkeeper,
                        String tags, boolean reimbursable, boolean includeBudget,
                        String note, int sortOrder) {
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
            this.sortOrder = sortOrder;
        }
    }

    private final Backend backend;

    public TemplateRepository(Backend backend) {
        if (backend == null) throw new IllegalArgumentException("backend cannot be null");
        this.backend = backend;
    }

    public TemplateRepository(LedgerDb db) {
        this(new AndroidBackend(db));
    }

    public List<Template> list(long ledgerId) {
        requireLedger(ledgerId);
        List<Template> values = new ArrayList<>(backend.load(ledgerId));
        values.sort(Comparator.comparingInt((Template value) -> value.sortOrder)
                .thenComparing(value -> value.name));
        return values;
    }

    public long saveFromTransaction(long ledgerId, String name, LedgerDb.Txn transaction) {
        requireLedger(ledgerId);
        if (transaction == null) throw new IllegalArgumentException("没有可保存的账单");
        String cleanName = requireText(name, "模板名称不能为空");
        List<Template> existing = list(ledgerId);
        int order = existing.isEmpty() ? 0
                : existing.get(existing.size() - 1).sortOrder + 10;
        Template template = new Template(-1L, ledgerId, cleanName,
                transaction.type, transaction.category, transaction.amountCents,
                transaction.discountCents, transaction.accountId,
                transaction.toAccountId, transaction.bookkeeper, transaction.tags,
                transaction.reimbursable, transaction.includeBudget,
                transaction.note, order);
        validate(template);
        return backend.insert(template, System.currentTimeMillis());
    }

    public void delete(long id) {
        if (id <= 0L) throw new IllegalArgumentException("模板无效");
        backend.delete(id);
    }

    private static void validate(Template value) {
        requireLedger(value.ledgerId);
        requireText(value.name, "模板名称不能为空");
        requireText(value.category, "模板分类不能为空");
        if (!LedgerDb.TYPE_EXPENSE.equals(value.type)
                && !LedgerDb.TYPE_INCOME.equals(value.type)
                && !LedgerDb.TYPE_TRANSFER.equals(value.type)) {
            throw new IllegalArgumentException("模板账单类型无效");
        }
        if (value.amountCents <= 0L) throw new IllegalArgumentException("模板金额必须大于 0");
        if (value.discountCents < 0L) throw new IllegalArgumentException("模板优惠不能为负数");
        if (value.accountId <= 0L) throw new IllegalArgumentException("模板账户无效");
        if (LedgerDb.TYPE_TRANSFER.equals(value.type) && value.toAccountId == null) {
            throw new IllegalArgumentException("转账模板必须有转入账户");
        }
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

        @Override public List<Template> load(long ledgerId) {
            List<Template> result = new ArrayList<>();
            try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                    "SELECT id,ledger_id,name,type,category,amount_cents,discount_cents,"
                            + "account_id,to_account_id,bookkeeper,tags,reimbursable,"
                            + "include_budget,note,sort_order FROM transaction_templates "
                            + "WHERE ledger_id=? ORDER BY sort_order,name",
                    new String[]{String.valueOf(ledgerId)})) {
                while (cursor.moveToNext()) {
                    result.add(new Template(cursor.getLong(0), cursor.getLong(1),
                            cursor.getString(2), cursor.getString(3), cursor.getString(4),
                            cursor.getLong(5), cursor.getLong(6), cursor.getLong(7),
                            cursor.isNull(8) ? null : cursor.getLong(8), cursor.getString(9),
                            cursor.getString(10), cursor.getInt(11) == 1,
                            cursor.getInt(12) == 1, cursor.getString(13), cursor.getInt(14)));
                }
            }
            return result;
        }

        @Override public long insert(Template value, long now) {
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
            values.put("sort_order", value.sortOrder);
            values.put("created_at", now);
            values.put("updated_at", now);
            long id = helper.getWritableDatabase().insertOrThrow(
                    "transaction_templates", null, values);
            if (id <= 0L) throw new IllegalStateException("模板保存失败");
            return id;
        }

        @Override public void delete(long id) {
            helper.getWritableDatabase().delete(
                    "transaction_templates", "id=?", new String[]{String.valueOf(id)});
        }
    }
}
