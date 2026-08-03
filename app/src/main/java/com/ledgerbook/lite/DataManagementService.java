package com.ledgerbook.lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Local-only CSV export and complete JSON backup/restore. */
public final class DataManagementService {
    private static final String[] EXPORT_TABLES = {
            "ledgers", "accounts", "transactions", "app_modules", "user_preferences",
            "budgets", "transaction_templates", "recurring_rules",
            "recurring_pending", "subscriptions"
    };
    private static final String[] DELETE_ORDER = {
            "recurring_pending", "recurring_rules", "subscriptions",
            "transaction_templates", "budgets", "transactions", "accounts",
            "ledgers", "user_preferences", "app_modules"
    };
    private static final String[] INSERT_ORDER = EXPORT_TABLES;

    private final Context context;
    private final LedgerDb helper;

    public DataManagementService(Context context, LedgerDb helper) {
        if (context == null || helper == null) throw new IllegalArgumentException("context and db are required");
        this.context = context.getApplicationContext();
        this.helper = helper;
    }

    public File exportCsv(long ledgerId) throws IOException {
        if (ledgerId <= 0L) throw new IllegalArgumentException("账本无效");
        File file = new File(directory("exports"), exportFileName(System.currentTimeMillis()));
        StringBuilder csv = new StringBuilder("\uFEFF日期,类型,分类,金额,币种,账户,转入账户,记账人,标签,报销,计入预算,备注\n");
        String sql = "SELECT t.occurred_at,t.type,t.category,t.amount_cents,l.base_currency,"
                + "a.name,COALESCE(b.name,''),t.bookkeeper,t.tags,t.reimbursable,"
                + "t.include_budget,t.note FROM transactions t "
                + "JOIN ledgers l ON l.id=t.ledger_id JOIN accounts a ON a.id=t.account_id "
                + "LEFT JOIN accounts b ON b.id=t.to_account_id WHERE t.ledger_id=? "
                + "ORDER BY t.occurred_at,t.id";
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(sql,
                new String[]{String.valueOf(ledgerId)})) {
            while (cursor.moveToNext()) {
                String[] fields = {
                        V13Ui.dateTime(cursor.getLong(0)), V13Ui.typeName(cursor.getString(1)),
                        cursor.getString(2), java.math.BigDecimal.valueOf(cursor.getLong(3), 2).toPlainString(),
                        cursor.getString(4), cursor.getString(5), cursor.getString(6),
                        cursor.getString(7), cursor.getString(8), cursor.getInt(9) == 1 ? "是" : "否",
                        cursor.getInt(10) == 1 ? "是" : "否", cursor.getString(11)
                };
                for (int index = 0; index < fields.length; index++) {
                    if (index > 0) csv.append(',');
                    csv.append(csvField(fields[index]));
                }
                csv.append('\n');
            }
        }
        write(file, csv.toString());
        return file;
    }

    public File createJsonBackup() throws IOException {
        return createJsonBackup(false);
    }

    public File createSafetySnapshot() throws IOException {
        return createJsonBackup(true);
    }

    private File createJsonBackup(boolean safety) throws IOException {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).close();
        JSONObject root = new JSONObject();
        try {
            root.put("format", "ledgerbook-lite-json");
            root.put("schemaVersion", LedgerV2Migration.VERSION);
            root.put("createdAt", System.currentTimeMillis());
            JSONObject tables = new JSONObject();
            for (String table : EXPORT_TABLES) tables.put(table, exportTable(db, table));
            root.put("tables", tables);
        } catch (JSONException error) {
            throw new IOException("备份编码失败", error);
        }
        File file = new File(directory("backups"),
                backupFileName(System.currentTimeMillis(), safety));
        write(file, root.toString(2));
        return file;
    }

    public void restoreJsonBackup(File file) throws IOException {
        if (file == null || !file.isFile()) throw new IllegalArgumentException("备份文件不存在");
        createSafetySnapshot();
        JSONObject root;
        try {
            root = new JSONObject(read(file));
            if (!"ledgerbook-lite-json".equals(root.optString("format"))) {
                throw new IllegalArgumentException("不是 LedgerBook JSON 备份");
            }
            if (root.optInt("schemaVersion", -1) != LedgerV2Migration.VERSION) {
                throw new IllegalArgumentException("备份数据库版本不兼容");
            }
        } catch (JSONException error) {
            throw new IOException("备份文件损坏", error);
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            JSONObject tables = root.getJSONObject("tables");
            for (String table : EXPORT_TABLES) {
                if (!tables.has(table) || !(tables.get(table) instanceof JSONArray)) {
                    throw new IllegalArgumentException("备份缺少数据表：" + table);
                }
            }
            for (String table : DELETE_ORDER) db.delete(table, null, null);
            for (String table : INSERT_ORDER) importTable(db, table, tables.getJSONArray(table));
            if (queryCount(db, "ledgers") <= 0L || queryCount(db, "accounts") <= 0L) {
                throw new IllegalArgumentException("备份缺少账本或账户");
            }
            db.setTransactionSuccessful();
        } catch (JSONException error) {
            throw new IOException("备份结构无效", error);
        } finally {
            db.endTransaction();
        }
    }

    public List<File> listBackups() {
        File[] files = directory("backups").listFiles((dir, name) -> name.endsWith(".json"));
        List<File> result = files == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(files));
        result.sort((left, right) -> Long.compare(right.lastModified(), left.lastModified()));
        return result;
    }

    public static String csvField(String value) {
        if (value == null) return "";
        boolean quote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }

    public static String exportFileName(long time) {
        return "ledgerbook-" + timestamp(time) + ".csv";
    }

    public static String backupFileName(long time, boolean safety) {
        return "ledgerbook-" + (safety ? "safety-" : "backup-") + timestamp(time) + ".json";
    }

    private static String timestamp(long time) {
        return new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date(time));
    }

    private File directory(String child) {
        File documents = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File base = documents == null ? new File(context.getFilesDir(), "documents") : documents;
        File result = new File(new File(base, "LedgerBook"), child);
        if (!result.exists() && !result.mkdirs()) throw new IllegalStateException("无法创建数据目录");
        return result;
    }

    private static JSONArray exportTable(SQLiteDatabase db, String table) throws JSONException {
        JSONArray rows = new JSONArray();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + table, null)) {
            while (cursor.moveToNext()) {
                JSONObject row = new JSONObject();
                for (int column = 0; column < cursor.getColumnCount(); column++) {
                    String name = cursor.getColumnName(column);
                    switch (cursor.getType(column)) {
                        case Cursor.FIELD_TYPE_NULL: row.put(name, JSONObject.NULL); break;
                        case Cursor.FIELD_TYPE_INTEGER: row.put(name, cursor.getLong(column)); break;
                        case Cursor.FIELD_TYPE_FLOAT: row.put(name, cursor.getDouble(column)); break;
                        case Cursor.FIELD_TYPE_BLOB:
                            row.put(name, android.util.Base64.encodeToString(cursor.getBlob(column), android.util.Base64.NO_WRAP));
                            break;
                        default: row.put(name, cursor.getString(column)); break;
                    }
                }
                rows.put(row);
            }
        }
        return rows;
    }

    private static void importTable(SQLiteDatabase db, String table, JSONArray rows) throws JSONException {
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.getJSONObject(index);
            ContentValues values = new ContentValues();
            java.util.Iterator<String> keys = row.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = row.get(key);
                if (value == JSONObject.NULL) values.putNull(key);
                else if (value instanceof Integer || value instanceof Long) values.put(key, ((Number) value).longValue());
                else if (value instanceof Number) values.put(key, ((Number) value).doubleValue());
                else values.put(key, String.valueOf(value));
            }
            db.insertOrThrow(table, null, values);
        }
    }

    private static long queryCount(SQLiteDatabase db, String table) {
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0L;
        }
    }

    private static void write(File file, String value) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
    }

    private static String read(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];
        try (FileInputStream input = new FileInputStream(file)) {
            int offset = 0;
            while (offset < buffer.length) {
                int count = input.read(buffer, offset, buffer.length - offset);
                if (count < 0) break;
                offset += count;
            }
            if (offset != buffer.length) throw new IOException("备份文件读取不完整");
        }
        return new String(buffer, StandardCharsets.UTF_8);
    }
}
