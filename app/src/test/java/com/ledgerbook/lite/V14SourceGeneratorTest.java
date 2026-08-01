package com.ledgerbook.lite;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public final class V14SourceGeneratorTest {
    @Test
    public void generatedActivityContainsV14NavigationSettingsAndInsetContracts() throws IOException {
        Path generated = findFromWorkingDirectory(
                "app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java");
        assertTrue("V1.4 activity was not generated: " + generated, Files.exists(generated));
        String source = new String(Files.readAllBytes(generated), StandardCharsets.UTF_8);

        assertTrue(source.contains("class LedgerV14Activity"));
        assertTrue(source.contains("private static final int SETTINGS = 4"));
        assertTrue(source.contains("new TextView[5]"));
        assertTrue(source.contains("⚙️\\n设置"));
        assertTrue(source.contains("new SettingsPageView"));
        assertTrue(source.contains("applyDrawerInsets(drawer)"));
        assertTrue(source.contains("ledgerDrawer.applySystemInsets(topInset, bottomInset)"));
        assertTrue(source.contains("appSettings.defaultAccountId(currentLedgerId)"));
        assertTrue(source.contains("appSettings.defaultBookkeeper()"));
    }

    private static Path findFromWorkingDirectory(String relative) {
        Path cursor = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 6 && cursor != null; depth++) {
            Path candidate = cursor.resolve(relative);
            if (Files.exists(candidate)) return candidate;
            cursor = cursor.getParent();
        }
        return Paths.get(relative).toAbsolutePath();
    }
}
