package com.ledgerbook.lite;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SettingsInteractionContractTest {
    @Test
    public void settingsExposeFullWidthClickTargetsInsteadOfTinySpinners() throws IOException {
        Path sourcePath = findFromWorkingDirectory(
                "app/src/main/java/com/ledgerbook/lite/SettingsPageView.java");
        String source = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);

        assertFalse("Settings must not depend on small Spinner hit targets",
                source.contains("new Spinner(activity)"));
        assertTrue(source.contains("choiceRow("));
        assertTrue(source.contains("showSingleChoice("));
        assertTrue(source.contains("toggleRow("));
        assertTrue(source.contains("setOnClickListener"));
        assertTrue(source.contains("setContentDescription"));
        assertTrue(source.contains("设置交互自检"));
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
