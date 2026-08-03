package com.ledgerbook.lite;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/** Crayon-style monthly dashboard with comparisons, trend bars, and category ranking. */
public final class StatisticsDashboardView extends LinearLayout {
    private final Context context;
    private final String currency;
    private final StatisticsRepository.Snapshot snapshot;

    public StatisticsDashboardView(Context context, LedgerDb db, long ledgerId) {
        super(context);
        if (context == null || db == null) {
            throw new IllegalArgumentException("context and db are required");
        }
        this.context = context;
        LedgerDb.Ledger ledger = db.getLedger(ledgerId);
        this.currency = ledger == null ? "CNY" : ledger.currency;
        this.snapshot = new StatisticsRepository(db).load(ledgerId, YearMonth.now());
        setOrientation(VERTICAL);
        setPadding(0, 0, 0, V13Ui.dp(context, 12));
        render();
    }

    private void render() {
        addHeader();
        addView(V13Ui.gap(context, 12));
        addSummary();
        addView(V13Ui.gap(context, 10));
        addComparison();
        addView(V13Ui.gap(context, 12));
        addTrend();
        addView(V13Ui.gap(context, 12));
        addCategoryRanking();
    }

    private void addHeader() {
        TextView title = V13Ui.text(context, "📊 本月统计", 23, CartoonStyle.INK, true);
        addView(title);
        TextView subtitle = V13Ui.text(context,
                snapshot.month.getYear() + " 年 " + snapshot.month.getMonthValue()
                        + " 月 · 当前账本收支回顾",
                13, CartoonStyle.MUTED, false);
        subtitle.setPadding(0, V13Ui.dp(context, 4), 0, 0);
        addView(subtitle);
    }

    private void addSummary() {
        LinearLayout card = V13Ui.card(context, CartoonStyle.SOFT_YELLOW);
        card.addView(V13Ui.text(context, "本月概览", 17, CartoonStyle.INK, true));
        card.addView(V13Ui.gap(context, 9));

        LinearLayout first = new LinearLayout(context);
        first.setOrientation(HORIZONTAL);
        first.addView(metric("🌱 收入", snapshot.current.incomeCents,
                CartoonStyle.INCOME), weighted());
        first.addView(horizontalGap(8));
        first.addView(metric("🍑 支出", snapshot.current.expenseCents,
                CartoonStyle.EXPENSE), weighted());
        card.addView(first);
        card.addView(V13Ui.gap(context, 8));

        LinearLayout second = new LinearLayout(context);
        long balance = snapshot.current.balanceCents();
        second.addView(metric("✨ 结余", balance,
                balance < 0L ? CartoonStyle.EXPENSE : CartoonStyle.INK), weighted());
        second.addView(horizontalGap(8));
        second.addView(metric("👛 净资产", snapshot.netAssetsCents,
                snapshot.netAssetsCents < 0L ? CartoonStyle.EXPENSE : CartoonStyle.INK),
                weighted());
        card.addView(second);
        addView(card);
    }

    private LinearLayout metric(String label, long cents, int color) {
        LinearLayout value = new LinearLayout(context);
        value.setOrientation(VERTICAL);
        value.setPadding(V13Ui.dp(context, 10), V13Ui.dp(context, 9),
                V13Ui.dp(context, 10), V13Ui.dp(context, 9));
        value.setBackground(V13Ui.panel(context, CartoonStyle.SURFACE,
                0xFFE5D7C3, 1, 15));
        value.addView(V13Ui.text(context, label, 13, CartoonStyle.MUTED, false));
        TextView money = V13Ui.text(context, money(cents), 16, color, true);
        money.setPadding(0, V13Ui.dp(context, 3), 0, 0);
        value.addView(money);
        return value;
    }

