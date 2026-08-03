package com.ledgerbook.lite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class PrivacyUiContractTest {
    @Test
    public void privacyModuleProvidesPinManagementAndLifecycleLock() throws IOException {
        String store = read("app/src/main/java/com/ledgerbook/lite/PinStore.java");
        String settingsDialog = read("app/src/main/java/com/ledgerbook/lite/PrivacySettingsDialog.java");
        String unlockDialog = read("app/src/main/java/com/ledgerbook/lite/PinUnlockDialog.java");
        String settings = read("app/src/main/java/com/ledgerbook/lite/SettingsPageView.java");
        String activity = read("app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java");

        assertTrue(store.contains("PinSecurity.create("));
        assertTrue(store.contains("PinSecurity.verify("));
        assertTrue(store.contains("PinSecurity.afterFailure("));
        assertTrue(store.contains("markBackground("));
        assertTrue(store.contains("shouldLock("));
        assertTrue(settingsDialog.contains("设置 PIN"));
        assertTrue(settingsDialog.contains("修改 PIN"));
        assertTrue(settingsDialog.contains("移除 PIN"));
        assertTrue(settingsDialog.contains("自动锁定时间"));
        assertTrue(unlockDialog.contains("setCancelable(false)"));
        assertTrue(unlockDialog.contains("store.verify("));
        assertTrue(settings.contains("addPrivacyCard()"));
        assertTrue(settings.contains("PrivacySettingsDialog.show("));
        assertTrue(activity.contains("ModuleRepository.PRIVACY_LOCK"));
        assertTrue(activity.contains("PinUnlockDialog.show("));
        assertTrue(activity.contains("protected void onResume()"));
        assertTrue(activity.contains("protected void onStop()"));
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
