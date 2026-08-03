package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.junit.Test;

public final class LedgerV2MigrationTest {
    @Test
    public void migrationPreservesV1LedgerAccountAndTransactionData() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            createV1Schema(connection);
            insertV1Fixture(connection);

            LedgerV2Migration.apply((sql, args) -> execute(connection, sql, args), 1_700_000_000_000L);

            assertEquals(1L, scalarLong(connection, "SELECT COUNT(*) FROM ledgers"));
            assertEquals("家庭账本", scalarText(connection, "SELECT name FROM ledgers WHERE id=1"));
            assertEquals(123_456L,
                    scalarLong(connection, "SELECT balance_cents FROM accounts WHERE id=1"));
            assertEquals("餐饮/午餐",
                    scalarText(connection, "SELECT category FROM transactions WHERE id=1"));
            assertEquals(2_580L,
                    scalarLong(connection, "SELECT amount_cents FROM transactions WHERE id=1"));
            assertEquals(7L, scalarLong(connection, "SELECT COUNT(*) FROM app_modules"));
            assertEquals(0L, scalarLong(connection, "SELECT COUNT(*) FROM user_preferences"));
        }
    }

    @Test
    public void migrationStatementsAreIdempotentAndNonDestructive() {
        List<String> statements = LedgerV2Migration.createStatements();
        assertEquals(2, statements.size());
        for (String statement : statements) {
            String upper = statement.toUpperCase(java.util.Locale.ROOT);
            assertTrue(upper.contains("CREATE TABLE IF NOT EXISTS"));
            assertFalse(upper.contains("DROP "));
            assertFalse(upper.contains("DELETE "));
            assertFalse(upper.contains("ALTER TABLE"));
        }
    }

    private static void createV1Schema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE ledgers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, base_currency TEXT NOT NULL DEFAULT 'CNY', created_at INTEGER NOT NULL)");
            statement.execute("CREATE TABLE accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, ledger_id INTEGER NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, group_name TEXT NOT NULL DEFAULT '', balance_cents INTEGER NOT NULL DEFAULT 0, note TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)");
            statement.execute("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, ledger_id INTEGER NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL, amount_cents INTEGER NOT NULL, discount_cents INTEGER NOT NULL DEFAULT 0, account_id INTEGER NOT NULL, to_account_id INTEGER, bookkeeper TEXT NOT NULL DEFAULT '本人', tags TEXT NOT NULL DEFAULT '', reimbursable INTEGER NOT NULL DEFAULT 0, include_budget INTEGER NOT NULL DEFAULT 1, note TEXT NOT NULL DEFAULT '', occurred_at INTEGER NOT NULL, created_at INTEGER NOT NULL)");
        }
    }

    private static void insertV1Fixture(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO ledgers(id,name,base_currency,created_at) VALUES(1,'家庭账本','CNY',1000)");
            statement.executeUpdate("INSERT INTO accounts(id,ledger_id,name,type,group_name,balance_cents,note,created_at) VALUES(1,1,'微信','支付账户','常用账户',123456,'历史账户',1000)");
            statement.executeUpdate("INSERT INTO transactions(id,ledger_id,type,category,amount_cents,discount_cents,account_id,to_account_id,bookkeeper,tags,reimbursable,include_budget,note,occurred_at,created_at) VALUES(1,1,'EXPENSE','餐饮/午餐',2580,0,1,NULL,'本人','工作日',0,1,'历史账单',2000,2000)");
        }
    }

    private static void execute(Connection connection, String sql, Object[] args) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < args.length; index++) {
                statement.setObject(index + 1, args[index]);
            }
            statement.executeUpdate();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static long scalarLong(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getLong(1) : 0L;
        }
    }

    private static String scalarText(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getString(1) : null;
        }
    }
}
