package com.ledgerbook.lite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class DataManagementContractTest {
    @Test
    public void dataManagerExportsAndRestoresWithSafetySnapshot() throws IOException {
        String service = read("app/src/main/java/com/ledgerbook/lite/DataManagementService.java");
        String dialog = read("app/src/main/java/com/ledgerbook/lite/DataManagementDialog.java");
        String settings = read("app/src/main/java/com/ledgerbook/lite/SettingsPageView.java");
        assertTrue(service.contains("exportCsv("));
        assertTrue(service.contains("createJsonBackup("));
        assertTrue(service.contains("createSafetySnapshot("));
        assertTrue(service.contains("restoreJsonBackup("));
        assertTrue(service.contains("createSafetySnapshot();"));
        assertTrue(service.contains("db.beginTransaction()"));
        assertTrue(service.contains("db.setTransactionSuccessful()"));
        assertTrue(service.contains("PRAGMA wal_checkpoint(FULL)"));
        assertTrue(dialog.contains("导出 CSV"));
        assertTrue(dialog.contains("创建 JSON 备份"));
        assertTrue(dialog.contains("恢复此备份"));
        assertTrue(settings.contains("addDataManagementCard()"));
        assertTrue(settings.contains("DataManagementDialog.show("));
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
