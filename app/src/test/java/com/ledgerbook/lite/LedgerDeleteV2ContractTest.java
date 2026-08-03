package com.ledgerbook.lite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class LedgerDeleteV2ContractTest {
    @Test
    public void deletingLedgerRemovesAllLedgerScopedV2RowsBeforeParent() throws IOException {
        String source = read("app/src/main/java/com/ledgerbook/lite/LedgerDb.java");
        int pending = source.indexOf("db.delete(\"recurring_pending\"");
        int recurring = source.indexOf("db.delete(\"recurring_rules\"");
        int templates = source.indexOf("db.delete(\"transaction_templates\"");
        int budgets = source.indexOf("db.delete(\"budgets\"");
        int transactions = source.indexOf("db.delete(\"transactions\"");
        int accounts = source.indexOf("db.delete(\"accounts\"");
        int ledger = source.indexOf("db.delete(\"ledgers\"");

        assertTrue(pending >= 0);
        assertTrue(recurring > pending);
        assertTrue(templates > recurring);
        assertTrue(budgets > templates);
        assertTrue(transactions > budgets);
        assertTrue(accounts > transactions);
        assertTrue(ledger > accounts);
    }

    private static String read(String relative) throws IOException {
        Path cursor = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 6 && cursor != null; depth++) {
            Path candidate = cursor.resolve(relative);
            if (Files.exists(candidate)) {
                return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
            }
            cursor = cursor.getParent();
        }
        return new String(Files.readAllBytes(Paths.get(relative).toAbsolutePath()),
                StandardCharsets.UTF_8);
    }
}
