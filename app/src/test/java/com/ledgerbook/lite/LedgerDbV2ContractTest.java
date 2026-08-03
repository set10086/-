package com.ledgerbook.lite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class LedgerDbV2ContractTest {
    @Test
    public void ledgerDbUsesSharedV2MigrationForCreateAndUpgrade() throws IOException {
        Path sourcePath = findFromWorkingDirectory(
                "app/src/main/java/com/ledgerbook/lite/LedgerDb.java");
        String source = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "private static final int DB_VERSION = LedgerV2Migration.VERSION;"));
        assertTrue(source.contains("applyV2Migration(db);"));
        assertTrue(source.contains("if (oldVersion < 2) applyV2Migration(db);"));
        assertTrue(source.contains("LedgerV2Migration.apply((sql, args) ->"));
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
