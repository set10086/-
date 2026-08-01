package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class LedgerV13Activity extends Activity {
    private static final String PREFS = "ledgerbook_v13";
    private static final String PREF_LEDGER = "current_ledger_id";
    private static final int HOME = 0;
    private static final int CALENDAR = 1;
    private static final int ACCOUNTS = 2;
    private static final int STATS = 3;

    private final TextView[] navItems = new TextView[4];
    private final List<LedgerDb.Ledger> ledgers = new ArrayList<>();
    private LedgerDb db;
    private FrameLayout root;
    private FrameLayout pageContainer;
    private LinearLayout headerWrap;
    private LinearLayout navWrap;
    private TextView ledgerButton;
    private TextView billButton;
    private TextView quickAdd;
    private View shade;
    private View openDrawer;
    private boolean openDrawerLeft;
    private LedgerDrawerView ledgerDrawer;
    private BillDrawerView billDrawer;
    private CalendarPageView calendarPage;
    private long currentLedgerId = -1L;
    private int currentPage = HOME;
    private int bottomInset;

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
        loadInitialLedger();
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (openDrawer != null) {
            closeDrawer();
            return;
        }
        super.onBackPressed();
    }

    private void buildShell() {
        root = new FrameLayout(this);
        root.setBackgroundColor(CartoonStyle.BACKGROUND);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(CartoonStyle.BACKGROUND);
        root.addView(main, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        headerWrap = new LinearLayout(this);
        headerWrap.setPadding(V13Ui.dp(this, 10), V13Ui.dp(this, 8),
                V13Ui.dp(this, 10), V13Ui.dp(this, 5));
        FrameLayout header = new FrameLayout(this);
        header.setBackground(V13Ui.panel(this, CartoonStyle.CREAM_YELLOW,
                CartoonStyle.OUTLINE, 1, 23));

        ledgerButton = V13Ui.button(this, "📒 账本 ▾", CartoonStyle.SOFT_YELLOW);
        ledgerButton.setGravity(Gravity.CENTER_VERTICAL);
        ledgerButton.setMaxLines(1);
        ledgerButton.setOnClickListener(v -> openLedgerDrawer());
        FrameLayout.LayoutParams ledgerParams = new FrameLayout.LayoutParams(
                V13Ui.dp(this, 168), V13Ui.dp(this, 46), Gravity.START | Gravity.CENTER_VERTICAL);
        ledgerParams.leftMargin = V13Ui.dp(this, 8);
        header.addView(ledgerButton, ledgerParams);

        TextView search = V13Ui.button(this, "🔍", CartoonStyle.SURFACE);
        search.setContentDescription("搜索账单");
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        search.setOnClickListener(v -> TransactionSearchDialog.show(
                this, db, currentLedgerId, this::refreshAll));
        header.addView(search, new FrameLayout.LayoutParams(
                V13Ui.dp(this, 48), V13Ui.dp(this, 46), Gravity.CENTER));

        billButton = V13Ui.button(this, "🧾", CartoonStyle.SURFACE);
        billButton.setContentDescription("打开账单列表");
        billButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        billButton.setOnClickListener(v -> openBillDrawer());
        FrameLayout.LayoutParams billParams = new FrameLayout.LayoutParams(
                V13Ui.dp(this, 52), V13Ui.dp(this, 46), Gravity.END | Gravity.CENTER_VERTICAL);
        billParams.rightMargin = V13Ui.dp(this, 8);
        header.addView(billButton, billParams);

        headerWrap.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(this, 58)));
        main.addView(headerWrap);

        pageContainer = new FrameLayout(this);
        main.addView(pageContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        navWrap = new LinearLayout(this);
        navWrap.setPadding(V13Ui.dp(this, 10), V13Ui.dp(this, 4),
                V13Ui.dp(this, 10), V13Ui.dp(this, 9));
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(V13Ui.dp(this, 5), V13Ui.dp(this, 4),
                V13Ui.dp(this, 5), V13Ui.dp(this, 4));
        nav.setBackground(V13Ui.panel(this, CartoonStyle.SURFACE, 0xFFE4D5C2, 1, 23));
        addNav(nav, HOME, "🏡\n首页");
        addNav(nav, CALENDAR, "📅\n日历");
        addNav(nav, ACCOUNTS, "👛\n账户");
        addNav(nav, STATS, "📊\n统计");
        navWrap.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(this, 68)));
        main.addView(navWrap);

        quickAdd = V13Ui.text(this, "+", 34, CartoonStyle.INK, true);
        quickAdd.setGravity(Gravity.CENTER);
        quickAdd.setContentDescription("快速记账");
        quickAdd.setBackground(V13Ui.panel(this, CartoonStyle.PEACH,
                CartoonStyle.OUTLINE, 1, 29));
        quickAdd.setElevation(V13Ui.dp(this, 8));
        quickAdd.setOnClickListener(v -> {
            long timestamp = System.currentTimeMillis();
            if (currentPage == CALENDAR && calendarPage != null) {
                LocalDate selected = calendarPage.getSelectedDate();
                LocalDateTime value = LocalDateTime.of(selected, LocalTime.now());
                timestamp = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            showAddTransaction(null, timestamp);
        });
        FrameLayout.LayoutParams quickParams = new FrameLayout.LayoutParams(
                V13Ui.dp(this, 58), V13Ui.dp(this, 58), Gravity.END | Gravity.BOTTOM);
        quickParams.rightMargin = V13Ui.dp(this, 16);
        quickParams.bottomMargin = V13Ui.dp(this, 92);
        root.addView(quickAdd, quickParams);

        shade = new View(this);
        shade.setBackgroundColor(0x88000000);
        shade.setVisibility(View.GONE);
        shade.setAlpha(0f);
        shade.setOnClickListener(v -> closeDrawer());
        root.addView(shade, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            bottomInset = insets.getSystemWindowInsetBottom();
            headerWrap.setPadding(V13Ui.dp(this, 10), V13Ui.dp(this, 8) + top,
                    V13Ui.dp(this, 10), V13Ui.dp(this, 5));
            navWrap.setPadding(V13Ui.dp(this, 10), V13Ui.dp(this, 4),
                    V13Ui.dp(this, 10), V13Ui.dp(this, 9) + bottomInset);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) quickAdd.getLayoutParams();
            params.bottomMargin = V13Ui.dp(this, 92) + bottomInset;
            quickAdd.setLayoutParams(params);
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();
    }

    private void addNav(LinearLayout parent, int index, String label) {
        TextView item = V13Ui.text(this, label, 13, CartoonStyle.MUTED, true);
        item.setGravity(Gravity.CENTER);
        item.setOnClickListener(v -> {
            currentPage = index;
            showPage();
        });
        navItems[index] = item;
        parent.addView(item, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
    }

    private void loadInitialLedger() {
        ledgers.clear();
        ledgers.addAll(db.getLedgers());
        long saved = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(PREF_LEDGER, -1L);
        currentLedgerId = containsLedger(saved) ? saved : ledgers.get(0).id;
        persistLedger();
        updateLedgerButton();
        showPage();
    }

    private boolean containsLedger(long id) {
        for (LedgerDb.Ledger ledger : ledgers) if (ledger.id == id) return true;
        return false;
    }

    private void switchLedger(long ledgerId) {
        ledgers.clear();
        ledgers.addAll(db.getLedgers());
        currentLedgerId = containsLedger(ledgerId) ? ledgerId : ledgers.get(0).id;
        persistLedger();
        updateLedgerButton();
        if (calendarPage != null) calendarPage.setLedger(currentLedgerId);
        if (ledgerDrawer != null) ledgerDrawer.setCurrentLedger(currentLedgerId);
        if (billDrawer != null) billDrawer.setCurrentLedger(currentLedgerId);
        showPage();
    }

    private void persistLedger() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putLong(PREF_LEDGER, currentLedgerId).apply();
    }

    private void updateLedgerButton() {
        LedgerDb.Ledger ledger = db.getLedger(currentLedgerId);
        ledgerButton.setText(ledger == null ? "📒 账本 ▾" : "📒 " + shorten(ledger.name, 9) + " ▾");
    }

    private void showPage() {
        if (currentLedgerId <= 0L) return;
        pageContainer.removeAllViews();
        int[] fills = {CartoonStyle.SOFT_YELLOW, CartoonStyle.SOFT_PEACH,
                CartoonStyle.SOFT_SKY, CartoonStyle.SOFT_GREEN};
        for (int index = 0; index < navItems.length; index++) {
            navItems[index].setTextColor(index == currentPage ? CartoonStyle.INK : CartoonStyle.MUTED);
            navItems[index].setBackground(index == currentPage
                    ? V13Ui.panel(this, fills[index], 0x00FFFFFF, 0, 17) : null);
        }
        if (currentPage == CALENDAR) {
            if (calendarPage == null) {
                calendarPage = new CalendarPageView(this, db, new CalendarPageView.Listener() {
                    @Override public void onQuickAdd(long occurredAt) {
                        showAddTransaction(null, occurredAt);
                    }
                    @Override public void onTransactionsChanged() {
                        refreshAll();
                    }
                });
            }
            ViewGroup parent = (ViewGroup) calendarPage.getParent();
            if (parent != null) parent.removeView(calendarPage);
            calendarPage.setLedger(currentLedgerId);
            pageContainer.addView(calendarPage, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return;
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(V13Ui.dp(this, 14), V13Ui.dp(this, 10),
                V13Ui.dp(this, 14), V13Ui.dp(this, 92));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pageContainer.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (currentPage == HOME) showHome(content);
        else if (currentPage == ACCOUNTS) showAccounts(content);
        else showStats(content);
    }

    private void showHome(LinearLayout content) {
        greeting(content, "今天也要好好记账呀", "账本、搜索和完整账单都在标题栏");
        LedgerDb.Summary summary = db.getCurrentMonthSummary(currentLedgerId);
        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.addView(metric("🌱 收入", summary.incomeCents,
                CartoonStyle.SOFT_GREEN, CartoonStyle.INCOME), weight());
        metrics.addView(horizontalGap(8));
        metrics.addView(metric("🍑 支出", summary.expenseCents,
                CartoonStyle.SOFT_PEACH, CartoonStyle.EXPENSE), weight());
        content.addView(metrics);
        content.addView(V13Ui.gap(this, 9));
        content.addView(metric("👛 净资产", db.getNetAssets(currentLedgerId),
                CartoonStyle.SOFT_YELLOW, CartoonStyle.INK));
        content.addView(V13Ui.gap(this, 18));
        section(content, "最近账单");
        List<LedgerDb.Txn> rows = db.getRecentTransactions(currentLedgerId, 10);
        if (rows.isEmpty()) {
            empty(content, "🖍️", "还没有账单", "点击右下角＋记下第一笔");
            return;
        }
        for (LedgerDb.Txn txn : rows) addTransactionCard(content, txn.id);
    }

    private void showAccounts(LinearLayout content) {
        greeting(content, "我的账户卡包", "当前账本下的现金、银行卡和其他账户");
        TextView add = V13Ui.button(this, "＋ 新增账户", CartoonStyle.SKY);
        add.setOnClickListener(v -> showAddAccountDialog());
        content.addView(add, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(this, 50)));
        content.addView(V13Ui.gap(this, 12));
        List<LedgerDb.Account> accounts = db.getAccounts(currentLedgerId);
        String group = null;
        for (LedgerDb.Account account : accounts) {
            if (!account.groupName.equals(group)) {
                group = account.groupName;
                section(content, group.isEmpty() ? "未分组" : group);
            }
            LinearLayout card = V13Ui.card(this, CartoonStyle.accountTint(account.type));
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(V13Ui.text(this, CartoonStyle.accountIcon(account.type),
                    27, CartoonStyle.INK, false), new LinearLayout.LayoutParams(
                    V13Ui.dp(this, 50), V13Ui.dp(this, 50)));
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.addView(V13Ui.text(this, account.name, 17, CartoonStyle.INK, true));
            info.addView(V13Ui.text(this, account.type + (account.note.isEmpty() ? "" : " · " + account.note),
                    12, CartoonStyle.MUTED, false));
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(V13Ui.text(this, formatMoney(account.balanceCents), 15,
                    account.balanceCents < 0 ? CartoonStyle.EXPENSE : CartoonStyle.INK, true));
            card.addView(row);
            content.addView(card);
            content.addView(V13Ui.gap(this, 8));
        }
    }

    private void showStats(LinearLayout content) {
        greeting(content, "本月统计", "当前账本的收入、支出和分类回顾");
        LedgerDb.Summary summary = db.getCurrentMonthSummary(currentLedgerId);
        content.addView(metric("✨ 本月结余", summary.incomeCents - summary.expenseCents,
                CartoonStyle.SOFT_LAVENDER, CartoonStyle.INK));
        content.addView(V13Ui.gap(this, 10));
        content.addView(metric("👛 账户净资产", db.getNetAssets(currentLedgerId),
                CartoonStyle.SOFT_YELLOW, CartoonStyle.INK));
        content.addView(V13Ui.gap(this, 18));
        section(content, "支出分类");
        List<LedgerDb.CategoryTotal> totals = db.getCurrentMonthExpenseCategories(currentLedgerId);
        if (totals.isEmpty()) {
            empty(content, "📊", "暂无统计数据", "记下支出后即可查看分类统计");
            return;
        }
        for (LedgerDb.CategoryTotal total : totals) {
            LinearLayout card = V13Ui.card(this, CartoonStyle.SURFACE);
            LinearLayout row = new LinearLayout(this);
            row.addView(V13Ui.text(this,
                    CartoonStyle.transactionIcon(LedgerDb.TYPE_EXPENSE, total.category)
                            + "  " + total.category, 15, CartoonStyle.INK, true), weight());
            row.addView(V13Ui.text(this, formatMoney(total.totalCents),
                    15, CartoonStyle.EXPENSE, true));
            card.addView(row);
            content.addView(card);
            content.addView(V13Ui.gap(this, 8));
        }
    }

    private void addTransactionCard(LinearLayout content, long transactionId) {
        LedgerDb.TxnView txn = db.getTransaction(transactionId);
        if (txn == null) return;
        TextView row = V13Ui.transactionRow(this, txn, false);
        row.setOnClickListener(v -> TransactionDetailDialog.show(this, db, txn, this::refreshAll));
        row.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this).setTitle("删除这笔账？")
                    .setMessage("删除后会恢复相关账户余额。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除", (dialog, which) -> {
                        try {
                            db.deleteTransaction(txn.id);
                            refreshAll();
                        } catch (RuntimeException error) {
                            toast(error.getMessage());
                        }
                    }).show();
            return true;
        });
        content.addView(row);
        content.addView(V13Ui.gap(this, 8));
    }

    private void openLedgerDrawer() {
        if (ledgerDrawer == null) {
            ledgerDrawer = new LedgerDrawerView(this, db, currentLedgerId,
                    new LedgerDrawerView.Listener() {
                        @Override public void onLedgerSelected(long ledgerId) { switchLedger(ledgerId); }
                        @Override public void onLedgerListChanged(long ledgerId) { switchLedger(ledgerId); }
                        @Override public void onClose() { closeDrawer(); }
                    });
        }
        ledgerDrawer.setCurrentLedger(currentLedgerId);
        showDrawer(ledgerDrawer, true, 0.84f, 340);
    }

    private void openBillDrawer() {
        if (billDrawer == null) {
            billDrawer = new BillDrawerView(this, db, currentLedgerId,
                    new BillDrawerView.Listener() {
                        @Override public void onClose() { closeDrawer(); }
                        @Override public void onFilterStateChanged(boolean active) {
                            billButton.setText(active ? "🧾 •" : "🧾");
                        }
                        @Override public void onTransactionsChanged() { refreshAll(); }
                    });
        }
        billDrawer.setCurrentLedger(currentLedgerId);
        billDrawer.refresh();
        showDrawer(billDrawer, false, 0.94f, 520);
    }

    private void showDrawer(View drawer, boolean fromLeft, float fraction, int maxDp) {
        if (openDrawer != null) closeDrawerImmediately();
        int width = Math.min((int) (getResources().getDisplayMetrics().widthPixels * fraction),
                V13Ui.dp(this, maxDp));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                width, ViewGroup.LayoutParams.MATCH_PARENT,
                fromLeft ? Gravity.START : Gravity.END);
        root.addView(drawer, params);
        openDrawer = drawer;
        openDrawerLeft = fromLeft;
        drawer.setTranslationX(fromLeft ? -width : width);
        shade.setVisibility(View.VISIBLE);
        shade.bringToFront();
        drawer.bringToFront();
        shade.animate().alpha(1f).setDuration(200L).start();
        drawer.animate().translationX(0f).setDuration(210L).start();
    }

    private void closeDrawer() {
        if (openDrawer == null) return;
        View closing = openDrawer;
        int width = closing.getWidth();
        closing.animate().translationX(openDrawerLeft ? -width : width)
                .setDuration(190L).withEndAction(() -> {
                    root.removeView(closing);
                    if (openDrawer == closing) openDrawer = null;
                }).start();
        shade.animate().alpha(0f).setDuration(180L).withEndAction(() -> {
            shade.setVisibility(View.GONE);
            shade.setAlpha(0f);
        }).start();
    }

    private void closeDrawerImmediately() {
        if (openDrawer != null) root.removeView(openDrawer);
        openDrawer = null;
        shade.setVisibility(View.GONE);
        shade.setAlpha(0f);
    }

    private void showAddTransaction(String presetType, long presetTime) {
        List<LedgerDb.Account> accounts = db.getAccounts(currentLedgerId);
        if (accounts.isEmpty()) {
            toast("请先创建账户");
            return;
        }
        long[] amountCents = {0L};
        long[] discountCents = {0L};
        String[] category = {LedgerDb.TYPE_INCOME.equals(presetType) ? "工资" : "餐饮"};
        String[] categoryIcon = {LedgerDb.TYPE_INCOME.equals(presetType) ? "💰" : "🍜"};
        LedgerDb.Account[] source = {accounts.get(0)};
        LedgerDb.Account[] target = {firstDifferent(accounts, source[0].id)};
        TextView[] targetViewRef = {null};
        long[] occurredAt = {presetTime > 0L ? presetTime : System.currentTimeMillis()};
        String[] bookkeeper = {"本人"};

        LinearLayout form = form();
        Spinner type = spinner(new String[]{"支出", "收入", "转账"});
        if (LedgerDb.TYPE_INCOME.equals(presetType)) type.setSelection(1);
        if (LedgerDb.TYPE_TRANSFER.equals(presetType)) type.setSelection(2);
        addField(form, "账单类型", type);

        TextView amount = selectField("💴  点击输入金额", "支持预加减乘除");
        addField(form, "金额", amount);
        amount.setOnClickListener(v -> AmountCalculatorDialog.show(this,
                "计算金额", amountCents[0], cents -> {
                    amountCents[0] = cents;
                    amount.setText("💴  " + formatMoney(cents));
                }));

        TextView discount = selectField("🏷️  无优惠", "点击计算优惠金额");
        addField(form, "优惠", discount);
        discount.setOnClickListener(v -> AmountCalculatorDialog.show(this,
                "计算优惠", discountCents[0], true, cents -> {
                    discountCents[0] = cents;
                    discount.setText(cents == 0L ? "🏷️  无优惠" : "🏷️  " + formatMoney(cents));
                }));

        TextView categoryView = selectField(categoryIcon[0] + "  " + category[0], "点击选择具体分类");
        addField(form, "分类", categoryView);

        TextView sourceView = selectField(accountLabel(source[0]), "点击选择账户");
        addField(form, "账户 / 转出账户", sourceView);
        sourceView.setOnClickListener(v -> AccountPickerDialog.show(this, "选择账户", accounts,
                currentCurrency(), null, account -> {
                    source[0] = account;
                    sourceView.setText(accountLabel(account));
                    if (target[0] != null && target[0].id == account.id) {
                        target[0] = firstDifferent(accounts, account.id);
                        if (targetViewRef[0] != null) {
                            targetViewRef[0].setText(target[0] == null
                                    ? "请先创建另一个账户" : accountLabel(target[0]));
                        }
                    }
                }));

        TextView targetLabel = formLabel("转入账户");
        TextView targetView = selectField(target[0] == null ? "请先创建另一个账户" : accountLabel(target[0]),
                "转入账户必须不同");
        targetViewRef[0] = targetView;
        form.addView(targetLabel);
        form.addView(targetView);
        targetView.setOnClickListener(v -> AccountPickerDialog.show(this, "选择转入账户", accounts,
                currentCurrency(), source[0].id, account -> {
                    target[0] = account;
                    targetView.setText(accountLabel(account));
                }));

        TextView timeView = selectField("🕒  " + V13Ui.dateTime(occurredAt[0]), "点击滚轮选择时间");
        addField(form, "发生时间", timeView);
        timeView.setOnClickListener(v -> WheelDateTimeDialog.show(this, occurredAt[0], timestamp -> {
            occurredAt[0] = timestamp;
            timeView.setText("🕒  " + V13Ui.dateTime(timestamp));
        }));

        TextView bookkeeperView = selectField("🙂  " + bookkeeper[0], "点击选择或新增记账人");
        addField(form, "记账人", bookkeeperView);
        bookkeeperView.setOnClickListener(v -> BookkeeperPickerDialog.show(this, value -> {
            bookkeeper[0] = value;
            bookkeeperView.setText("🙂  " + value);
        }));

        EditText tags = input("多个标签用逗号分隔", InputType.TYPE_CLASS_TEXT);
        EditText note = input("写点备注（可选）",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        CheckBox reimbursable = check("这笔钱需要报销", false);
        CheckBox budget = check("计入预算统计", true);
        addField(form, "标签", tags);
        form.addView(reimbursable);
        form.addView(budget);
        addField(form, "备注", note);

        Runnable updateType = () -> {
            int selected = type.getSelectedItemPosition();
            boolean transfer = selected == 2;
            targetLabel.setVisibility(transfer ? View.VISIBLE : View.GONE);
            targetView.setVisibility(transfer ? View.VISIBLE : View.GONE);
            String dbType = selected == 0 ? LedgerDb.TYPE_EXPENSE
                    : selected == 1 ? LedgerDb.TYPE_INCOME : LedgerDb.TYPE_TRANSFER;
            InputCatalog.Option first = InputCatalog.categories(dbType).get(0);
            category[0] = first.label;
            categoryIcon[0] = first.icon;
            categoryView.setText(first.icon + "  " + first.label);
            categoryView.setEnabled(!transfer);
            categoryView.setAlpha(transfer ? 0.65f : 1f);
        };
        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateType.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        categoryView.setOnClickListener(v -> {
            int selected = type.getSelectedItemPosition();
            String dbType = selected == 0 ? LedgerDb.TYPE_EXPENSE
                    : selected == 1 ? LedgerDb.TYPE_INCOME : LedgerDb.TYPE_TRANSFER;
            if (LedgerDb.TYPE_TRANSFER.equals(dbType)) return;
            CategoryPickerDialog.show(this, dbType, (icon, label) -> {
                categoryIcon[0] = icon;
                category[0] = label;
                categoryView.setText(icon + "  " + label);
            });
        });

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("记一笔小账")
                .setView(wrap(form)).setNegativeButton("取消", null)
                .setPositiveButton("保存", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        if (amountCents[0] <= 0L) throw new IllegalArgumentException("请先输入金额");
                        int selected = type.getSelectedItemPosition();
                        String dbType = selected == 0 ? LedgerDb.TYPE_EXPENSE
                                : selected == 1 ? LedgerDb.TYPE_INCOME : LedgerDb.TYPE_TRANSFER;
                        Long targetId = null;
                        if (LedgerDb.TYPE_TRANSFER.equals(dbType)) {
                            if (target[0] == null) throw new IllegalArgumentException("请选择转入账户");
                            if (source[0].id == target[0].id) {
                                throw new IllegalArgumentException("转入和转出账户不能相同");
                            }
                            targetId = target[0].id;
                        }
                        db.addTransaction(currentLedgerId, dbType, category[0], amountCents[0],
                                source[0].id, targetId, bookkeeper[0], tags.getText().toString(),
                                reimbursable.isChecked(), discountCents[0], budget.isChecked(),
                                note.getText().toString(), occurredAt[0]);
                        dialog.dismiss();
                        toast("记好啦");
                        refreshAll();
                    } catch (RuntimeException error) {
                        toast(error.getMessage());
                    }
                }));
        dialog.show();
    }

    private void showAddAccountDialog() {
        LinearLayout form = form();
        EditText name = input("例如：工资卡", InputType.TYPE_CLASS_TEXT);
        Spinner type = spinner(new String[]{"现金账户", "储蓄账户", "信用卡账户", "投资账户", "其他账户"});
        EditText group = input("例如：银行卡", InputType.TYPE_CLASS_TEXT);
        EditText balance = input("余额或信用卡欠款", InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        EditText note = input("备注（可选）", InputType.TYPE_CLASS_TEXT);
        addField(form, "账户名称", name);
        addField(form, "账户类型", type);
        addField(form, "分组", group);
        addField(form, "初始余额", balance);
        addField(form, "备注", note);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("新增账户")
                .setView(wrap(form)).setNegativeButton("取消", null)
                .setPositiveButton("保存", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        long cents = balance.getText().toString().trim().isEmpty()
                                ? 0L : parseCents(balance.getText().toString());
                        db.addAccount(currentLedgerId, name.getText().toString(),
                                type.getSelectedItem().toString(), group.getText().toString(),
                                cents, note.getText().toString());
                        dialog.dismiss();
                        refreshAll();
                    } catch (RuntimeException error) {
                        toast(error.getMessage());
                    }
                }));
        dialog.show();
    }

    private void refreshAll() {
        showPage();
        if (calendarPage != null) calendarPage.refresh();
        if (ledgerDrawer != null) ledgerDrawer.refresh();
        if (billDrawer != null) billDrawer.refresh();
    }

    private LedgerDb.Account firstDifferent(List<LedgerDb.Account> accounts, long excluded) {
        for (LedgerDb.Account account : accounts) if (account.id != excluded) return account;
        return null;
    }

    private String accountLabel(LedgerDb.Account account) {
        return CartoonStyle.accountIcon(account.type) + "  " + account.name + "\n"
                + account.type + " · " + formatMoney(account.balanceCents);
    }

    private LinearLayout metric(String label, long cents, int fill, int valueColor) {
        LinearLayout card = V13Ui.card(this, fill);
        card.addView(V13Ui.text(this, label, 13, CartoonStyle.MUTED, true));
        card.addView(V13Ui.text(this, formatMoney(cents), 20, valueColor, true));
        return card;
    }

    private void greeting(LinearLayout content, String title, String subtitle) {
        content.addView(V13Ui.text(this, title, 22, CartoonStyle.INK, true));
        content.addView(V13Ui.text(this, subtitle, 13, CartoonStyle.MUTED, false));
        content.addView(V13Ui.gap(this, 15));
    }

    private void section(LinearLayout content, String title) {
        TextView view = V13Ui.text(this, title, 16, CartoonStyle.INK, true);
        view.setPadding(V13Ui.dp(this, 2), V13Ui.dp(this, 5),
                V13Ui.dp(this, 2), V13Ui.dp(this, 8));
        content.addView(view);
    }

    private void empty(LinearLayout content, String icon, String title, String subtitle) {
        TextView view = V13Ui.text(this, icon + "\n" + title + "\n" + subtitle,
                16, CartoonStyle.MUTED, true);
        view.setGravity(Gravity.CENTER);
        view.setLineSpacing(0f, 1.25f);
        content.addView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(this, 180)));
    }

    private LinearLayout form() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(V13Ui.dp(this, 18), V13Ui.dp(this, 8),
                V13Ui.dp(this, 18), V13Ui.dp(this, 18));
        return form;
    }

    private ScrollView wrap(View child) {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(child, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private TextView selectField(String value, String hint) {
        TextView view = V13Ui.text(this, value + "\n" + hint, 15, CartoonStyle.INK, true);
        view.setPadding(V13Ui.dp(this, 14), V13Ui.dp(this, 10),
                V13Ui.dp(this, 14), V13Ui.dp(this, 10));
        view.setMinHeight(V13Ui.dp(this, 58));
        view.setBackground(V13Ui.panel(this, CartoonStyle.SURFACE, 0xFFE4D5C2, 1, 16));
        return view;
    }

    private EditText input(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setTextColor(CartoonStyle.INK);
        input.setHintTextColor(CartoonStyle.MUTED);
        input.setBackground(V13Ui.panel(this, CartoonStyle.SURFACE, 0xFFE4D5C2, 1, 15));
        input.setPadding(V13Ui.dp(this, 12), V13Ui.dp(this, 9),
                V13Ui.dp(this, 12), V13Ui.dp(this, 9));
        return input;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values));
        return spinner;
    }

    private CheckBox check(String label, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextColor(CartoonStyle.INK);
        box.setChecked(checked);
        return box;
    }

    private void addField(LinearLayout form, String label, View field) {
        form.addView(formLabel(label));
        form.addView(field, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private TextView formLabel(String value) {
        TextView label = V13Ui.text(this, value, 13, CartoonStyle.MUTED, true);
        label.setPadding(0, V13Ui.dp(this, 8), 0, V13Ui.dp(this, 3));
        return label;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private View horizontalGap(int dp) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(V13Ui.dp(this, dp), 1));
        return view;
    }

    private String currentCurrency() {
        LedgerDb.Ledger ledger = db.getLedger(currentLedgerId);
        return ledger == null ? "CNY" : ledger.currency;
    }

    private String formatMoney(long cents) {
        return V13Ui.money(currentCurrency(), cents);
    }

    private long parseCents(String value) {
        try {
            return new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2).longValueExact();
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("请输入正确的金额");
        }
    }

    private String shorten(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
