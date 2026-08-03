package com.ledgerbook.lite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class FastEntryUiContractTest {
    @Test
    public void generatedActivityProvidesProgressiveFastEntry() throws IOException {
        String source = read("app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java");
        assertTrue(source.contains("ModuleRepository.QUICK_ENTRY"));
        assertTrue(source.contains("QuickEntryRules.recentCategories("));
        assertTrue(source.contains("QuickEntryRules.copyOf("));
        assertTrue(source.contains("复制上一笔"));
        assertTrue(source.contains("最近使用"));
        assertTrue(source.contains("更多设置"));
        assertTrue(source.contains("advancedView.setVisibility("));
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
