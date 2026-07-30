package com.ledgerbook.lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class LedgerDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "ledgerbook_lite.db";
    private static final int DB_VERSION = 1;

    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_INCOME = "INCOME";
    public static final String TYPE_TRANSFER = "TRANSFER";

    public LedgerDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE ledgers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "base_currency TEXT NOT NULL DEFAULT 'CNY'," +
                "created_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ledger_id INTEGER NOT NULL," +
                "name TEXT NOT NULL," +
                "type TEXT NOT NULL," +
                "group_name TEXT NOT NULL DEFAULT ''," +
                "balance_cents INTEGER NOT NULL DEFAULT 0," +
                "note TEXT NOT NULL DEFAULT ''," +
                "created_at INTEGER NOT NULL," +
                "FOREIGN KEY(ledger_id) REFERENCES ledgers(id))");
        db.execSQL("CREATE INDEX idx_accounts_ledger ON accounts(ledger_id)");

        db.execSQL("CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ledger_id INTEGER NOT NULL," +
                "type TEXT NOT NULL," +
                "category TEXT NOT NULL," +
                "amount_cents INTEGER NOT NULL," +
                "discount_cents INTEGER NOT NULL DEFAULT 0," +
                "account_id INTEGER NOT NULL," +
                "to_account_id INTEGER," +
                "bookkeeper TEXT NOT NULL DEFAULT '本人'," +
                "tags TEXT NOT NULL DEFAULT ''," +
                "reimbursable INTEGER NOT NULL DEFAULT 0," +
                "include_budget INTEGER NOT NULL DEFAULT 1," +
                "note TEXT NOT NULL DEFAULT ''," +
                "occurred_at INTEGER NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "FOREIGN KEY(ledger_id) REFERENCES ledgers(id)," +
                "FOREIGN KEY(account_id) REFERENCES accounts(id)," +
                "FOREIGN KEY(to_account_id) REFERENCES accounts(id))");
        db.execSQL("CREATE INDEX idx_transactions_ledger_time ON transactions(ledger_id, occurred_at DESC)");
        db.execSQL("CREATE INDEX idx_transactions_category ON transactions(ledger_id, category)");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the first installable release.
    }

    public void ensureDefaults() {
        SQLiteDatabase db = getWritableDatabase();
        long count = queryLong(db, "SELECT COUNT(*) FROM ledgers", null);
        if (count > 0) {
            return;
        }
        db.beginTransaction();
        try {
            ContentValues ledger = new ContentValues();
            ledger.put("name", "日常账本");
            ledger.put("base_currency", "CNY");
            ledger.put("created_at", System.currentTimeMillis());
            long ledgerId = db.insertOrThrow("ledgers", null, ledger);

            ContentValues account = new ContentValues();
            account.put("ledger_id", ledgerId);
            account.put("name", "现金");
            account.put("type", "现金账户");
            account.put("group_name", "常用账户");
            account.put("balance_cents", 0L);
            account.put("note", "默认现金账户");
            account.put("created_at", System.currentTimeMillis());
            db.insertOrThrow("accounts", null, account);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Ledger> getLedgers() {
        List<Ledger> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, name, base_currency FROM ledgers ORDER BY id", null)) {
            while (cursor.moveToNext()) {
                result.add(new Ledger(cursor.getLong(0), cursor.getString(1), cursor.getString(2)));
            }
        }
        return result;
    }

    public long addLedger(String name, String currency) {
        String cleanName = requireText(name, "账本名称不能为空");
        String cleanCurrency = requireText(currency, "币种不能为空");
        ContentValues values = new ContentValues();
        values.put("name", cleanName);
        values.put("base_currency", cleanCurrency);
        values.put("created_at", System.currentTimeMillis());
        long ledgerId = getWritableDatabase().insertOrThrow("ledgers", null, values);
        addAccount(ledgerId, "现金", "现金账户", "常用账户", 0L, "默认现金账户");
        return ledgerId;
    }

    public List<Account> getAccounts(long ledgerId) {
        List<Account> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, ledger_id, name, type, group_name, balance_cents, note " +
                        "FROM accounts WHERE ledger_id=? ORDER BY group_name, id",
                new String[]{String.valueOf(ledgerId)})) {
            while (cursor.moveToNext()) {
                result.add(new Account(
                        cursor.getLong(0), cursor.getLong(1), cursor.getString(2), cursor.getString(3),
                        cursor.getString(4), cursor.getLong(5), cursor.getString(6)));
            }
        }
        return result;
    }

    public long addAccount(long ledgerId, String name, String type, String groupName,
                           long balanceCents, String note) {
        requireLedger(ledgerId);
        String cleanName = requireText(name, "账户名称不能为空");
        String cleanType = requireText(type, "账户类型不能为空");
        long storedBalance = "信用卡账户".equals(cleanType) && balanceCents > 0
                ? -balanceCents : balanceCents;
        ContentValues values = new ContentValues();
        values.put("ledger_id", ledgerId);
        values.put("name", cleanName);
        values.put("type", cleanType);
        values.put("group_name", safe(groupName));
        values.put("balance_cents", storedBalance);
        values.put("note", safe(note));
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("accounts", null, values);
    }

    public long addTransaction(long ledgerId, String type, String category, long amountCents,
                               long accountId, Long toAccountId, String bookkeeper, String tags,
                               boolean reimbursable, long discountCents, boolean includeBudget,
                               String note, long occurredAt) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }
        if (discountCents < 0) {
            throw new IllegalArgumentException("优惠金额不能为负数");
        }
        if (!TYPE_EXPENSE.equals(type) && !TYPE_INCOME.equals(type) && !TYPE_TRANSFER.equals(type)) {
            throw new IllegalArgumentException("不支持的账单类型");
        }
        requireAccountInLedger(accountId, ledgerId);
        if (TYPE_TRANSFER.equals(type)) {
            if (toAccountId == null) {
                throw new IllegalArgumentException("转账必须选择转入账户");
            }
            requireAccountInLedger(toAccountId, ledgerId);
            if (accountId == toAccountId) {
                throw new IllegalArgumentException("转出账户和转入账户不能相同");
            }
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (TYPE_EXPENSE.equals(type)) {
                updateBalance(db, accountId, -amountCents);
            } else if (TYPE_INCOME.equals(type)) {
                updateBalance(db, accountId, amountCents);
            } else {
                updateBalance(db, accountId, -amountCents);
                updateBalance(db, toAccountId, amountCents);
            }

            ContentValues values = new ContentValues();
            values.put("ledger_id", ledgerId);
            values.put("type", type);
            values.put("category", requireText(category, "分类不能为空"));
            values.put("amount_cents", amountCents);
            values.put("discount_cents", discountCents);
            values.put("account_id", accountId);
            if (toAccountId == null) {
                values.putNull("to_account_id");
            } else {
                values.put("to_account_id", toAccountId);
            }
            values.put("bookkeeper", emptyToDefault(bookkeeper, "本人"));
            values.put("tags", safe(tags));
            values.put("reimbursable", reimbursable ? 1 : 0);
            values.put("include_budget", includeBudget ? 1 : 0);
            values.put("note", safe(note));
            values.put("occurred_at", occurredAt);
            values.put("created_at", System.currentTimeMillis());
            long id = db.insertOrThrow("transactions", null, values);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public void deleteTransaction(long transactionId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.rawQuery(
                "SELECT type, amount_cents, account_id, to_account_id FROM transactions WHERE id=?",
                new String[]{String.valueOf(transactionId)})) {
            if (!cursor.moveToFirst()) {
                throw new IllegalArgumentException("账单不存在");
            }
            String type = cursor.getString(0);
            long amount = cursor.getLong(1);
            long accountId = cursor.getLong(2);
            Long toAccountId = cursor.isNull(3) ? null : cursor.getLong(3);
            if (TYPE_EXPENSE.equals(type)) {
                updateBalance(db, accountId, amount);
            } else if (TYPE_INCOME.equals(type)) {
                updateBalance(db, accountId, -amount);
            } else if (TYPE_TRANSFER.equals(type)) {
                updateBalance(db, accountId, amount);
                if (toAccountId != null) {
                    updateBalance(db, toAccountId, -amount);
                }
            }
            db.delete("transactions", "id=?", new String[]{String.valueOf(transactionId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Txn> getRecentTransactions(long ledgerId, int limit) {
        List<Txn> result = new ArrayList<>();
        String sql = "SELECT t.id, t.type, t.category, t.amount_cents, t.discount_cents, " +
                "t.account_id, a.name, t.to_account_id, b.name, t.bookkeeper, t.tags, " +
                "t.reimbursable, t.include_budget, t.note, t.occurred_at " +
                "FROM transactions t " +
                "JOIN accounts a ON a.id=t.account_id " +
                "LEFT JOIN accounts b ON b.id=t.to_account_id " +
                "WHERE t.ledger_id=? ORDER BY t.occurred_at DESC, t.id DESC LIMIT ?";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql,
                new String[]{String.valueOf(ledgerId), String.valueOf(limit)})) {
            while (cursor.moveToNext()) {
                result.add(new Txn(
                        cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3),
                        cursor.getLong(4), cursor.getLong(5), cursor.getString(6),
                        cursor.isNull(7) ? null : cursor.getLong(7), cursor.getString(8),
                        cursor.getString(9), cursor.getString(10), cursor.getInt(11) == 1,
                        cursor.getInt(12) == 1, cursor.getString(13), cursor.getLong(14)));
            }
        }
        return result;
    }

    public Summary getCurrentMonthSummary(long ledgerId) {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        SQLiteDatabase db = getReadableDatabase();
        long income = sumType(db, ledgerId, TYPE_INCOME, start.getTimeInMillis(), end.getTimeInMillis());
        long expense = sumType(db, ledgerId, TYPE_EXPENSE, start.getTimeInMillis(), end.getTimeInMillis());
        return new Summary(income, expense);
    }

    public List<CategoryTotal> getCurrentMonthExpenseCategories(long ledgerId) {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        List<CategoryTotal> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT category, SUM(amount_cents) total FROM transactions " +
                        "WHERE ledger_id=? AND type=? AND occurred_at>=? AND occurred_at<? " +
                        "GROUP BY category ORDER BY total DESC",
                new String[]{String.valueOf(ledgerId), TYPE_EXPENSE,
                        String.valueOf(start.getTimeInMillis()), String.valueOf(end.getTimeInMillis())})) {
            while (cursor.moveToNext()) {
                result.add(new CategoryTotal(cursor.getString(0), cursor.getLong(1)));
            }
        }
        return result;
    }

    public long getNetAssets(long ledgerId) {
        return queryLong(getReadableDatabase(),
                "SELECT COALESCE(SUM(balance_cents),0) FROM accounts WHERE ledger_id=?",
                new String[]{String.valueOf(ledgerId)});
    }

    private long sumType(SQLiteDatabase db, long ledgerId, String type, long from, long to) {
        return queryLong(db,
                "SELECT COALESCE(SUM(amount_cents),0) FROM transactions " +
                        "WHERE ledger_id=? AND type=? AND occurred_at>=? AND occurred_at<?",
                new String[]{String.valueOf(ledgerId), type, String.valueOf(from), String.valueOf(to)});
    }

    private void updateBalance(SQLiteDatabase db, long accountId, long delta) {
        db.execSQL("UPDATE accounts SET balance_cents=balance_cents+? WHERE id=?",
                new Object[]{delta, accountId});
        if (queryLong(db, "SELECT changes()", null) != 1L) {
            throw new IllegalStateException("账户余额更新失败");
        }
    }

    private void requireLedger(long ledgerId) {
        long count = queryLong(getReadableDatabase(),
                "SELECT COUNT(*) FROM ledgers WHERE id=?", new String[]{String.valueOf(ledgerId)});
        if (count != 1) {
            throw new IllegalArgumentException("账本不存在");
        }
    }

    private void requireAccountInLedger(long accountId, long ledgerId) {
        long count = queryLong(getReadableDatabase(),
                "SELECT COUNT(*) FROM accounts WHERE id=? AND ledger_id=?",
                new String[]{String.valueOf(accountId), String.valueOf(ledgerId)});
        if (count != 1) {
            throw new IllegalArgumentException("账户不属于当前账本");
        }
    }

    private static long queryLong(SQLiteDatabase db, String sql, String[] args) {
        try (Cursor cursor = db.rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0L;
        }
    }

    private static String requireText(String value, String message) {
        String clean = safe(value).trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return clean;
    }

    private static String emptyToDefault(String value, String defaultValue) {
        String clean = safe(value).trim();
        return clean.isEmpty() ? defaultValue : clean;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Ledger {
        public final long id;
        public final String name;
        public final String currency;

        public Ledger(long id, String name, String currency) {
            this.id = id;
            this.name = name;
            this.currency = currency;
        }

        @Override
        public String toString() {
            return name + " · " + currency;
        }
    }

    public static final class Account {
        public final long id;
        public final long ledgerId;
        public final String name;
        public final String type;
        public final String groupName;
        public final long balanceCents;
        public final String note;

        public Account(long id, long ledgerId, String name, String type, String groupName,
                       long balanceCents, String note) {
            this.id = id;
            this.ledgerId = ledgerId;
            this.name = name;
            this.type = type;
            this.groupName = groupName;
            this.balanceCents = balanceCents;
            this.note = note;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final class Txn {
        public final long id;
        public final String type;
        public final String category;
        public final long amountCents;
        public final long discountCents;
        public final long accountId;
        public final String accountName;
        public final Long toAccountId;
        public final String toAccountName;
        public final String bookkeeper;
        public final String tags;
        public final boolean reimbursable;
        public final boolean includeBudget;
        public final String note;
        public final long occurredAt;

        public Txn(long id, String type, String category, long amountCents, long discountCents,
                   long accountId, String accountName, Long toAccountId, String toAccountName,
                   String bookkeeper, String tags, boolean reimbursable, boolean includeBudget,
                   String note, long occurredAt) {
            this.id = id;
            this.type = type;
            this.category = category;
            this.amountCents = amountCents;
            this.discountCents = discountCents;
            this.accountId = accountId;
            this.accountName = accountName;
            this.toAccountId = toAccountId;
            this.toAccountName = toAccountName;
            this.bookkeeper = bookkeeper;
            this.tags = tags;
            this.reimbursable = reimbursable;
            this.includeBudget = includeBudget;
            this.note = note;
            this.occurredAt = occurredAt;
        }
    }

    public static final class Summary {
        public final long incomeCents;
        public final long expenseCents;

        public Summary(long incomeCents, long expenseCents) {
            this.incomeCents = incomeCents;
            this.expenseCents = expenseCents;
        }
    }

    public static final class CategoryTotal {
        public final String category;
        public final long totalCents;

        public CategoryTotal(String category, long totalCents) {
            this.category = category;
            this.totalCents = totalCents;
        }
    }
}
