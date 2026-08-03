package com.ledgerbook.lite;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public final class StatisticsUiContractTest {
    @Test
    public void statisticsPageShowsComparisonTrendAndCategoryRanking() throws IOException {
        String dashboard = read("app/src/main/java/com/ledgerbook/lite/StatisticsDashboardView.java");
        String repository = read("app/src/main/java/com/ledgerbook/lite/StatisticsRepository.java");
        String activity = read("app/src/main/java/com/ledgerbook/lite/LedgerV14Activity.java");
        String build = read("app/build.gradle");

        assertTrue(dashboard.contains("本月概览"));
        assertTrue(dashboard.contains("较上月"));
        assertTrue(dashboard.contains("近 6 个月趋势"));
        assertTrue(dashboard.contains("支出分类排行"));
        assertTrue(repository.contains("TREND_MONTHS = 6"));
        assertTrue(repository.contains("db.getSummary("));
        assertTrue(repository.contains("db.getExpenseCategories("));
        assertTrue(activity.contains("new StatisticsDashboardView(this, db, currentLedgerId)"));
        assertTrue(build.contains("prepareV15StatisticsUi"));
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
