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
        statements.add("CREATE TABLE IF NOT EXISTS transaction_templates ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "ledger_id INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "type TEXT NOT NULL,"
                + "category TEXT NOT NULL,"
                + "amount_cents INTEGER NOT NULL CHECK(amount_cents>0),"
                + "discount_cents INTEGER NOT NULL DEFAULT 0,"
                + "account_id INTEGER NOT NULL,"
                + "to_account_id INTEGER,"
                + "bookkeeper TEXT NOT NULL DEFAULT '本人',"
                + "tags TEXT NOT NULL DEFAULT '',"
                + "reimbursable INTEGER NOT NULL DEFAULT 0 CHECK(reimbursable IN (0,1)),"
                + "include_budget INTEGER NOT NULL DEFAULT 1 CHECK(include_budget IN (0,1)),"
                + "note TEXT NOT NULL DEFAULT '',"
                + "sort_order INTEGER NOT NULL DEFAULT 0,"
                + "created_at INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL,"
                + "FOREIGN KEY(ledger_id) REFERENCES ledgers(id))");
        statements.add("CREATE TABLE IF NOT EXISTS recurring_rules ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "ledger_id INTEGER NOT NULL,"
                + "name TEXT NOT NULL,"
                + "type TEXT NOT NULL,"
                + "category TEXT NOT NULL,"
                + "amount_cents INTEGER NOT NULL CHECK(amount_cents>0),"
                + "discount_cents INTEGER NOT NULL DEFAULT 0,"
                + "account_id INTEGER NOT NULL,"
                + "to_account_id INTEGER,"
                + "bookkeeper TEXT NOT NULL DEFAULT '本人',"
                + "tags TEXT NOT NULL DEFAULT '',"
                + "reimbursable INTEGER NOT NULL DEFAULT 0 CHECK(reimbursable IN (0,1)),"
                + "include_budget INTEGER NOT NULL DEFAULT 1 CHECK(include_budget IN (0,1)),"
                + "note TEXT NOT NULL DEFAULT '',"
                + "frequency TEXT NOT NULL CHECK(frequency IN ('DAILY','WEEKLY','MONTHLY','YEARLY')),"
                + "interval_count INTEGER NOT NULL DEFAULT 1 CHECK(interval_count>0),"
                + "start_date TEXT NOT NULL,"
                + "end_date TEXT,"
                + "max_occurrences INTEGER NOT NULL DEFAULT 0 CHECK(max_occurrences>=0),"
                + "enabled INTEGER NOT NULL DEFAULT 1 CHECK(enabled IN (0,1)),"
                + "sort_order INTEGER NOT NULL DEFAULT 0,"
                + "created_at INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL,"
                + "FOREIGN KEY(ledger_id) REFERENCES ledgers(id))");
        statements.add("CREATE TABLE IF NOT EXISTS recurring_pending ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "rule_id INTEGER NOT NULL,"
                + "occurrence_index INTEGER NOT NULL CHECK(occurrence_index>=0),"
                + "due_date TEXT NOT NULL,"
                + "status TEXT NOT NULL DEFAULT 'PENDING' "
                + "CHECK(status IN ('PENDING','POSTED','SKIPPED')),"
                + "transaction_id INTEGER,"
                + "created_at INTEGER NOT NULL,"
                + "resolved_at INTEGER,"
                + "UNIQUE(rule_id,occurrence_index),"
                + "FOREIGN KEY(rule_id) REFERENCES recurring_rules(id) ON DELETE CASCADE)");
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
