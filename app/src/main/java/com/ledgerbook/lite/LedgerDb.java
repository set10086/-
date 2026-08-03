package com.ledgerbook.lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LedgerDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "ledgerbook_lite.db";
    private static final int DB_VERSION = 1;
    private static final int MAX_QUERY_LIMIT = 5000;

    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_INCOME = "INCOME";
    public static final String TYPE_TRANSFER = "TRANSFER";

    private static final String TXN_VIEW_SELECT =
            "SELECT t.id, t.ledger_id, l.name, l.base_currency, t.type, t.category, " +
                    "t.amount_cents, t.discount_cents, t.account_id, a.name, " +
                    "t.to_account_id, b.name, t.bookkeeper, t.tags, t.reimbursable, " +
                    "t.include_budget, t.note, t.occurred_at " +
                    "FROM transactions t " +
                    "JOIN ledgers l ON l.id=t.ledger_id " +
                    "JOIN accounts a ON a.id=t.account_id " +
                    "LEFT JOIN accounts b ON b.id=t.to_account_id ";

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
        // Version 1 remains compatible with V1.0–V1.3.
    }

    public void ensureDefaults() {
        SQLiteDatabase db = getWritableDatabase();
        long count = queryLong(db, "SELECT COUNT(*) FROM ledgers", null);
        if (count > 0) return;
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

    public Ledger getLedger(long ledgerId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, name, base_currency FROM ledgers WHERE id=?",
                new String[]{String.valueOf(ledgerId)})) {
            return cursor.moveToFirst()
                    ? new Ledger(cursor.getLong(0), cursor.getString(1), cursor.getString(2)) : null;
        }
    }

    public long addLedger(String name, String currency) {
        String cleanName = requireText(name, "账本名称不能为空");
        String cleanCurrency = requireText(currency, "币种不能为空");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("name", cleanName);
            values.put("base_currency", cleanCurrency);
            values.put("created_at", System.currentTimeMillis());
            long ledgerId = db.insertOrThrow("ledgers", null, values);
            insertAccount(db, ledgerId, "现金", "现金账户", "常用账户", 0L, "默认现金账户");
            db.setTransactionSuccessful();
            return ledgerId;
        } finally {
            db.endTransaction();
        }
    }

    public void renameLedger(long ledgerId, String name) {
        requireLedger(ledgerId);
        ContentValues values = new ContentValues();
        values.put("name", requireText(name, "账本名称不能为空"));
        int changed = getWritableDatabase().update(
                "ledgers", values, "id=?", new String[]{String.valueOf(ledgerId)});
        if (changed != 1) throw new IllegalStateException("账本重命名失败");
    }

    public LedgerCounts getLedgerCounts(long ledgerId) {
        requireLedger(ledgerId);
        SQLiteDatabase db = getReadableDatabase();
        long accounts = queryLong(db, "SELECT COUNT(*) FROM accounts WHERE ledger_id=?",
                new String[]{String.valueOf(ledgerId)});
        long transactions = queryLong(db, "SELECT COUNT(*) FROM transactions WHERE ledger_id=?",
                new String[]{String.valueOf(ledgerId)});
        return new LedgerCounts(accounts, transactions);
    }

    public long deleteLedger(long ledgerId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (queryLong(db, "SELECT COUNT(*) FROM ledgers WHERE id=?",
                    new String[]{String.valueOf(ledgerId)}) != 1L) {
                throw new IllegalArgumentException("账本不存在");
            }
            if (queryLong(db, "SELECT COUNT(*) FROM ledgers", null) <= 1L) {
                throw new IllegalArgumentException("至少需要保留一个账本");
            }
            long nextLedgerId = queryLong(db,
                    "SELECT id FROM ledgers WHERE id<>? ORDER BY id LIMIT 1",
                    new String[]{String.valueOf(ledgerId)});
            db.delete("transactions", "ledger_id=?", new String[]{String.valueOf(ledgerId)});
            db.delete("accounts", "ledger_id=?", new String[]{String.valueOf(ledgerId)});
            if (db.delete("ledgers", "id=?", new String[]{String.valueOf(ledgerId)}) != 1) {
                throw new IllegalStateException("账本删除失败");
            }
            db.setTransactionSuccessful();
            return nextLedgerId;
        } finally {
            db.endTransaction();
        }
    }

    public List<Account> getAccounts(long ledgerId) {
        requireLedger(ledgerId);
        return queryAccounts("WHERE ledger_id=? ORDER BY group_name, id",
                new String[]{String.valueOf(ledgerId)});
    }

    public List<Account> getAccountsForScope(Long ledgerId) {
        if (ledgerId == null || ledgerId == TransactionFilter.ALL_LEDGERS) {
            return queryAccounts("ORDER BY ledger_id, group_name, id", null);
        }
        return getAccounts(ledgerId);
    }

    private List<Account> queryAccounts(String suffix, String[] args) {
        List<Account> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, ledger_id, name, type, group_name, balance_cents, note " +
                        "FROM accounts " + suffix, args)) {
            while (cursor.moveToNext()) {
                result.add(new Account(cursor.getLong(0), cursor.getLong(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getLong(5), cursor.getString(6)));
            }
        }
        return result;
    }

    public long addAccount(long ledgerId, String name, String type, String groupName,
                           long balanceCents, String note) {
        requireLedger(ledgerId);
        return insertAccount(getWritableDatabase(), ledgerId, name, type, groupName, balanceCents, note);
    }

    private long insertAccount(SQLiteDatabase db, long ledgerId, String name, String type,
                               String groupName, long balanceCents, String note) {
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
        return db.insertOrThrow("accounts", null, values);
    }

    public long addTransaction(long ledgerId, String type, String category, long amountCents,
                               long accountId, Long toAccountId, String bookkeeper, String tags,
                               boolean reimbursable, long discountCents, boolean includeBudget,
                               String note, long occurredAt) {
        if (amountCents <= 0) throw new IllegalArgumentException("金额必须大于 0");
        if (discountCents < 0) throw new IllegalArgumentException("优惠金额不能为负数");
        if (!TYPE_EXPENSE.equals(type) && !TYPE_INCOME.equals(type) && !TYPE_TRANSFER.equals(type)) {
            throw new IllegalArgumentException("不支持的账单类型");
        }
        requireAccountInLedger(accountId, ledgerId);
        if (TYPE_TRANSFER.equals(type)) {
            if (toAccountId == null) throw new IllegalArgumentException("转账必须选择转入账户");
            requireAccountInLedger(toAccountId, ledgerId);
            if (accountId == toAccountId) {
                throw new IllegalArgumentException("转出账户和转入账户不能相同");
            }
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (TYPE_EXPENSE.equals(type)) updateBalance(db, accountId, -amountCents);
            else if (TYPE_INCOME.equals(type)) updateBalance(db, accountId, amountCents);
            else {
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
            if (toAccountId == null) values.putNull("to_account_id");
            else values.put("to_account_id", toAccountId);
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
            if (!cursor.moveToFirst()) throw new IllegalArgumentException("账单不存在");
            String type = cursor.getString(0);
            long amount = cursor.getLong(1);
            long accountId = cursor.getLong(2);
            Long toAccountId = cursor.isNull(3) ? null : cursor.getLong(3);
            if (TYPE_EXPENSE.equals(type)) updateBalance(db, accountId, amount);
            else if (TYPE_INCOME.equals(type)) updateBalance(db, accountId, -amount);
            else if (TYPE_TRANSFER.equals(type)) {
                updateBalance(db, accountId, amount);
                if (toAccountId != null) updateBalance(db, toAccountId, -amount);
            }
            db.delete("transactions", "id=?", new String[]{String.valueOf(transactionId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Txn> getRecentTransactions(long ledgerId, int limit) {
        validatePaging(0, limit);
        List<Txn> result = new ArrayList<>();
        String sql = "SELECT t.id, t.type, t.category, t.amount_cents, t.discount_cents, " +
                "t.account_id, a.name, t.to_account_id, b.name, t.bookkeeper, t.tags, " +
                "t.reimbursable, t.include_budget, t.note, t.occurred_at " +
                "FROM transactions t JOIN accounts a ON a.id=t.account_id " +
                "LEFT JOIN accounts b ON b.id=t.to_account_id " +
                "WHERE t.ledger_id=? ORDER BY t.occurred_at DESC, t.id DESC LIMIT ?";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql,
                new String[]{String.valueOf(ledgerId), String.valueOf(limit)})) {
            while (cursor.moveToNext()) result.add(readTxn(cursor));
        }
        return result;
    }

    public List<TxnView> searchTransactions(Long ledgerId, String query, int offset, int limit) {
        validatePaging(offset, limit);
        String clean = requireText(query, "请输入搜索内容");
        boolean allLedgers = ledgerId == null || ledgerId == TransactionFilter.ALL_LEDGERS;
        if (!allLedgers) requireLedger(ledgerId);
        String pattern = "%" + TransactionQueryRules.escapeLike(clean) + "%";
        StringBuilder sql = new StringBuilder(TXN_VIEW_SELECT).append("WHERE ");
        List<String> args = new ArrayList<>();
        if (!allLedgers) {
            sql.append("t.ledger_id=? AND ");
            args.add(String.valueOf(ledgerId));
        }
        sql.append("(t.category LIKE ? ESCAPE '\\' OR t.note LIKE ? ESCAPE '\\' " +
                "OR t.tags LIKE ? ESCAPE '\\' OR t.bookkeeper LIKE ? ESCAPE '\\' " +
                "OR a.name LIKE ? ESCAPE '\\' OR COALESCE(b.name,'') LIKE ? ESCAPE '\\'");
        for (int index = 0; index < 6; index++) args.add(pattern);
        if (allLedgers) {
            sql.append(" OR l.name LIKE ? ESCAPE '\\'");
            args.add(pattern);
        }
        sql.append(") ORDER BY t.occurred_at DESC, t.id DESC LIMIT ? OFFSET ?");
        args.add(String.valueOf(limit));
        args.add(String.valueOf(offset));
        return queryTxnViews(sql.toString(), args);
    }

    public List<TxnView> getFilteredTransactions(TransactionFilter filter, int offset, int limit) {
        if (filter == null) throw new IllegalArgumentException("筛选条件不能为空");
        validatePaging(offset, limit);
        StringBuilder sql = new StringBuilder(TXN_VIEW_SELECT)
                .append("WHERE t.occurred_at>=? AND t.occurred_at<?");
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(filter.fromInclusive));
        args.add(String.valueOf(filter.toExclusive));
        if (filter.ledgerId != TransactionFilter.ALL_LEDGERS) {
            requireLedger(filter.ledgerId);
            sql.append(" AND t.ledger_id=?");
            args.add(String.valueOf(filter.ledgerId));
        }
        if (filter.type != null) {
            sql.append(" AND t.type=?");
            args.add(filter.type);
        }
        if (filter.category != null) {
            sql.append(" AND t.category=?");
            args.add(filter.category);
        }
        if (filter.accountId > 0L) {
            sql.append(" AND (t.account_id=? OR t.to_account_id=?)");
            args.add(String.valueOf(filter.accountId));
            args.add(String.valueOf(filter.accountId));
        }
        if (filter.bookkeeper != null) {
            sql.append(" AND t.bookkeeper=?");
            args.add(filter.bookkeeper);
        }
        sql.append(" ORDER BY t.occurred_at DESC, t.id DESC LIMIT ? OFFSET ?");
        args.add(String.valueOf(limit));
        args.add(String.valueOf(offset));
        return queryTxnViews(sql.toString(), args);
    }

    public List<TxnView> getTransactionsForDay(long ledgerId, long from, long to) {
        return getFilteredTransactions(TransactionFilter.forCurrentLedger(ledgerId, from, to),
                0, MAX_QUERY_LIMIT);
    }

    public List<DaySummary> getMonthDaySummaries(long ledgerId, long from, long to) {
        requireLedger(ledgerId);
        Map<Long, long[]> totals = new LinkedHashMap<>();
        ZoneId zone = ZoneId.systemDefault();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT type, amount_cents, occurred_at FROM transactions " +
                        "WHERE ledger_id=? AND occurred_at>=? AND occurred_at<? ORDER BY occurred_at",
                new String[]{String.valueOf(ledgerId), String.valueOf(from), String.valueOf(to)})) {
            while (cursor.moveToNext()) {
                String type = cursor.getString(0);
                long amount = cursor.getLong(1);
                LocalDate date = Instant.ofEpochMilli(cursor.getLong(2)).atZone(zone).toLocalDate();
                long[] values = totals.get(date.toEpochDay());
                if (values == null) {
                    values = new long[3];
                    totals.put(date.toEpochDay(), values);
                }
                if (TYPE_EXPENSE.equals(type)) values[0] += amount;
                else if (TYPE_INCOME.equals(type)) values[1] += amount;
                else if (TYPE_TRANSFER.equals(type)) values[2] = 1L;
            }
        }
        List<DaySummary> result = new ArrayList<>();
        for (Map.Entry<Long, long[]> entry : totals.entrySet()) {
            long[] value = entry.getValue();
            result.add(new DaySummary(entry.getKey(), value[0], value[1], value[2] == 1L));
        }
        return result;
    }

    public TxnView getTransaction(long transactionId) {
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(transactionId));
        List<TxnView> rows = queryTxnViews(TXN_VIEW_SELECT + "WHERE t.id=?", args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<String> getBookkeepers(Long ledgerId) {
        boolean all = ledgerId == null || ledgerId == TransactionFilter.ALL_LEDGERS;
        if (!all) requireLedger(ledgerId);
        List<String> result = new ArrayList<>();
        String sql = "SELECT DISTINCT bookkeeper FROM transactions " +
                (all ? "" : "WHERE ledger_id=? ") + "ORDER BY bookkeeper";
        String[] args = all ? null : new String[]{String.valueOf(ledgerId)};
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                String value = safe(cursor.getString(0)).trim();
                if (!value.isEmpty()) result.add(value);
            }
        }
        return result;
    }

    private List<TxnView> queryTxnViews(String sql, List<String> values) {
        List<TxnView> result = new ArrayList<>();
        String[] args = values == null ? null : values.toArray(new String[0]);
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            while (cursor.moveToNext()) result.add(readTxnView(cursor));
        }
        return result;
    }

    private static Txn readTxn(Cursor cursor) {
        return new Txn(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                cursor.getLong(3), cursor.getLong(4), cursor.getLong(5), cursor.getString(6),
                cursor.isNull(7) ? null : cursor.getLong(7), cursor.getString(8),
                cursor.getString(9), cursor.getString(10), cursor.getInt(11) == 1,
                cursor.getInt(12) == 1, cursor.getString(13), cursor.getLong(14));
    }

    private static TxnView readTxnView(Cursor cursor) {
        return new TxnView(cursor.getLong(0), cursor.getLong(1), cursor.getString(2),
                cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getLong(6),
                cursor.getLong(7), cursor.getLong(8), cursor.getString(9),
                cursor.isNull(10) ? null : cursor.getLong(10), cursor.getString(11),
                cursor.getString(12), cursor.getString(13), cursor.getInt(14) == 1,
                cursor.getInt(15) == 1, cursor.getString(16), cursor.getLong(17));
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
        return getSummary(ledgerId, start.getTimeInMillis(), end.getTimeInMillis());
    }

    public Summary getSummary(long ledgerId, long from, long to) {
        requireLedger(ledgerId);
        SQLiteDatabase db = getReadableDatabase();
        long income = sumType(db, ledgerId, TYPE_INCOME, from, to);
        long expense = sumType(db, ledgerId, TYPE_EXPENSE, from, to);
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
        return getExpenseCategories(ledgerId, start.getTimeInMillis(), end.getTimeInMillis());
    }

    public List<CategoryTotal> getExpenseCategories(long ledgerId, long from, long to) {
        requireLedger(ledgerId);
        List<CategoryTotal> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT category, SUM(amount_cents) total FROM transactions " +
                        "WHERE ledger_id=? AND type=? AND occurred_at>=? AND occurred_at<? " +
                        "GROUP BY category ORDER BY total DESC",
                new String[]{String.valueOf(ledgerId), TYPE_EXPENSE,
                        String.valueOf(from), String.valueOf(to)})) {
            while (cursor.moveToNext()) {
                result.add(new CategoryTotal(cursor.getString(0), cursor.getLong(1)));
            }
        }
        return result;
    }

    public long getNetAssets(long ledgerId) {
        requireLedger(ledgerId);
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
        if (count != 1) throw new IllegalArgumentException("账本不存在");
    }

    private void requireAccountInLedger(long accountId, long ledgerId) {
        long count = queryLong(getReadableDatabase(),
                "SELECT COUNT(*) FROM accounts WHERE id=? AND ledger_id=?",
                new String[]{String.valueOf(accountId), String.valueOf(ledgerId)});
        if (count != 1) throw new IllegalArgumentException("账户不属于当前账本");
    }

    private static void validatePaging(int offset, int limit) {
        if (offset < 0) throw new IllegalArgumentException("分页起点无效");
        if (limit <= 0 || limit > MAX_QUERY_LIMIT) {
            throw new IllegalArgumentException("分页数量无效");
        }
    }

    private static long queryLong(SQLiteDatabase db, String sql, String[] args) {
        try (Cursor cursor = db.rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0L;
        }
    }

    private static String requireText(String value, String message) {
        String clean = safe(value).trim();
        if (clean.isEmpty()) throw new IllegalArgumentException(message);
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

        @Override public String toString() { return name + " · " + currency; }
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

        @Override public String toString() { return name; }
    }

    public static class Txn {
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

    public static final class TxnView extends Txn {
        public final long ledgerId;
        public final String ledgerName;
        public final String currency;

        public TxnView(long id, long ledgerId, String ledgerName, String currency,
                       String type, String category, long amountCents, long discountCents,
                       long accountId, String accountName, Long toAccountId, String toAccountName,
                       String bookkeeper, String tags, boolean reimbursable, boolean includeBudget,
                       String note, long occurredAt) {
            super(id, type, category, amountCents, discountCents, accountId, accountName,
                    toAccountId, toAccountName, bookkeeper, tags, reimbursable,
                    includeBudget, note, occurredAt);
            this.ledgerId = ledgerId;
            this.ledgerName = ledgerName;
            this.currency = currency;
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

    public static final class DaySummary {
        public final long epochDay;
        public final long expenseCents;
        public final long incomeCents;
        public final boolean hasTransfer;

        public DaySummary(long epochDay, long expenseCents, long incomeCents, boolean hasTransfer) {
            this.epochDay = epochDay;
            this.expenseCents = expenseCents;
            this.incomeCents = incomeCents;
            this.hasTransfer = hasTransfer;
        }
    }

    public static final class LedgerCounts {
        public final long accountCount;
        public final long transactionCount;

        public LedgerCounts(long accountCount, long transactionCount) {
            this.accountCount = accountCount;
            this.transactionCount = transactionCount;
        }
    }
}
