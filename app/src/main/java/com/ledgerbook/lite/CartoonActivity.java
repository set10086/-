package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CartoonActivity extends Activity {
    private static final int SCREEN_HOME = 0;
    private static final int SCREEN_TRANSACTIONS = 1;
    private static final int SCREEN_ACCOUNTS = 2;
    private static final int SCREEN_STATS = 3;

    private final TextView[] navItems = new TextView[4];
    private final List<LedgerDb.Ledger> ledgers = new ArrayList<>();

    private LedgerDb db;
    private LinearLayout content;
    private TextView ledgerButton;
    private long currentLedgerId = -1L;
    private int currentScreen = SCREEN_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(CartoonStyle.CREAM_YELLOW);
        getWindow().setNavigationBarColor(CartoonStyle.BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        db = new LedgerDb(this);
        db.ensureDefaults();
        buildShell();
        reloadLedgers(-1L);
    }

    @Override
    protected void onDestroy() {
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CartoonStyle.BACKGROUND);

        LinearLayout headerWrap = new LinearLayout(this);
        headerWrap.setPadding(dp(12), dp(10), dp(12), dp(4));
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(12), dp(12));
        header.setBackground(panelDrawable(CartoonStyle.CREAM_YELLOW, CartoonStyle.OUTLINE, 2,
                CartoonStyle.CARD_RADIUS_DP));
        header.setElevation(dp(2));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("✏️  蜡笔小账本", 21, CartoonStyle.INK, true);
        TextView subtitle = text("把每一笔小日子都收好", 12, CartoonStyle.MUTED, false);
        brand.addView(title);
        brand.addView(subtitle);
        header.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ledgerButton = pill("选择账本", CartoonStyle.SURFACE, CartoonStyle.INK);
        ledgerButton.setOnClickListener(v -> showLedgerPicker());
        header.addView(ledgerButton, new LinearLayout.LayoutParams(dp(126), dp(44)));
        header.addView(gapH(dp(7)));

        TextView addLedger = pill("＋", CartoonStyle.PEACH, CartoonStyle.INK);
        addLedger.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        addLedger.setContentDescription("新增账本");
        addLedger.setOnClickListener(v -> showAddLedgerDialog());
        header.addView(addLedger, new LinearLayout.LayoutParams(dp(48), dp(44)));

        headerWrap.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(headerWrap);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout navWrap = new LinearLayout(this);
        navWrap.setPadding(dp(12), dp(4), dp(12), dp(10));
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(6), dp(5), dp(6), dp(5));
        nav.setBackground(panelDrawable(CartoonStyle.SURFACE, 0xFFE8D9C3, 1,
                CartoonStyle.CARD_RADIUS_DP));
        nav.setElevation(dp(5));
        addNavItem(nav, 0, "🏡", "首页");
        addNavItem(nav, 1, "🧾", "账单");
        addNavItem(nav, 2, "👛", "账户");
        addNavItem(nav, 3, "📊", "统计");
        navWrap.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70)));
        root.addView(navWrap);

        setContentView(root);
    }

    private void addNavItem(LinearLayout parent, int index, String icon, String label) {
        TextView item = text(icon + "\n" + label, 13, CartoonStyle.MUTED, true);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(4), dp(4), dp(4), dp(4));
        item.setLineSpacing(0f, 0.92f);
        item.setOnClickListener(v -> {
            currentScreen = index;
            showScreen(index);
        });
        navItems[index] = item;
        parent.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
    }

    private void reloadLedgers(long selectId) {
        ledgers.clear();
        ledgers.addAll(db.getLedgers());
        int target = 0;
        for (int i = 0; i < ledgers.size(); i++) {
            LedgerDb.Ledger ledger = ledgers.get(i);
            if (ledger.id == selectId || (selectId < 0 && ledger.id == currentLedgerId)) {
                target = i;
                break;
            }
        }
        if (!ledgers.isEmpty()) {
            currentLedgerId = ledgers.get(target).id;
            updateLedgerButton();
            showScreen(currentScreen);
        }
    }

    private void updateLedgerButton() {
        for (LedgerDb.Ledger ledger : ledgers) {
            if (ledger.id == currentLedgerId) {
                ledgerButton.setText("📒 " + shorten(ledger.name, 7));
                return;
            }
        }
    }

    private void showLedgerPicker() {
        String[] names = new String[ledgers.size()];
        int checked = 0;
        for (int i = 0; i < ledgers.size(); i++) {
            LedgerDb.Ledger ledger = ledgers.get(i);
            names[i] = "📒  " + ledger.name + "  ·  " + ledger.currency;
            if (ledger.id == currentLedgerId) checked = i;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("切换小账本")
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    currentLedgerId = ledgers.get(which).id;
                    updateLedgerButton();
                    d.dismiss();
                    showScreen(currentScreen);
                })
                .setNegativeButton("取消", null)
                .create();
        showStyledDialog(dialog);
    }

    private void showScreen(int screen) {
        if (currentLedgerId < 0) return;
        content.animate().cancel();
        content.removeAllViews();
        updateNavigation(screen);
        if (screen == SCREEN_HOME) {
            showHome();
        } else if (screen == SCREEN_TRANSACTIONS) {
            showTransactions();
        } else if (screen == SCREEN_ACCOUNTS) {
            showAccounts();
        } else {
            showStats();
        }
        content.setAlpha(0f);
        content.setTranslationY(dp(8));
        content.animate().alpha(1f).translationY(0f).setDuration(180).start();
    }

    private void updateNavigation(int active) {
        int[] tints = {CartoonStyle.SOFT_YELLOW, CartoonStyle.SOFT_PEACH,
                CartoonStyle.SOFT_SKY, CartoonStyle.SOFT_LAVENDER};
        for (int i = 0; i < navItems.length; i++) {
            TextView item = navItems[i];
            if (i == active) {
                item.setTextColor(CartoonStyle.INK);
                item.setBackground(panelDrawable(tints[i], CartoonStyle.OUTLINE, 1,
                        CartoonStyle.BUTTON_RADIUS_DP));
            } else {
                item.setTextColor(CartoonStyle.MUTED);
                item.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    private void showHome() {
        addGreeting("嗨，今天也把小账记清楚吧", "每一笔记录，都是生活的小脚印 ✨");
        LedgerDb.Summary summary = db.getCurrentMonthSummary(currentLedgerId);
        long assets = db.getNetAssets(currentLedgerId);
        long balance = summary.incomeCents - summary.expenseCents;

        LinearLayout hero = card(CartoonStyle.SOFT_YELLOW);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout heroTop = new LinearLayout(this);
        heroTop.setOrientation(LinearLayout.HORIZONTAL);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);
        heroTop.addView(sticker("🐷", CartoonStyle.PEACH), new LinearLayout.LayoutParams(dp(64), dp(64)));
        heroTop.addView(gapH(dp(14)));
        LinearLayout heroText = new LinearLayout(this);
        heroText.setOrientation(LinearLayout.VERTICAL);
        heroText.addView(text("全部账户净资产", 13, CartoonStyle.MUTED, true));
        heroText.addView(text(formatMoney(assets), 27, CartoonStyle.INK, true));
        heroText.addView(text("本月结余 " + signedMoney(balance), 13,
                balance >= 0 ? CartoonStyle.INCOME : CartoonStyle.EXPENSE, true));
        heroTop.addView(heroText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        hero.addView(heroTop);
        content.addView(hero);
        content.addView(gapV(dp(12)));

        LinearLayout metricRow = new LinearLayout(this);
        metricRow.setOrientation(LinearLayout.HORIZONTAL);
        metricRow.addView(metricCard("🌱 本月收入", formatMoney(summary.incomeCents),
                CartoonStyle.SOFT_GREEN, CartoonStyle.INCOME),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        metricRow.addView(gapH(dp(10)));
        metricRow.addView(metricCard("🍑 本月支出", formatMoney(summary.expenseCents),
                CartoonStyle.SOFT_PEACH, CartoonStyle.EXPENSE),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(metricRow);

        content.addView(gapV(dp(20)));
        addSectionTitle("快速记一笔", "点一下，三步完成");
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(actionTile("🍜", "记支出", CartoonStyle.SOFT_PEACH, LedgerDb.TYPE_EXPENSE),
                new LinearLayout.LayoutParams(0, dp(92), 1f));
        actions.addView(gapH(dp(9)));
        actions.addView(actionTile("💰", "记收入", CartoonStyle.SOFT_GREEN, LedgerDb.TYPE_INCOME),
                new LinearLayout.LayoutParams(0, dp(92), 1f));
        actions.addView(gapH(dp(9)));
        actions.addView(actionTile("↔", "转一笔", CartoonStyle.SOFT_SKY, LedgerDb.TYPE_TRANSFER),
                new LinearLayout.LayoutParams(0, dp(92), 1f));
        content.addView(actions);

        content.addView(gapV(dp(20)));
        addSectionTitle("最近的小账", "长按账单可以删除");
        List<LedgerDb.Txn> txns = db.getRecentTransactions(currentLedgerId, 8);
        if (txns.isEmpty()) {
            addEmptyState("🖍️", "还没有账单", "先记一笔，让钱的去向变清楚吧", "立即记账",
                    v -> showAddTransactionDialog(LedgerDb.TYPE_EXPENSE));
        } else {
            for (LedgerDb.Txn txn : txns) {
                content.addView(transactionCard(txn));
                content.addView(gapV(dp(9)));
            }
        }
    }

    private void showTransactions() {
        addGreeting("账单小抽屉", "收入、支出和转账都整整齐齐放在这里");
        TextView add = wideButton("＋  记一笔新账", CartoonStyle.PEACH);
        add.setOnClickListener(v -> showAddTransactionDialog(null));
        content.addView(add);
        content.addView(gapV(dp(16)));
        List<LedgerDb.Txn> txns = db.getRecentTransactions(currentLedgerId, 300);
        if (txns.isEmpty()) {
            addEmptyState("🧾", "抽屉还是空的", "记录第一笔账单后，这里会出现时间线", "开始记账",
                    v -> showAddTransactionDialog(null));
            return;
        }
        String lastDay = "";
        for (LedgerDb.Txn txn : txns) {
            String day = new SimpleDateFormat("MM月dd日 E", Locale.CHINA).format(new Date(txn.occurredAt));
            if (!day.equals(lastDay)) {
                TextView date = text("📅  " + day, 13, CartoonStyle.MUTED, true);
                date.setPadding(dp(4), dp(8), 0, dp(8));
                content.addView(date);
                lastDay = day;
            }
            content.addView(transactionCard(txn));
            content.addView(gapV(dp(9)));
        }
    }

    private void showAccounts() {
        addGreeting("我的资产小卡包", "银行卡、现金和信用卡都可以分组收纳");
        long assets = db.getNetAssets(currentLedgerId);
        LinearLayout hero = card(CartoonStyle.SOFT_SKY);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(sticker("🏡", CartoonStyle.SKY), new LinearLayout.LayoutParams(dp(58), dp(58)));
        row.addView(gapH(dp(12)));
        LinearLayout values = new LinearLayout(this);
        values.setOrientation(LinearLayout.VERTICAL);
        values.addView(text("当前账本净资产", 13, CartoonStyle.MUTED, true));
        values.addView(text(formatMoney(assets), 25, CartoonStyle.INK, true));
        row.addView(values, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        hero.addView(row);
        content.addView(hero);
        content.addView(gapV(dp(12)));

        TextView add = wideButton("＋  新建一张账户卡", CartoonStyle.SKY);
        add.setOnClickListener(v -> showAddAccountDialog());
        content.addView(add);
        content.addView(gapV(dp(18)));

        List<LedgerDb.Account> accounts = db.getAccounts(currentLedgerId);
        if (accounts.isEmpty()) {
            addEmptyState("👛", "还没有账户卡", "先建立现金或银行卡账户，再开始记账", "新建账户",
                    v -> showAddAccountDialog());
            return;
        }
        String lastGroup = null;
        for (LedgerDb.Account account : accounts) {
            String group = account.groupName == null || account.groupName.trim().isEmpty()
                    ? "未分组" : account.groupName;
            if (!group.equals(lastGroup)) {
                addSectionTitle(group, "");
                lastGroup = group;
            }
            content.addView(accountCard(account));
            content.addView(gapV(dp(10)));
        }
    }

    private void showStats() {
        addGreeting("本月回顾", "看看钱都去了哪里，也看看自己攒下了多少");
        LedgerDb.Summary summary = db.getCurrentMonthSummary(currentLedgerId);
        long balance = summary.incomeCents - summary.expenseCents;

        LinearLayout balanceCard = card(CartoonStyle.SOFT_LAVENDER);
        balanceCard.addView(text("🎈  本月结余", 14, CartoonStyle.MUTED, true));
        balanceCard.addView(text(signedMoney(balance), 30,
                balance >= 0 ? CartoonStyle.INCOME : CartoonStyle.EXPENSE, true));
        balanceCard.addView(text(balance >= 0 ? "做得不错，给未来留下一点小惊喜" : "这个月花得有点多，下个月慢慢调整",
                13, CartoonStyle.MUTED, false));
        content.addView(balanceCard);
        content.addView(gapV(dp(12)));

        LinearLayout metricRow = new LinearLayout(this);
        metricRow.setOrientation(LinearLayout.HORIZONTAL);
        metricRow.addView(metricCard("收入", formatMoney(summary.incomeCents), CartoonStyle.SOFT_GREEN,
                CartoonStyle.INCOME), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        metricRow.addView(gapH(dp(9)));
        metricRow.addView(metricCard("支出", formatMoney(summary.expenseCents), CartoonStyle.SOFT_PEACH,
                CartoonStyle.EXPENSE), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(metricRow);
        content.addView(gapV(dp(20)));

        addSectionTitle("支出分类", "条形越长，花得越多");
        List<LedgerDb.CategoryTotal> totals = db.getCurrentMonthExpenseCategories(currentLedgerId);
        if (totals.isEmpty()) {
            addEmptyState("📊", "还没有统计数据", "记下本月的支出后，就能看到分类排行", null, null);
            return;
        }
        long max = totals.get(0).totalCents;
        int[] barColors = {CartoonStyle.PEACH, CartoonStyle.SKY, CartoonStyle.AVOCADO,
                CartoonStyle.LAVENDER, CartoonStyle.CREAM_YELLOW};
        for (int i = 0; i < totals.size(); i++) {
            LedgerDb.CategoryTotal total = totals.get(i);
            content.addView(categoryBar(total, max, barColors[i % barColors.length]));
            content.addView(gapV(dp(9)));
        }
    }

    private LinearLayout metricCard(String label, String value, int background, int valueColor) {
        LinearLayout card = card(background);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.addView(text(label, 12, CartoonStyle.MUTED, true));
        TextView amount = text(value, 18, valueColor, true);
        amount.setPadding(0, dp(4), 0, 0);
        card.addView(amount);
        return card;
    }

    private TextView actionTile(String icon, String label, int background, String type) {
        TextView tile = text(icon + "\n" + label, 16, CartoonStyle.INK, true);
        tile.setGravity(Gravity.CENTER);
        tile.setLineSpacing(dp(2), 1f);
        tile.setBackground(panelDrawable(background, CartoonStyle.OUTLINE, 1,
                CartoonStyle.CARD_RADIUS_DP));
        tile.setElevation(dp(1));
        tile.setOnClickListener(v -> {
            press(v);
            showAddTransactionDialog(type);
        });
        return tile;
    }

    private LinearLayout transactionCard(LedgerDb.Txn txn) {
        LinearLayout card = card(CartoonStyle.SURFACE);
        card.setLongClickable(true);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        String icon = CartoonStyle.transactionIcon(txn.type, txn.category);
        row.addView(sticker(icon, CartoonStyle.transactionTint(txn.type)),
                new LinearLayout.LayoutParams(dp(54), dp(54)));
        row.addView(gapH(dp(12)));

        LinearLayout middle = new LinearLayout(this);
        middle.setOrientation(LinearLayout.VERTICAL);
        middle.addView(text(txn.category, 16, CartoonStyle.INK, true));
        String accounts = txn.accountName;
        if (LedgerDb.TYPE_TRANSFER.equals(txn.type)) accounts += " → " + txn.toAccountName;
        middle.addView(text(accounts + " · " + formatTime(txn.occurredAt), 12, CartoonStyle.MUTED, false));
        String meta = buildTxnMeta(txn);
        if (!meta.isEmpty()) middle.addView(text(meta, 11, CartoonStyle.MUTED, false));
        if (txn.note != null && !txn.note.trim().isEmpty()) {
            middle.addView(text(txn.note, 12, CartoonStyle.INK, false));
        }
        row.addView(middle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int color = CartoonStyle.transactionColor(txn.type);
        String prefix = LedgerDb.TYPE_EXPENSE.equals(txn.type) ? "−"
                : LedgerDb.TYPE_INCOME.equals(txn.type) ? "+" : "";
        TextView amount = text(prefix + formatMoney(txn.amountCents), 16, color, true);
        amount.setGravity(Gravity.END);
        row.addView(amount);
        card.addView(row);
        card.setOnLongClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("擦掉这笔账？")
                    .setMessage("删除后会自动恢复相关账户余额。")
                    .setNegativeButton("先留着", null)
                    .setPositiveButton("删除", (d, which) -> {
                        try {
                            db.deleteTransaction(txn.id);
                            celebrate("账单已删除，余额也恢复啦");
                            showScreen(currentScreen);
                        } catch (RuntimeException error) {
                            toast(error.getMessage());
                        }
                    }).create();
            showStyledDialog(dialog);
            return true;
        });
        return card;
    }

    private LinearLayout accountCard(LedgerDb.Account account) {
        LinearLayout card = card(CartoonStyle.accountTint(account.type));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(sticker(CartoonStyle.accountIcon(account.type), CartoonStyle.SURFACE),
                new LinearLayout.LayoutParams(dp(56), dp(56)));
        row.addView(gapH(dp(12)));
        LinearLayout middle = new LinearLayout(this);
        middle.setOrientation(LinearLayout.VERTICAL);
        middle.addView(text(account.name, 17, CartoonStyle.INK, true));
        middle.addView(text(account.type + (account.note.trim().isEmpty() ? "" : " · " + account.note),
                12, CartoonStyle.MUTED, false));
        row.addView(middle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        int balanceColor = account.balanceCents < 0 ? CartoonStyle.EXPENSE : CartoonStyle.INK;
        row.addView(text(formatMoney(account.balanceCents), 17, balanceColor, true));
        card.addView(row);
        return card;
    }

    private LinearLayout categoryBar(LedgerDb.CategoryTotal total, long max, int color) {
        LinearLayout card = card(CartoonStyle.SURFACE);
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.HORIZONTAL);
        title.addView(text(CartoonStyle.transactionIcon(LedgerDb.TYPE_EXPENSE, total.category)
                + "  " + total.category, 15, CartoonStyle.INK, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        title.addView(text(formatMoney(total.totalCents), 14, CartoonStyle.EXPENSE, true));
        card.addView(title);
        card.addView(gapV(dp(8)));
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(panelDrawable(0xFFF3EBDD, 0x00FFFFFF, 0, 8));
        float ratio = max <= 0 ? 0f : Math.max(0.06f, Math.min(1f, (float) total.totalCents / (float) max));
        View fill = new View(this);
        fill.setBackground(panelDrawable(color, 0x00FFFFFF, 0, 8));
        track.addView(fill, new LinearLayout.LayoutParams(0, dp(11), ratio));
        track.addView(new View(this), new LinearLayout.LayoutParams(0, dp(11), 1f - ratio));
        card.addView(track, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(11)));
        return card;
    }

    private void addGreeting(String title, String subtitle) {
        TextView heading = text(title, 23, CartoonStyle.INK, true);
        heading.setPadding(dp(2), 0, dp(2), 0);
        content.addView(heading);
        TextView caption = text(subtitle, 13, CartoonStyle.MUTED, false);
        caption.setPadding(dp(2), dp(3), dp(2), 0);
        content.addView(caption);
        content.addView(gapV(dp(16)));
    }

    private void addSectionTitle(String title, String subtitle) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.BOTTOM);
        line.setPadding(dp(2), 0, dp(2), dp(9));
        line.addView(text(title, 17, CartoonStyle.INK, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (subtitle != null && !subtitle.isEmpty()) {
            line.addView(text(subtitle, 11, CartoonStyle.MUTED, false));
        }
        content.addView(line);
    }

    private void addEmptyState(String icon, String title, String message, String buttonLabel,
                               View.OnClickListener listener) {
        LinearLayout card = card(CartoonStyle.SURFACE);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(20), dp(24), dp(20), dp(24));
        TextView emoji = text(icon, 38, CartoonStyle.INK, false);
        emoji.setGravity(Gravity.CENTER);
        card.addView(emoji);
        card.addView(gapV(dp(6)));
        TextView heading = text(title, 17, CartoonStyle.INK, true);
        heading.setGravity(Gravity.CENTER);
        card.addView(heading);
        TextView desc = text(message, 13, CartoonStyle.MUTED, false);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(dp(14), dp(6), dp(14), dp(12));
        card.addView(desc);
        if (buttonLabel != null && listener != null) {
            TextView button = pill(buttonLabel, CartoonStyle.CREAM_YELLOW, CartoonStyle.INK);
            button.setOnClickListener(listener);
            card.addView(button, new LinearLayout.LayoutParams(dp(130), dp(44)));
        }
        content.addView(card);
    }

    private void showAddLedgerDialog() {
        LinearLayout form = form();
        EditText name = input("例如：家庭账本", InputType.TYPE_CLASS_TEXT);
        Spinner currency = spinner(new String[]{"CNY", "JPY", "USD", "EUR", "GBP", "HKD"});
        addField(form, "账本名称", name);
        addField(form, "本位币", currency);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("新建一本小账本")
                .setView(wrapScroll(form))
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            styleDialogButtons(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    long id = db.addLedger(name.getText().toString(), currency.getSelectedItem().toString());
                    dialog.dismiss();
                    reloadLedgers(id);
                    celebrate("新账本已经准备好啦");
                } catch (RuntimeException error) {
                    toast(error.getMessage());
                }
            });
        });
        showStyledDialog(dialog);
    }

    private void showAddAccountDialog() {
        LinearLayout form = form();
        EditText name = input("例如：工资卡", InputType.TYPE_CLASS_TEXT);
        Spinner type = spinner(new String[]{"现金账户", "储蓄账户", "信用卡账户", "投资账户", "其他账户"});
        EditText group = input("例如：银行卡", InputType.TYPE_CLASS_TEXT);
        EditText balance = input("当前余额或信用卡欠款", InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        EditText note = input("写点备注（可选）", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        addField(form, "账户名称", name);
        addField(form, "账户类型", type);
        addField(form, "放进哪个分组", group);
        addField(form, "初始余额", balance);
        addField(form, "备注", note);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("新建账户卡")
                .setView(wrapScroll(form))
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            styleDialogButtons(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    long cents = balance.getText().toString().trim().isEmpty()
                            ? 0L : parseCents(balance.getText().toString());
                    db.addAccount(currentLedgerId, name.getText().toString(), type.getSelectedItem().toString(),
                            group.getText().toString(), cents, note.getText().toString());
                    dialog.dismiss();
                    celebrate("账户卡已经收进卡包");
                    showScreen(currentScreen);
                } catch (RuntimeException error) {
                    toast(error.getMessage());
                }
            });
        });
        showStyledDialog(dialog);
    }

    private void showAddTransactionDialog(String presetType) {
        List<LedgerDb.Account> accounts = db.getAccounts(currentLedgerId);
        if (accounts.isEmpty()) {
            toast("请先创建账户");
            return;
        }
        LinearLayout form = form();
        Spinner type = spinner(new String[]{"支出", "收入", "转账"});
        if (LedgerDb.TYPE_INCOME.equals(presetType)) type.setSelection(1);
        if (LedgerDb.TYPE_TRANSFER.equals(presetType)) type.setSelection(2);
        EditText amount = input("实际支付或收到的金额", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText discount = input("优惠金额（可选）", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText category = input("例如：餐饮", InputType.TYPE_CLASS_TEXT);
        category.setText(LedgerDb.TYPE_INCOME.equals(presetType) ? "工资"
                : LedgerDb.TYPE_TRANSFER.equals(presetType) ? "账户转账" : "餐饮");
        Spinner from = accountSpinner(accounts);
        Spinner to = accountSpinner(accounts);
        EditText occurred = input("yyyy-MM-dd HH:mm", InputType.TYPE_CLASS_DATETIME);
        occurred.setText(formatDate(System.currentTimeMillis()));
        EditText bookkeeper = input("记账人", InputType.TYPE_CLASS_TEXT);
        bookkeeper.setText("本人");
        EditText tags = input("多个标签用逗号分隔", InputType.TYPE_CLASS_TEXT);
        CheckBox reimbursable = checkbox("这笔钱需要报销", false);
        CheckBox includeBudget = checkbox("计入预算统计", true);
        EditText note = input("写点备注（可选）", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        addField(form, "账单类型", type);
        addField(form, "金额", amount);
        addField(form, "优惠", discount);
        addField(form, "分类", category);
        addField(form, "账户 / 转出账户", from);
        TextView toLabel = formLabel("转入账户");
        form.addView(toLabel);
        form.addView(to);
        addField(form, "发生时间", occurred);
        addField(form, "记账人", bookkeeper);
        addField(form, "标签", tags);
        form.addView(reimbursable);
        form.addView(includeBudget);
        addField(form, "备注", note);

        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean transfer = position == 2;
                toLabel.setVisibility(transfer ? View.VISIBLE : View.GONE);
                to.setVisibility(transfer ? View.VISIBLE : View.GONE);
                if (position == 1) category.setText("工资");
                if (position == 2) category.setText("账户转账");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        boolean initiallyTransfer = type.getSelectedItemPosition() == 2;
        toLabel.setVisibility(initiallyTransfer ? View.VISIBLE : View.GONE);
        to.setVisibility(initiallyTransfer ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("记一笔小账")
                .setView(wrapScroll(form))
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            styleDialogButtons(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    long cents = parseCents(amount.getText().toString());
                    long discountCents = discount.getText().toString().trim().isEmpty()
                            ? 0L : parseCents(discount.getText().toString());
                    String dbType = type.getSelectedItemPosition() == 0 ? LedgerDb.TYPE_EXPENSE
                            : type.getSelectedItemPosition() == 1 ? LedgerDb.TYPE_INCOME
                            : LedgerDb.TYPE_TRANSFER;
                    LedgerDb.Account source = (LedgerDb.Account) from.getSelectedItem();
                    Long targetId = null;
                    if (LedgerDb.TYPE_TRANSFER.equals(dbType)) {
                        LedgerDb.Account target = (LedgerDb.Account) to.getSelectedItem();
                        targetId = target.id;
                    }
                    db.addTransaction(currentLedgerId, dbType, category.getText().toString(), cents,
                            source.id, targetId, bookkeeper.getText().toString(), tags.getText().toString(),
                            reimbursable.isChecked(), discountCents, includeBudget.isChecked(),
                            note.getText().toString(), parseDate(occurred.getText().toString()));
                    dialog.dismiss();
                    celebrate("记好啦，今天也很有条理");
                    showScreen(currentScreen);
                } catch (RuntimeException error) {
                    toast(error.getMessage());
                }
            });
        });
        showStyledDialog(dialog);
    }

    private Spinner accountSpinner(List<LedgerDb.Account> accounts) {
        Spinner spinner = new Spinner(this);
        spinner.setBackground(panelDrawable(CartoonStyle.SURFACE, 0xFFE4D5C2, 1, 14));
        ArrayAdapter<LedgerDb.Account> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, accounts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPadding(dp(8), 0, dp(8), 0);
        return spinner;
    }

    private Spinner spinner(String[] items) {
        Spinner spinner = new Spinner(this);
        spinner.setBackground(panelDrawable(CartoonStyle.SURFACE, 0xFFE4D5C2, 1, 14));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPadding(dp(8), 0, dp(8), 0);
        return spinner;
    }

    private CheckBox checkbox(String label, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextColor(CartoonStyle.INK);
        box.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        box.setChecked(checked);
        box.setPadding(0, dp(5), 0, dp(2));
        return box;
    }

    private LinearLayout form() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(6), dp(20), dp(18));
        form.setBackgroundColor(CartoonStyle.BACKGROUND);
        return form;
    }

    private void addField(LinearLayout form, String label, View input) {
        form.addView(formLabel(label));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, input instanceof EditText
                ? ViewGroup.LayoutParams.WRAP_CONTENT : dp(48));
        form.addView(input, params);
    }

    private TextView formLabel(String value) {
        TextView label = text(value, 13, CartoonStyle.MUTED, true);
        label.setPadding(dp(2), dp(12), 0, dp(5));
        return label;
    }

    private EditText input(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(0xFFAAA097);
        input.setTextColor(CartoonStyle.INK);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        input.setInputType(inputType);
        input.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        input.setPadding(dp(13), dp(10), dp(13), dp(10));
        input.setBackground(panelDrawable(CartoonStyle.SURFACE, 0xFFE4D5C2, 1, 14));
        return input;
    }

    private ScrollView wrapScroll(View child) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(CartoonStyle.BACKGROUND);
        scroll.addView(child);
        return scroll;
    }

    private void showStyledDialog(AlertDialog dialog) {
        dialog.setOnShowListener(dialog.getOnShowListener());
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(panelDrawable(CartoonStyle.BACKGROUND, CartoonStyle.OUTLINE, 1,
                    CartoonStyle.CARD_RADIUS_DP));
        }
        styleDialogButtons(dialog);
    }

    private void styleDialogButtons(AlertDialog dialog) {
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positive != null) {
            positive.setTextColor(CartoonStyle.EXPENSE);
            positive.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        if (negative != null) negative.setTextColor(CartoonStyle.MUTED);
    }

    private LinearLayout card(int background) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(panelDrawable(background, 0xFFE8D9C3, 1, CartoonStyle.CARD_RADIUS_DP));
        card.setElevation(dp(1));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private TextView sticker(String emoji, int background) {
        TextView view = text(emoji, 27, CartoonStyle.INK, false);
        view.setGravity(Gravity.CENTER);
        view.setBackground(panelDrawable(background, CartoonStyle.OUTLINE, 1, 18));
        return view;
    }

    private TextView pill(String value, int background, int foreground) {
        TextView view = text(value, 13, foreground, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), dp(6), dp(10), dp(6));
        view.setBackground(panelDrawable(background, CartoonStyle.OUTLINE, 1,
                CartoonStyle.BUTTON_RADIUS_DP));
        view.setClickable(true);
        return view;
    }

    private TextView wideButton(String value, int background) {
        TextView button = pill(value, background, CartoonStyle.INK);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        button.setElevation(dp(1));
        button.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        return button;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable panelDrawable(int fill, int stroke, int strokeDp, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), stroke);
        return drawable;
    }

    private View gapV(int height) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(1), height));
        return view;
    }

    private View gapH(int width) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(width, dp(1)));
        return view;
    }

    private void press(View view) {
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(70)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(90).start())
                .start();
    }

    private String buildTxnMeta(LedgerDb.Txn txn) {
        StringBuilder meta = new StringBuilder();
        if (txn.bookkeeper != null && !txn.bookkeeper.trim().isEmpty()) meta.append(txn.bookkeeper);
        if (txn.tags != null && !txn.tags.trim().isEmpty()) appendDot(meta).append("#").append(txn.tags);
        if (txn.reimbursable) appendDot(meta).append("待报销");
        if (!txn.includeBudget) appendDot(meta).append("不计预算");
        if (txn.discountCents > 0) appendDot(meta).append("省了 ").append(formatMoney(txn.discountCents));
        return meta.toString();
    }

    private StringBuilder appendDot(StringBuilder builder) {
        if (builder.length() > 0) builder.append(" · ");
        return builder;
    }

    private String currentCurrency() {
        for (LedgerDb.Ledger ledger : ledgers) {
            if (ledger.id == currentLedgerId) return ledger.currency;
        }
        return "CNY";
    }

    private String formatMoney(long cents) {
        return currentCurrency() + " " + BigDecimal.valueOf(cents, 2).toPlainString();
    }

    private String signedMoney(long cents) {
        return (cents >= 0 ? "+" : "−") + formatMoney(Math.abs(cents));
    }

    private long parseCents(String raw) {
        String clean = raw == null ? "" : raw.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("请输入金额");
        try {
            BigDecimal amount = new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
            return amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException | NumberFormatException error) {
            throw new IllegalArgumentException("金额格式不正确");
        }
    }

    private long parseDate(String raw) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        format.setLenient(false);
        try {
            Date date = format.parse(raw == null ? "" : raw.trim());
            if (date == null) throw new ParseException("empty", 0);
            return date.getTime();
        } catch (ParseException error) {
            throw new IllegalArgumentException("时间格式应为 yyyy-MM-dd HH:mm");
        }
    }

    private String formatDate(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(timestamp));
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(timestamp));
    }

    private String shorten(String value, int max) {
        if (value == null) return "账本";
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void celebrate(String message) {
        Toast.makeText(this, "✨ " + message, Toast.LENGTH_SHORT).show();
    }

    private void toast(String message) {
        Toast.makeText(this, message == null ? "操作失败" : message, Toast.LENGTH_SHORT).show();
    }
}
