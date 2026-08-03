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
import android.view.WindowInsets;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class LedgerV12Activity extends Activity {
    private static final int HOME = 0;
    private static final int BILLS = 1;
    private static final int ACCOUNTS = 2;
    private static final int STATS = 3;

    private final List<LedgerDb.Ledger> ledgers = new ArrayList<>();
    private final TextView[] navItems = new TextView[4];
    private LedgerDb db;
    private LinearLayout content;
    private TextView ledgerButton;
    private long currentLedgerId = -1L;
    private int currentScreen = HOME;

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
        if (db != null) db.close();
        super.onDestroy();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CartoonStyle.BACKGROUND);

        LinearLayout headerWrap = new LinearLayout(this);
        int headerLeft = dp(12);
        int headerTop = dp(10);
        int headerRight = dp(12);
        int headerBottom = dp(4);
        headerWrap.setPadding(headerLeft, headerTop, headerRight, headerBottom);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(12), dp(12));
        header.setBackground(panel(CartoonStyle.CREAM_YELLOW, CartoonStyle.OUTLINE, 2, 24));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.addView(text("✏️  蜡笔小账本", 20, CartoonStyle.INK, true));
        brand.addView(text("把每一笔小日子都收好", 12, CartoonStyle.MUTED, false));
        header.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ledgerButton = pill("选择账本", CartoonStyle.SURFACE);
        ledgerButton.setOnClickListener(v -> showLedgerPicker());
        header.addView(ledgerButton, new LinearLayout.LayoutParams(dp(126), dp(44)));
        header.addView(gapH(dp(6)));
        TextView addLedger = pill("＋", CartoonStyle.PEACH);
        addLedger.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        addLedger.setOnClickListener(v -> showAddLedgerDialog());
        header.addView(addLedger, new LinearLayout.LayoutParams(dp(48), dp(44)));
        headerWrap.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(headerWrap);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout navWrap = new LinearLayout(this);
        int navLeft = dp(12);
        int navTop = dp(4);
        int navRight = dp(12);
        int navBottom = dp(10);
        navWrap.setPadding(navLeft, navTop, navRight, navBottom);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(6), dp(5), dp(6), dp(5));
        nav.setBackground(panel(CartoonStyle.SURFACE, 0xFFE8D9C3, 1, 24));
        addNav(nav, 0, "🏡\n首页");
        addNav(nav, 1, "🧾\n账单");
        addNav(nav, 2, "👛\n账户");
        addNav(nav, 3, "📊\n统计");
        navWrap.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70)));
        root.addView(navWrap);

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int status = insets.getSystemWindowInsetTop();
            int navigation = insets.getSystemWindowInsetBottom();
            headerWrap.setPadding(headerLeft, headerTop + status, headerRight, headerBottom);
            navWrap.setPadding(navLeft, navTop, navRight, navBottom + navigation);
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();
    }

    private void addNav(LinearLayout parent, int index, String label) {
        TextView item = text(label, 13, CartoonStyle.MUTED, true);
        item.setGravity(Gravity.CENTER);
        item.setOnClickListener(v -> {
            currentScreen = index;
            showScreen(index);
        });
        navItems[index] = item;
        parent.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
    }

    private void reloadLedgers(long selectedId) {
        ledgers.clear();
        ledgers.addAll(db.getLedgers());
        int selected = 0;
        for (int index = 0; index < ledgers.size(); index++) {
            long id = ledgers.get(index).id;
            if (id == selectedId || (selectedId < 0 && id == currentLedgerId)) selected = index;
        }
        if (!ledgers.isEmpty()) {
            currentLedgerId = ledgers.get(selected).id;
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
        String[] labels = new String[ledgers.size()];
        for (int index = 0; index < ledgers.size(); index++) {
            LedgerDb.Ledger ledger = ledgers.get(index);
            labels[index] = "📒  " + ledger.name + " · " + ledger.currency;
        }
        new AlertDialog.Builder(this).setTitle("切换小账本")
                .setItems(labels, (dialog, which) -> {
                    currentLedgerId = ledgers.get(which).id;
                    updateLedgerButton();
                    showScreen(currentScreen);
                }).setNegativeButton("取消", null).show();
    }

    private void showScreen(int screen) {
        if (currentLedgerId < 0) return;
        content.removeAllViews();
        int[] tints = {CartoonStyle.SOFT_YELLOW, CartoonStyle.SOFT_PEACH,
                CartoonStyle.SOFT_SKY, CartoonStyle.SOFT_GREEN};
        for (int index = 0; index < navItems.length; index++) {
            navItems[index].setTextColor(index == screen ? CartoonStyle.INK : CartoonStyle.MUTED);
            navItems[index].setBackground(index == screen ? panel(tints[index], 0, 0, 18) : ColorDrawable.transparent());
        }
        if (screen == HOME) showHome();
        else if (screen == BILLS) showBills();
        else if (screen == ACCOUNTS) showAccounts();
        else showStats();
    }

    private void showHome() {
        greeting("今天也要好好记账呀", "金额、分类、账户和时间都可以点选");
        LedgerDb.Summary summary = db.getCurrentMonthSummary(currentLedgerId);
        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.addView(metric("🌱 收入", summary.incomeCents, CartoonStyle.SOFT_GREEN, CartoonStyle.INCOME), weight());
        metrics.addView(gapH(dp(8)));
        metrics.addView(metric("🍑 支出", summary.expenseCents, CartoonStyle.SOFT_PEACH, CartoonStyle.EXPENSE), weight());
        content.addView(metrics);
        content.addView(gapV(dp(9)));
        content.addView(metric("👛 净资产", db.getNetAssets(currentLedgerId), CartoonStyle.SOFT_YELLOW, CartoonStyle.INK));
        content.addView(gapV(dp(16)));
        section("快速记一笔");
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(action("🍜\n支出", CartoonStyle.PEACH, LedgerDb.TYPE_EXPENSE), weight());
        actions.addView(gapH(dp(8)));
        actions.addView(action("💰\n收入", CartoonStyle.AVOCADO, LedgerDb.TYPE_INCOME), weight());
        actions.addView(gapH(dp(8)));
        actions.addView(action("↔\n转账", CartoonStyle.SKY, LedgerDb.TYPE_TRANSFER), weight());
        content.addView(actions);
        content.addView(gapV(dp(18)));
        section("最近账单");
        List<LedgerDb.Txn> transactions = db.getRecentTransactions(currentLedgerId, 8);
        if (transactions.isEmpty()) empty("🖍️", "还没有账单", "点上方按钮记下第一笔吧");
        else for (LedgerDb.Txn txn : transactions) addBillCard(txn);
    }

    private void showBills() {
        greeting("账单时间线", "长按账单可以删除并恢复账户余额");
        TextView add = pill("＋ 记一笔", CartoonStyle.CREAM_YELLOW);
        add.setOnClickListener(v -> showAddTransaction(null));
        content.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        content.addView(gapV(dp(12)));
        List<LedgerDb.Txn> transactions = db.getRecentTransactions(currentLedgerId, 300);
        if (transactions.isEmpty()) empty("🧾", "账单页还是空的", "记下一笔后会出现在这里");
        else for (LedgerDb.Txn txn : transactions) addBillCard(txn);
    }

    private void showAccounts() {
        greeting("我的账户卡包", "记账时可以直接选择这些账户");
        TextView add = pill("＋ 新增账户", CartoonStyle.SKY);
        add.setOnClickListener(v -> showAddAccountDialog());
        content.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        content.addView(gapV(dp(12)));
        List<LedgerDb.Account> accounts = db.getAccounts(currentLedgerId);
        if (accounts.isEmpty()) {
            empty("👛", "还没有账户", "先创建一个现金或银行卡账户");
            return;
        }
        String group = null;
        for (LedgerDb.Account account : accounts) {
            if (!account.groupName.equals(group)) {
                group = account.groupName;
                section(group.isEmpty() ? "未分组" : group);
            }
            LinearLayout card = card(CartoonStyle.accountTint(account.type));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(text(CartoonStyle.accountIcon(account.type), 28, CartoonStyle.INK, false),
                    new LinearLayout.LayoutParams(dp(50), dp(50)));
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.addView(text(account.name, 17, CartoonStyle.INK, true));
            info.addView(text(account.type + (account.note.isEmpty() ? "" : " · " + account.note),
                    12, CartoonStyle.MUTED, false));
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(text(formatMoney(account.balanceCents), 16,
                    account.balanceCents < 0 ? CartoonStyle.EXPENSE : CartoonStyle.INK, true));
            card.addView(row);
            content.addView(card);
            content.addView(gapV(dp(8)));
        }
    }

    private void showStats() {
        greeting("本月统计", "当前账本的收入、支出和分类回顾");
        LedgerDb.Summary summary = db.getCurrentMonthSummary(currentLedgerId);
        content.addView(metric("✨ 本月结余", summary.incomeCents - summary.expenseCents,
                CartoonStyle.SOFT_LAVENDER, CartoonStyle.INK));
        content.addView(gapV(dp(10)));
        content.addView(metric("👛 账户净资产", db.getNetAssets(currentLedgerId),
                CartoonStyle.SOFT_YELLOW, CartoonStyle.INK));
        content.addView(gapV(dp(16)));
        section("支出分类");
        List<LedgerDb.CategoryTotal> totals = db.getCurrentMonthExpenseCategories(currentLedgerId);
        if (totals.isEmpty()) empty("📊", "暂无统计数据", "记下支出后即可查看分类统计");
        else for (LedgerDb.CategoryTotal total : totals) {
            LinearLayout card = card(CartoonStyle.SURFACE);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(text(CartoonStyle.transactionIcon(LedgerDb.TYPE_EXPENSE, total.category)
                    + "  " + total.category, 15, CartoonStyle.INK, true), weight());
            row.addView(text(formatMoney(total.totalCents), 15, CartoonStyle.EXPENSE, true));
            card.addView(row);
            content.addView(card);
            content.addView(gapV(dp(8)));
        }
    }

    private void addBillCard(LedgerDb.Txn txn) {
        LinearLayout card = card(CartoonStyle.SURFACE);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(CartoonStyle.transactionIcon(txn.type, txn.category), 26, CartoonStyle.INK, false),
                new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(text(txn.category, 16, CartoonStyle.INK, true));
        String accounts = txn.accountName;
        if (LedgerDb.TYPE_TRANSFER.equals(txn.type)) accounts += " → " + txn.toAccountName;
        info.addView(text(accounts + " · " + formatTime(txn.occurredAt), 12, CartoonStyle.MUTED, false));
        if (!txn.bookkeeper.isEmpty()) info.addView(text("记账人：" + txn.bookkeeper, 11, CartoonStyle.MUTED, false));
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        String prefix = LedgerDb.TYPE_EXPENSE.equals(txn.type) ? "−"
                : LedgerDb.TYPE_INCOME.equals(txn.type) ? "+" : "";
        row.addView(text(prefix + formatMoney(txn.amountCents), 16,
                CartoonStyle.transactionColor(txn.type), true));
        card.addView(row);
        card.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this).setTitle("删除这笔账？")
                    .setMessage("删除后会恢复相关账户余额。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除", (dialog, which) -> {
                        db.deleteTransaction(txn.id);
                        toast("账单已删除");
                        showScreen(currentScreen);
                    }).show();
            return true;
        });
        content.addView(card);
        content.addView(gapV(dp(8)));
    }

    private void showAddTransaction(String presetType) {
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
        long[] occurredAt = {System.currentTimeMillis()};
        String[] bookkeeper = {"本人"};

        LinearLayout form = form();
        Spinner type = spinner(new String[]{"支出", "收入", "转账"});
        if (LedgerDb.TYPE_INCOME.equals(presetType)) type.setSelection(1);
        if (LedgerDb.TYPE_TRANSFER.equals(presetType)) type.setSelection(2);
        addField(form, "账单类型", type);

        TextView amount = selectField("💴  点击输入金额", "支持预加减乘除");
        addField(form, "金额", amount);
        amount.setOnClickListener(v -> AmountCalculatorDialog.show(this, "计算金额", amountCents[0], cents -> {
            amountCents[0] = cents;
            amount.setText("💴  " + formatMoney(cents));
        }));

        TextView discount = selectField("🏷️  无优惠", "点击计算优惠金额");
        addField(form, "优惠", discount);
        discount.setOnClickListener(v -> AmountCalculatorDialog.show(this, "计算优惠", discountCents[0], cents -> {
            discountCents[0] = cents;
            discount.setText("🏷️  " + formatMoney(cents));
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
                    }
                }));

        TextView targetLabel = formLabel("转入账户");
        TextView targetView = selectField(target[0] == null ? "请先创建另一个账户" : accountLabel(target[0]),
                "转入账户必须不同");
        form.addView(targetLabel);
        form.addView(targetView);
        targetView.setOnClickListener(v -> AccountPickerDialog.show(this, "选择转入账户", accounts,
                currentCurrency(), source[0].id, account -> {
                    target[0] = account;
                    targetView.setText(accountLabel(account));
                }));

        TextView timeView = selectField("🕒  " + formatDate(occurredAt[0]), "点击滚轮选择时间");
        addField(form, "发生时间", timeView);
        timeView.setOnClickListener(v -> WheelDateTimeDialog.show(this, occurredAt[0], timestamp -> {
            occurredAt[0] = timestamp;
            timeView.setText("🕒  " + formatDate(timestamp));
        }));

        TextView bookkeeperView = selectField("🙂  " + bookkeeper[0], "点击选择或新增记账人");
        addField(form, "记账人", bookkeeperView);
        bookkeeperView.setOnClickListener(v -> BookkeeperPickerDialog.show(this, value -> {
            bookkeeper[0] = value;
            bookkeeperView.setText("🙂  " + value);
        }));

        EditText tags = input("多个标签用逗号分隔", InputType.TYPE_CLASS_TEXT);
        EditText note = input("写点备注（可选）", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
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
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { updateType.run(); }
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
                .setView(wrap(form)).setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                if (amountCents[0] <= 0) throw new IllegalArgumentException("请先输入金额");
                int selected = type.getSelectedItemPosition();
                String dbType = selected == 0 ? LedgerDb.TYPE_EXPENSE
                        : selected == 1 ? LedgerDb.TYPE_INCOME : LedgerDb.TYPE_TRANSFER;
                Long targetId = null;
                if (LedgerDb.TYPE_TRANSFER.equals(dbType)) {
                    if (target[0] == null) throw new IllegalArgumentException("请选择转入账户");
                    if (source[0].id == target[0].id) throw new IllegalArgumentException("转入和转出账户不能相同");
                    targetId = target[0].id;
                }
                db.addTransaction(currentLedgerId, dbType, category[0], amountCents[0], source[0].id,
                        targetId, bookkeeper[0], tags.getText().toString(), reimbursable.isChecked(),
                        discountCents[0], budget.isChecked(), note.getText().toString(), occurredAt[0]);
                dialog.dismiss();
                toast("记好啦");
                showScreen(currentScreen);
            } catch (RuntimeException error) {
                toast(error.getMessage());
            }
        }));
        dialog.show();
    }

    private void showAddLedgerDialog() {
        LinearLayout form = form();
        EditText name = input("例如：家庭账本", InputType.TYPE_CLASS_TEXT);
        Spinner currency = spinner(new String[]{"CNY", "JPY", "USD", "EUR", "GBP", "HKD"});
        addField(form, "账本名称", name);
        addField(form, "本位币", currency);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("新建账本")
                .setView(wrap(form)).setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                long id = db.addLedger(name.getText().toString(), currency.getSelectedItem().toString());
                dialog.dismiss();
                reloadLedgers(id);
            } catch (RuntimeException error) { toast(error.getMessage()); }
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
                .setView(wrap(form)).setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                long cents = balance.getText().toString().trim().isEmpty() ? 0L : parseCents(balance.getText().toString());
                db.addAccount(currentLedgerId, name.getText().toString(), type.getSelectedItem().toString(),
                        group.getText().toString(), cents, note.getText().toString());
                dialog.dismiss();
                showScreen(currentScreen);
            } catch (RuntimeException error) { toast(error.getMessage()); }
        }));
        dialog.show();
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
        LinearLayout card = card(fill);
        card.addView(text(label, 13, CartoonStyle.MUTED, true));
        card.addView(text(formatMoney(cents), 20, valueColor, true));
        return card;
    }

    private TextView action(String label, int fill, String type) {
        TextView view = pill(label, fill);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        view.setOnClickListener(v -> showAddTransaction(type));
        return view;
    }

    private TextView selectField(String value, String hint) {
        TextView view = text(value + "\n" + hint, 15, CartoonStyle.INK, true);
        view.setPadding(dp(14), dp(11), dp(14), dp(11));
        view.setBackground(panel(CartoonStyle.SURFACE, 0xFFE4D5C2, 1, 16));
        view.setMinHeight(dp(58));
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private void greeting(String title, String subtitle) {
        content.addView(text(title, 22, CartoonStyle.INK, true));
        content.addView(text(subtitle, 13, CartoonStyle.MUTED, false));
        content.addView(gapV(dp(15)));
    }

    private void section(String title) {
        TextView view = text(title, 17, CartoonStyle.INK, true);
        view.setPadding(dp(2), dp(4), 0, dp(9));
        content.addView(view);
    }

    private void empty(String icon, String title, String description) {
        LinearLayout card = card(CartoonStyle.SURFACE);
        card.setGravity(Gravity.CENTER);
        card.addView(text(icon, 36, CartoonStyle.INK, false));
        TextView heading = text(title, 17, CartoonStyle.INK, true);
        heading.setGravity(Gravity.CENTER);
        card.addView(heading);
        TextView body = text(description, 13, CartoonStyle.MUTED, false);
        body.setGravity(Gravity.CENTER);
        card.addView(body);
        content.addView(card);
    }

    private LinearLayout card(int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(panel(color, 0xFFE5D5BE, 1, 22));
        return card;
    }

    private TextView pill(String value, int color) {
        TextView view = text(value, 14, CartoonStyle.INK, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), dp(6), dp(8), dp(6));
        view.setBackground(panel(color, CartoonStyle.OUTLINE, 1, 18));
        return view;
    }

    private LinearLayout form() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(6), dp(18), dp(16));
        return form;
    }

    private void addField(LinearLayout form, String label, View field) {
        form.addView(formLabel(label));
        form.addView(field, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private TextView formLabel(String label) {
        TextView view = text(label, 13, CartoonStyle.MUTED, true);
        view.setPadding(0, dp(11), 0, dp(5));
        return view;
    }

    private EditText input(String hint, int type) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(type);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setPadding(dp(12), dp(9), dp(12), dp(9));
        input.setBackground(panel(CartoonStyle.SURFACE, 0xFFE4D5C2, 1, 14));
        return input;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
        spinner.setBackground(panel(CartoonStyle.SURFACE, 0xFFE4D5C2, 1, 14));
        spinner.setPadding(dp(8), 0, dp(8), 0);
        return spinner;
    }

    private CheckBox check(String label, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setChecked(checked);
        box.setTextColor(CartoonStyle.INK);
        return box;
    }

    private ScrollView wrap(View view) {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(view);
        return scroll;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable panel(int color, int stroke, int strokeDp, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private View gapH(int width) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(width, 1));
        return view;
    }

    private View gapV(int height) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, height));
        return view;
    }

    private String currentCurrency() {
        for (LedgerDb.Ledger ledger : ledgers) if (ledger.id == currentLedgerId) return ledger.currency;
        return "CNY";
    }

    private String formatMoney(long cents) {
        return currentCurrency() + " " + BigDecimal.valueOf(cents, 2).toPlainString();
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(timestamp));
    }

    private String formatDate(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(timestamp));
    }

    private long parseCents(String raw) {
        try {
            return new BigDecimal(raw.trim()).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("金额格式不正确");
        }
    }

    private String shorten(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message == null ? "操作失败" : message, Toast.LENGTH_SHORT).show();
    }

    private static final class ColorDrawable {
        private static android.graphics.drawable.ColorDrawable transparent() {
            return new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT);
        }
    }
}