    private void addComparison() {
        LinearLayout card = V13Ui.card(context, CartoonStyle.SOFT_SKY);
        card.addView(V13Ui.text(context, "较上月", 17, CartoonStyle.INK, true));
        card.addView(V13Ui.gap(context, 7));
        card.addView(comparisonRow("🌱 收入", snapshot.incomeComparison,
                CartoonStyle.INCOME));
        card.addView(V13Ui.gap(context, 6));
        card.addView(comparisonRow("🍑 支出", snapshot.expenseComparison,
                CartoonStyle.EXPENSE));
        addView(card);
    }

    private TextView comparisonRow(String label, StatisticsRules.Comparison value, int color) {
        String detail;
        if (value.direction == StatisticsRules.Direction.NEW) {
            detail = "上月无记录，本月新增 " + money(value.currentCents);
        } else if (value.direction == StatisticsRules.Direction.FLAT) {
            detail = "与上月持平 · " + money(value.currentCents);
        } else {
            String verb = value.direction == StatisticsRules.Direction.UP ? "增加" : "减少";
            detail = verb + " " + Math.abs(value.percentChange) + "% · "
                    + signedMoney(value.deltaCents);
        }
        TextView row = V13Ui.text(context, label + "\n" + detail,
                14, color, true);
        row.setLineSpacing(0f, 1.16f);
        row.setPadding(V13Ui.dp(context, 10), V13Ui.dp(context, 8),
                V13Ui.dp(context, 10), V13Ui.dp(context, 8));
        row.setBackground(V13Ui.panel(context, CartoonStyle.SURFACE,
                0xFFE5D7C3, 1, 14));
        return row;
    }

    private void addTrend() {
        LinearLayout card = V13Ui.card(context, CartoonStyle.SOFT_LAVENDER);
        card.addView(V13Ui.text(context, "近 6 个月趋势", 17, CartoonStyle.INK, true));
        TextView legend = V13Ui.text(context, "绿色为收入 · 橙色为支出",
                12, CartoonStyle.MUTED, false);
        legend.setPadding(0, V13Ui.dp(context, 3), 0, V13Ui.dp(context, 7));
        card.addView(legend);

        long maximum = 0L;
        for (StatisticsRules.MonthTotals month : snapshot.trend) {
            maximum = Math.max(maximum, Math.max(month.incomeCents, month.expenseCents));
        }
        for (StatisticsRules.MonthTotals month : snapshot.trend) {
            card.addView(trendMonth(month, maximum));
            card.addView(V13Ui.gap(context, 7));
        }
        addView(card);
    }

