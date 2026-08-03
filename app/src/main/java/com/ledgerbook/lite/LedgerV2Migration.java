package com.ledgerbook.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure SQL migration definition shared by Android SQLite and JVM migration tests. */
public final class LedgerV2Migration {
    public static final int VERSION = 2;

    public interface SqlExecutor {
        void execute(String sql, Object[] args);
    }

    public static final class ModuleSeed {
        public final String key;
        public final boolean enabled;
        public final int sortOrder;

        ModuleSeed(String key, boolean enabled, int sortOrder) {
            this.key = key;
            this.enabled = enabled;
            this.sortOrder = sortOrder;
        }
    }

    private static final List<String> CREATE_STATEMENTS;
    private static final List<ModuleSeed> DEFAULT_MODULES;

    static {
        List<String> statements = new ArrayList<>();
        statements.add("CREATE TABLE IF NOT EXISTS app_modules ("
                + "module_key TEXT PRIMARY KEY,"
                + "enabled INTEGER NOT NULL DEFAULT 0 CHECK(enabled IN (0,1)),"
                + "sort_order INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL)");
        statements.add("CREATE TABLE IF NOT EXISTS user_preferences ("
                + "pref_key TEXT PRIMARY KEY,"
                + "pref_value TEXT NOT NULL,"
                + "updated_at INTEGER NOT NULL)");
        statements.add("CREATE TABLE IF NOT EXISTS budgets ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "ledger_id INTEGER NOT NULL,"
                + "month_key TEXT NOT NULL,"
                + "category_key TEXT NOT NULL DEFAULT '',"
                + "amount_cents INTEGER NOT NULL CHECK(amount_cents>0),"
                + "created_at INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL,"
                + "UNIQUE(ledger_id,month_key,category_key),"
                + "FOREIGN KEY(ledger_id) REFERENCES ledgers(id))");
        CREATE_STATEMENTS = Collections.unmodifiableList(statements);

        List<ModuleSeed> modules = new ArrayList<>();
        modules.add(new ModuleSeed("quick_entry", true, 0));
        modules.add(new ModuleSeed("budget", true, 10));
        modules.add(new ModuleSeed("data_management", true, 20));
        modules.add(new ModuleSeed("templates", false, 30));
        modules.add(new ModuleSeed("recurring", false, 40));
        modules.add(new ModuleSeed("subscriptions", false, 50));
        modules.add(new ModuleSeed("privacy_lock", false, 60));
        DEFAULT_MODULES = Collections.unmodifiableList(modules);
    }

    private LedgerV2Migration() {
    }

    public static List<String> createStatements() {
        return CREATE_STATEMENTS;
    }

    public static List<ModuleSeed> defaultModules() {
        return DEFAULT_MODULES;
    }

    public static void apply(SqlExecutor executor, long now) {
        if (executor == null) throw new IllegalArgumentException("executor cannot be null");
        for (String statement : CREATE_STATEMENTS) {
            executor.execute(statement, new Object[0]);
        }
        String insert = "INSERT OR IGNORE INTO app_modules"
                + "(module_key,enabled,sort_order,updated_at) VALUES(?,?,?,?)";
        for (ModuleSeed module : DEFAULT_MODULES) {
            executor.execute(insert, new Object[]{
                    module.key, module.enabled ? 1 : 0, module.sortOrder, now
            });
        }
    }
}
