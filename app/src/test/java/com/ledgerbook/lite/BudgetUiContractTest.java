package com.ledgerbook.lite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class BudgetUiContractTest {
    @Test
    public void homeAndSettingsExposeLiveBudgetManagement() throws IOException {
        String home = read("app/src/main/java/com/ledgerbook/lite/BudgetHomeCard.java");
        String manager = read("app/src/main/java/com/ledgerbook/lite/BudgetManagerDialog.java");
        String activity = read("app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java");
        String settings = read("app/src/main/java/com/ledgerbook/lite/SettingsPageView.java");

        assertTrue(home.contains("BudgetRepository.MonthSnapshot"));
        assertTrue(home.contains("BudgetManagerDialog.show("));
        assertTrue(home.contains("本月预算"));
        assertTrue(manager.contains("setTotalBudget("));
        assertTrue(manager.contains("setCategoryBudget("));
        assertTrue(manager.contains("removeCategoryBudget("));
        assertTrue(manager.contains("CategoryPickerDialog.show("));
        assertTrue(activity.contains("BudgetHomeCard.create("));
        assertTrue(activity.contains("ModuleRepository.BUDGET"));
        assertTrue(settings.contains("addBudgetCard()"));
        assertTrue(settings.contains("BudgetManagerDialog.show("));
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
