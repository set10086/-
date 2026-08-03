package com.ledgerbook.lite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class RecurringUiContractTest {
    @Test
    public void enabledRecurringModuleExposesPendingConfirmationFlow() throws IOException {
        String manager = read("app/src/main/java/com/ledgerbook/lite/RecurringManagerDialog.java");
        String home = read("app/src/main/java/com/ledgerbook/lite/RecurringHomeCard.java");
        String activity = read("app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java");
        String settings = read("app/src/main/java/com/ledgerbook/lite/SettingsPageView.java");

        assertTrue(manager.contains("materializeDue("));
        assertTrue(manager.contains("RecurringRules.instantiate("));
        assertTrue(manager.contains("db.addTransaction("));
        assertTrue(manager.contains("repository.markPosted("));
        assertTrue(manager.contains("repository.skip("));
        assertTrue(manager.contains("repository.setEnabled("));
        assertTrue(manager.contains("repository.deleteRule("));
        assertTrue(manager.contains("将最近一笔设为周期"));
        assertTrue(home.contains("repository.listPending("));
        assertTrue(home.contains("RecurringManagerDialog.show("));
        assertTrue(activity.contains("RecurringHomeCard.create("));
        assertTrue(activity.contains("ModuleRepository.RECURRING"));
        assertTrue(settings.contains("addRecurringCard()"));
        assertTrue(settings.contains("RecurringManagerDialog.show("));
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
