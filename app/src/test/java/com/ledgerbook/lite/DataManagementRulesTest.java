package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class DataManagementRulesTest {
    @Test
    public void csvFieldsEscapeQuotesCommasAndNewlines() {
        assertEquals("plain", DataManagementService.csvField("plain"));
        assertEquals("\"a,b\"", DataManagementService.csvField("a,b"));
        assertEquals("\"a\"\"b\"", DataManagementService.csvField("a\"b"));
        assertEquals("\"a\nb\"", DataManagementService.csvField("a\nb"));
        assertEquals("", DataManagementService.csvField(null));
    }

    @Test
    public void generatedNamesArePortableAndTyped() {
        String csv = DataManagementService.exportFileName(1_700_000_000_000L);
        String backup = DataManagementService.backupFileName(1_700_000_000_000L, false);
        String safety = DataManagementService.backupFileName(1_700_000_000_000L, true);
        assertTrue(csv.matches("ledgerbook-\\d{8}-\\d{6}\\.csv"));
        assertTrue(backup.matches("ledgerbook-backup-\\d{8}-\\d{6}\\.json"));
        assertTrue(safety.matches("ledgerbook-safety-\\d{8}-\\d{6}\\.json"));
    }
}
