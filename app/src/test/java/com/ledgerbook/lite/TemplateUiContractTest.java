package com.ledgerbook.lite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class TemplateUiContractTest {
    @Test
    public void enabledTemplateModuleExposesManagerAndHomeShortcuts() throws IOException {
        String manager = read("app/src/main/java/com/ledgerbook/lite/TemplateManagerDialog.java");
        String home = read("app/src/main/java/com/ledgerbook/lite/TemplateHomeCard.java");
        String activity = read("app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java");
        String settings = read("app/src/main/java/com/ledgerbook/lite/SettingsPageView.java");

        assertTrue(manager.contains("saveFromTransaction("));
        assertTrue(manager.contains("TemplateRules.instantiate("));
        assertTrue(manager.contains("db.addTransaction("));
        assertTrue(manager.contains("将最近一笔保存为模板"));
        assertTrue(home.contains("TemplateManagerDialog.confirmUse("));
        assertTrue(activity.contains("TemplateHomeCard.create("));
        assertTrue(activity.contains("ModuleRepository.TEMPLATES"));
        assertTrue(settings.contains("addTemplateCard()"));
        assertTrue(settings.contains("TemplateManagerDialog.show("));
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
