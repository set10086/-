package com.ledgerbook.lite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class SubscriptionUiContractTest {
    @Test
    public void enabledSubscriptionModuleExposesExplicitPostingFlow() throws IOException {
        String manager = read("app/src/main/java/com/ledgerbook/lite/SubscriptionManagerDialog.java");
        String home = read("app/src/main/java/com/ledgerbook/lite/SubscriptionHomeCard.java");
        String activity = read("app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java");
        String settings = read("app/src/main/java/com/ledgerbook/lite/SettingsPageView.java");
        assertTrue(manager.contains("将最近一笔设为订阅"));
        assertTrue(manager.contains("SubscriptionRules.instantiate("));
        assertTrue(manager.contains("db.addTransaction("));
        assertTrue(manager.contains("repository.advanceAfterPosting("));
        assertTrue(manager.contains("repository.setActive("));
        assertTrue(manager.contains("repository.delete("));
        assertTrue(home.contains("repository.snapshot("));
        assertTrue(home.contains("SubscriptionManagerDialog.show("));
        assertTrue(activity.contains("SubscriptionHomeCard.create("));
        assertTrue(activity.contains("ModuleRepository.SUBSCRIPTIONS"));
        assertTrue(settings.contains("addSubscriptionCard()"));
        assertTrue(settings.contains("SubscriptionManagerDialog.show("));
    }

    private static String read(String relative) throws IOException {
        Path cursor = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 6 && cursor != null; depth++) {
            Path candidate = cursor.resolve(relative);
            if (Files.exists(candidate)) return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
            cursor = cursor.getParent();
        }
        return new String(Files.readAllBytes(Paths.get(relative).toAbsolutePath()), StandardCharsets.UTF_8);
    }
}
