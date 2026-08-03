package com.ledgerbook.lite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class ModuleCenterContractTest {
    @Test
    public void settingsExposePersistedInteractiveModuleCenter() throws IOException {
        String settings = read("app/src/main/java/com/ledgerbook/lite/SettingsPageView.java");
        String dialog = read("app/src/main/java/com/ledgerbook/lite/ModuleCenterDialog.java");

        assertTrue(settings.contains("new ModuleRepository(db)"));
        assertTrue(settings.contains("addModuleCenterCard()"));
        assertTrue(settings.contains("ModuleCenterDialog.show("));
        assertTrue(dialog.contains("repository.setEnabled("));
        assertTrue(dialog.contains("repository.move("));
        assertTrue(dialog.contains("CheckBox"));
        assertFalse(dialog.contains("功能即将上线"));
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
