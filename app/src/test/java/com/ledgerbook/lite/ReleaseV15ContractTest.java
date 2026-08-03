package com.ledgerbook.lite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class ReleaseV15ContractTest {
    @Test
    public void appAndArtifactsUseV150Metadata() throws IOException {
        String build = read("app/build.gradle");
        String settings = read("app/src/main/java/com/ledgerbook/lite/SettingsPageView.java");
        String workflow = read(".github/workflows/build-apk.yml");

        assertTrue(build.contains("versionCode 7"));
        assertTrue(build.contains("versionName '1.5.0'"));
        assertTrue(settings.contains("LedgerBook Lite 1.5.0"));
        assertTrue(workflow.contains("LedgerBook-Lite-v1.5.0.apk"));
        assertTrue(workflow.contains("LedgerBook-Lite-v1.5.0.sha256"));
        assertTrue(workflow.contains("LedgerBook-Lite-V1.5.0-APK"));
        assertTrue(workflow.contains("apksigner\" verify"));
        assertTrue(workflow.contains("unzip -t"));
        assertTrue(workflow.contains("sha256sum"));
        assertFalse(workflow.contains("LedgerBook-Lite-v1.4.1"));
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