    private LinearLayout trendMonth(StatisticsRules.MonthTotals month, long maximum) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        YearMonth parsed = YearMonth.parse(month.monthKey);
        TextView label = V13Ui.text(context, parsed.getMonthValue() + "月",
                13, CartoonStyle.INK, true);
        row.addView(label, new LinearLayout.LayoutParams(
                V13Ui.dp(context, 42), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout bars = new LinearLayout(context);
        bars.setOrientation(VERTICAL);
        bars.addView(trendBar("收", month.incomeCents, maximum,
                CartoonStyle.SOFT_GREEN));
        bars.addView(V13Ui.gap(context, 3));
        bars.addView(trendBar("支", month.expenseCents, maximum,
                CartoonStyle.SOFT_PEACH));
        row.addView(bars, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private LinearLayout trendBar(String prefix, long value, long maximum, int fill) {
        LinearLayout line = new LinearLayout(context);
        line.setOrientation(HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = V13Ui.text(context, prefix, 11, CartoonStyle.MUTED, true);
        line.addView(label, new LinearLayout.LayoutParams(
                V13Ui.dp(context, 22), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout track = new LinearLayout(context);
        track.setOrientation(HORIZONTAL);
        track.setBackground(V13Ui.panel(context, 0xFFF4EDE2, 0, 0, 6));
        int percent = maximum <= 0L ? 0
                : (int) Math.round(value * 100d / maximum);
        View bar = new View(context);
        bar.setBackground(V13Ui.panel(context, fill, 0, 0, 6));
        track.addView(bar, new LinearLayout.LayoutParams(
                0, V13Ui.dp(context, 10), Math.max(0, percent)));
        track.addView(new View(context), new LinearLayout.LayoutParams(
                0, V13Ui.dp(context, 10), Math.max(0, 100 - percent)));
        line.addView(track, new LinearLayout.LayoutParams(
                0, V13Ui.dp(context, 10), 1f));

        TextView amount = V13Ui.text(context, compactMoney(value),
                11, CartoonStyle.MUTED, false);
        amount.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        line.addView(amount, new LinearLayout.LayoutParams(
                V13Ui.dp(context, 74), ViewGroup.LayoutParams.WRAP_CONTENT));
        return line;
    }

    private void addCategoryRanking() {
        LinearLayout card = V13Ui.card(context, CartoonStyle.SOFT_PEACH);
        card.addView(V13Ui.text(context, "支出分类排行", 17, CartoonStyle.INK, true));
        card.addView(V13Ui.gap(context, 8));
        List<StatisticsRules.CategoryShare> categories = snapshot.categories;
        if (categories.isEmpty()) {
            card.addView(V13Ui.text(context,
                    "本月暂无支出，记账后会显示分类占比。",
                    13, CartoonStyle.MUTED, false));
            addView(card);
            return;
        }
        for (int index = 0; index < categories.size(); index++) {
            StatisticsRules.CategoryShare category = categories.get(index);
            card.addView(categoryRow(index + 1, category));
            if (index < categories.size() - 1) card.addView(V13Ui.gap(context, 8));
        }
        addView(card);
    }

    private LinearLayout categoryRow(int rank, StatisticsRules.CategoryShare category) {
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(VERTICAL);
        wrapper.setPadding(V13Ui.dp(context, 10), V13Ui.dp(context, 8),
                V13Ui.dp(context, 10), V13Ui.dp(context, 8));
        wrapper.setBackground(V13Ui.panel(context, CartoonStyle.SURFACE,
                0xFFE5D7C3, 1, 14));

        LinearLayout title = new LinearLayout(context);
        title.setGravity(Gravity.CENTER_VERTICAL);
        String icon = "其他".equals(category.category) ? "🧺"
                : CartoonStyle.transactionIcon(LedgerDb.TYPE_EXPENSE, category.category);
        title.addView(V13Ui.text(context,
                rank + ". " + icon + "  " + category.category,
                14, CartoonStyle.INK, true), weighted());
        title.addView(V13Ui.text(context,
                money(category.totalCents) + " · " + category.percent + "%",
                13, CartoonStyle.EXPENSE, true));
        wrapper.addView(title);
        wrapper.addView(V13Ui.gap(context, 6));
        wrapper.addView(percentBar(category.percent, CartoonStyle.SOFT_PEACH),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(context, 9)));
        return wrapper;
    }

    private LinearLayout percentBar(int percent, int fill) {
        int value = Math.max(0, Math.min(100, percent));
        LinearLayout track = new LinearLayout(context);
        track.setOrientation(HORIZONTAL);
        track.setBackground(V13Ui.panel(context, 0xFFF4EDE2, 0, 0, 6));
        View bar = new View(context);
        bar.setBackground(V13Ui.panel(context, fill, 0, 0, 6));
        track.addView(bar, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, value));
        track.addView(new View(context), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 100 - value));
        return track;
    }

    private String money(long cents) {
        return V13Ui.money(currency, cents);
    }

    private String signedMoney(long cents) {
        return (cents > 0L ? "+" : cents < 0L ? "−" : "")
                + money(Math.abs(cents));
    }

    private String compactMoney(long cents) {
        double amount = cents / 100d;
        if (Math.abs(amount) >= 10_000d) {
            return String.format(Locale.ROOT, "%.1f万", amount / 10_000d);
        }
        if (Math.abs(amount) >= 1_000d) {
            return String.format(Locale.ROOT, "%.1fk", amount / 1_000d);
        }
        return BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString();
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private View horizontalGap(int dp) {
        View view = new View(context);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                V13Ui.dp(context, dp), 1));
        return view;
    }
}
