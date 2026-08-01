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

public final class MainActivity extends Activity {
    private static final int SCREEN_HOME = 0;
    private static final int SCREEN_TRANSACTIONS = 1;
    private static final int SCREEN_ACCOUNTS = 2;
    private static final int SCREEN_STATS = 3;

    private static final int INDIGO = Color.rgb(57, 73, 171);
    private static final int GREEN = Color.rgb(34, 139, 94);
    private static final int RED = Color.rgb(198, 40, 40);
    private static final int BLUE = Color.rgb(30, 96, 180);
    private static final int TEXT = Color.rgb(32, 33, 36);
    private static final int MUTED = Color.rgb(95, 99, 104);
    private static final int SURFACE = Color.rgb(246, 247, 251);

    private LedgerDb db;
    private LinearLayout content;
    private Spinner ledgerSpinner;
    private final List<LedgerDb.Ledger> ledgers = new ArrayList<>();
    private long currentLedgerId = -1L;
    private int currentScreen = SCREEN_HOME;
    private boolean changingLedgerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(INDIGO);
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
        root.setBackgroundColor(SURFACE);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(12), dp(8), dp(8), dp(8));
        top.setBackgroundColor(INDIGO);

        ledgerSpinner = new Spinner(this);
        ledgerSpinner.setPopupBackgroundResource(android.R.color.white);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        top.addView(ledgerSpinner, spinnerParams);

        Button addLedger = new Button(this);
        addLedger.setText("＋账本");
        addLedger.setTextColor(Color.WHITE);
        addLedger.setBackgroundColor(Color.TRANSPARENT);
        addLedger.setOnClickListener(v -> showAddLedgerDialog());
        top.addView(addLedger, new LinearLayout.LayoutParams(dp(88), dp(48)));
        root.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(14), dp(14), dp(24));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(4), dp(3), dp(4), dp(3));
        nav.setBackgroundColor(Color.WHITE);
        addNavButton(nav, "首页", SCREEN_HOME);
        addNavButton(nav, "账单", SCREEN_TRANSACTIONS);
        addNavButton(nav, "账户", SCREEN_ACCOUNTS);
        addNavButton(nav, "统计", SCREEN_STATS);
        root.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        ledgerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (changingLedgerList || position < 0 || position >= ledgers.size()) {
                    return;
                }
                currentLedgerId = ledgers.get(position).id;
                showScreen(currentScreen);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // The database always has a default ledger.
            }
        });

        setContentView(root);
    }

    private void addNavButton(LinearLayout nav, String label, int screen) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setTextColor(TEXT);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setOnClickListener(v -> {
            currentScreen = screen;
            showScreen(screen);
        });
        nav.addView(button, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
    }

    private void reloadLedgers(long selectId) {
        changingLedgerList = true;
        ledgers.clear();
        ledgers.addAll(db.getLedgers());
        ArrayAdapter<LedgerDb.Ledger> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, ledgers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ledgerSpinner.setAdapter(adapter);

        int target = 0;
        for (int i = 0; i < ledgers.size(); i++) {
            if (ledgers.get(i).id == selectId || (selectId < 0 && ledgers.get(i).id == currentLedgerId)) {
                target = i;
                break;
            }
        }
        if (!ledgers.isEmpty()) {
            currentLedgerId = ledgers.get(target).id;
            ledgerSpinner.setSelection(target, false);
        }
        changingLedgerList = false;
        showScreen(currentScreen);
    }

    private void showScreen(int screen) {
        if (currentLedgerId < 0) {
            return;
        }
        content.removeAllViews();
        if (screen == SCREEN_HOME) {
            showHome();
        } else if (screen == SCREEN_TRANSACTIONS) {
            showTransactions();
        } else if (screen == SCREEN_ACCOUNTS) {
            showAccounts();
        } else {
            showStats();
        }
    }

    private void showHome() {
        addPageTitle("本月概览", "离线数据仅保存在当前手机");
        LedgerDb.Summary summary = db.getCurrentMonthSummary(currentLedgerId);
        long assets = db.getNetAssets(currentLedgerId);

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.addView(metricCard("收入", formatMoney(summary.incomeCents), GREEN),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        metrics.addView(space(dp(8)));
        metrics.addView(metricCard("支出", formatMoney(summary.expenseCents), RED),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(metrics);
        content.addView(space(dp(10)));
        content.addView(metricCard("净资产", formatMoney(assets), INDIGO));

        content.addView(space(dp(16)));
        addSectionTitle("快速记账");
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(actionButton("记支出", RED, LedgerDb.TYPE_EXPENSE),
                new LinearLayout.LayoutParams(0, dp(52), 1f));
        actions.addView(space(dp(8)));
        actions.addView(actionButton("记收入", GREEN, LedgerDb.TYPE_INCOME),
                new LinearLayout.LayoutParams(0, dp(52), 1f));
        actions.addView(space(dp(8)));
        actions.addView(actionButton("转账", BLUE, LedgerDb.TYPE_TRANSFER),
                new LinearLayout.LayoutParams(0, dp(52), 1f));
        content.addView(actions);

        content.addView(space(dp(18)));
        addSectionTitle("最近账单");
        List<LedgerDb.Txn> transactions = db.getRecentTransactions(currentLedgerId, 8);
        if (transactions.isEmpty()) {
            addEmpty("暂无账单，点击上方按钮开始记账。");
        } else {
            for (LedgerDb.Txn txn : transactions) {
                content.addView(transactionRow(txn));
                content.addView(space(dp(8)));
            }
        }
    }

    private void showTransactions() {
        addPageTitle("全部账单", "长按账单可以删除并恢复账户余额");
        Button add = primaryButton("＋ 新增账单");
        add.setOnClickListener(v -> showAddTransactionDialog(null));
        content.addView(add);
        content.addView(space(dp(14)));

        List<LedgerDb.Txn> transactions = db.getRecentTransactions(currentLedgerId, 300);
        if (transactions.isEmpty()) {
            addEmpty("当前账本还没有账单。");
            return;
        }
        for (LedgerDb.Txn txn : transactions) {
            content.addView(transactionRow(txn));
            content.addView(space(dp(8)));
        }
    }

    private void showAccounts() {
        addPageTitle("账户管理", "信用卡输入正数欠款时会自动保存为负债");
        Button add = primaryButton("＋ 新增账户");
        add.setOnClickListener(v -> showAddAccountDialog());
        content.addView(add);
        content.addView(space(dp(14)));

        List<LedgerDb.Account> accounts = db.getAccounts(currentLedgerId);
        if (accounts.isEmpty()) {
            addEmpty("当前账本还没有账户。");
            return;
        }
        String lastGroup = null;
        for (LedgerDb.Account account : accounts) {
            if (!account.groupName.equals(lastGroup)) {
                addSectionTitle(account.groupName.isEmpty() ? "未分组" : account.groupName);
                lastGroup = account.groupName;
            }
            LinearLayout card = card();
            TextView title = text(account.name, 17, TEXT, true);
            card.addView(title);
            card.addView(text(account.type + " · " + formatMoney(account.balanceCents), 15,
                    account.balanceCents < 0 ? RED : GREEN, true));
            if (!account.note.trim().isEmpty()) {
                card.addView(text(account.note, 13, MUTED, false));
            }
            content.addView(card);
            content.addView(space(dp(8)));
        }
    }

    private void showStats() {
        addPageTitle("统计回顾", "统计范围为当前账本和当前自然月");
        LedgerDb.Summary summary = db.getCurrentMonthSummary(currentLedgerId);
        long net = summary.incomeCents - summary.expenseCents;
        content.addView(metricCard("本月结余", formatMoney(net), net >= 0 ? GREEN : RED));
        content.addView(space(dp(12)));
        content.addView(metricCard("全部账户净资产", formatMoney(db.getNetAssets(currentLedgerId)), INDIGO));
        content.addView(space(dp(18)));
        addSectionTitle("支出分类");
        List<LedgerDb.CategoryTotal> totals = db.getCurrentMonthExpenseCategories(currentLedgerId);
        if (totals.isEmpty()) {
            addEmpty("本月还没有支出数据。");
            return;
        }
        long max = totals.get(0).totalCents;
        for (LedgerDb.CategoryTotal total : totals) {
            LinearLayout card = card();
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            TextView category = text(total.category, 16, TEXT, true);
            line.addView(category, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            line.addView(text(formatMoney(total.totalCents), 15, RED, true));
            card.addView(line);
            TextView bar = new TextView(this);
            int width = max == 0 ? 0 : (int) Math.max(8, Math.min(100, total.totalCents * 100 / max));
            bar.setText(repeat("■", Math.max(1, width / 5)));
            bar.setTextColor(INDIGO);
            bar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            card.addView(bar);
            content.addView(card);
            content.addView(space(dp(8)));
        }
    }

    private View actionButton(String text, int color, String type) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setAllCaps(false);
        button.setBackground(rounded(color, dp(12)));
        button.setOnClickListener(v -> showAddTransactionDialog(type));
        return button;
    }

    private View transactionRow(LedgerDb.Txn txn) {
        LinearLayout card = card();
        card.setClickable(true);
        card.setLongClickable(true);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        String typeName = typeName(txn.type);
        TextView title = text(typeName + " · " + txn.category, 16, TEXT, true);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        int color = LedgerDb.TYPE_EXPENSE.equals(txn.type) ? RED
                : LedgerDb.TYPE_INCOME.equals(txn.type) ? GREEN : BLUE;
        String prefix = LedgerDb.TYPE_EXPENSE.equals(txn.type) ? "−"
                : LedgerDb.TYPE_INCOME.equals(txn.type) ? "+" : "";
        top.addView(text(prefix + formatMoney(txn.amountCents), 16, color, true));
        card.addView(top);

        String accountInfo = txn.accountName;
        if (LedgerDb.TYPE_TRANSFER.equals(txn.type)) {
            accountInfo += " → " + txn.toAccountName;
        }
        card.addView(text(accountInfo + " · " + formatDate(txn.occurredAt), 13, MUTED, false));
        StringBuilder meta = new StringBuilder();
        if (!txn.bookkeeper.trim().isEmpty()) {
            meta.append("记账人：").append(txn.bookkeeper);
        }
        if (!txn.tags.trim().isEmpty()) {
            appendDot(meta).append("标签：").append(txn.tags);
        }
        if (txn.reimbursable) {
            appendDot(meta).append("待报销");
        }
        if (!txn.includeBudget) {
            appendDot(meta).append("不计预算");
        }
        if (txn.discountCents > 0) {
            appendDot(meta).append("优惠 ").append(formatMoney(txn.discountCents));
        }
        if (meta.length() > 0) {
            card.addView(text(meta.toString(), 12, MUTED, false));
        }
        if (!txn.note.trim().isEmpty()) {
            card.addView(text(txn.note, 13, TEXT, false));
        }

        card.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("删除账单")
                    .setMessage("删除后会同步恢复相关账户余额，是否继续？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除", (dialog, which) -> {
                        try {
                            db.deleteTransaction(txn.id);
                            toast("账单已删除");
                            showScreen(currentScreen);
                        } catch (RuntimeException error) {
                            toast(error.getMessage());
                        }
                    })
                    .show();
            return true;
        });
        return card;
    }

    private void showAddLedgerDialog() {
        LinearLayout form = form();
        EditText name = input("账本名称，例如：家庭账本", InputType.TYPE_CLASS_TEXT);
        Spinner currency = spinner(new String[]{"CNY", "JPY", "USD", "EUR", "GBP", "HKD"});
        form.addView(label("账本名称"));
        form.addView(name);
        form.addView(label("本位币"));
        form.addView(currency);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("新增账本")
                .setView(wrapScroll(form))
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                long id = db.addLedger(name.getText().toString(), currency.getSelectedItem().toString());
                dialog.dismiss();
                reloadLedgers(id);
                toast("账本已创建");
            } catch (RuntimeException error) {
                toast(error.getMessage());
            }
        }));
        dialog.show();
    }

    private void showAddAccountDialog() {
        LinearLayout form = form();
        EditText name = input("账户名称，例如：工资卡", InputType.TYPE_CLASS_TEXT);
        Spinner type = spinner(new String[]{"现金账户", "储蓄账户", "信用卡账户", "投资账户", "其他账户"});
        EditText group = input("分组，例如：银行卡", InputType.TYPE_CLASS_TEXT);
        EditText balance = input("当前余额或信用卡欠款", InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        EditText note = input("备注（可选）", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        form.addView(label("账户名称"));
        form.addView(name);
        form.addView(label("账户类型"));
        form.addView(type);
        form.addView(label("分组"));
        form.addView(group);
        form.addView(label("初始余额"));
        form.addView(balance);
        form.addView(label("备注"));
        form.addView(note);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("新增账户")
                .setView(wrapScroll(form))
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                long cents = balance.getText().toString().trim().isEmpty()
                        ? 0L : parseCents(balance.getText().toString());
                db.addAccount(currentLedgerId, name.getText().toString(), type.getSelectedItem().toString(),
                        group.getText().toString(), cents, note.getText().toString());
                dialog.dismiss();
                toast("账户已创建");
                showScreen(currentScreen);
            } catch (RuntimeException error) {
                toast(error.getMessage());
            }
        }));
        dialog.show();
    }

    private void showAddTransactionDialog(String presetType) {
        List<LedgerDb.Account> accounts = db.getAccounts(currentLedgerId);
        if (accounts.isEmpty()) {
            toast("请先创建账户");
            return;
        }
        LinearLayout form = form();
        Spinner type = spinner(new String[]{"支出", "收入", "转账"});
        if (LedgerDb.TYPE_INCOME.equals(presetType)) {
            type.setSelection(1);
        } else if (LedgerDb.TYPE_TRANSFER.equals(presetType)) {
            type.setSelection(2);
        }
        EditText amount = input("实际金额", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText discount = input("优惠金额（可选）", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText category = input("分类，例如：餐饮", InputType.TYPE_CLASS_TEXT);
        category.setText(LedgerDb.TYPE_INCOME.equals(presetType) ? "工资"
                : LedgerDb.TYPE_TRANSFER.equals(presetType) ? "账户转账" : "餐饮");
        Spinner fromAccount = accountSpinner(accounts);
        Spinner toAccount = accountSpinner(accounts);
        EditText occurredAt = input("yyyy-MM-dd HH:mm", InputType.TYPE_CLASS_DATETIME);
        occurredAt.setText(formatDate(System.currentTimeMillis()));
        EditText bookkeeper = input("记账人", InputType.TYPE_CLASS_TEXT);
        bookkeeper.setText("本人");
        EditText tags = input("标签，多个可用逗号分隔", InputType.TYPE_CLASS_TEXT);
        CheckBox reimbursable = new CheckBox(this);
        reimbursable.setText("需要报销");
        CheckBox includeBudget = new CheckBox(this);
        includeBudget.setText("计入预算");
        includeBudget.setChecked(true);
        EditText note = input("备注（可选）", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        form.addView(label("账单类型"));
        form.addView(type);
        form.addView(label("金额"));
        form.addView(amount);
        form.addView(label("优惠"));
        form.addView(discount);
        form.addView(label("分类"));
        form.addView(category);
        form.addView(label("账户 / 转出账户"));
        form.addView(fromAccount);
        TextView toLabel = label("转入账户");
        form.addView(toLabel);
        form.addView(toAccount);
        form.addView(label("发生时间"));
        form.addView(occurredAt);
        form.addView(label("记账人"));
        form.addView(bookkeeper);
        form.addView(label("标签"));
        form.addView(tags);
        form.addView(reimbursable);
        form.addView(includeBudget);
        form.addView(label("备注"));
        form.addView(note);

        View.OnClickListener visibilityUpdater = ignored -> {
            boolean transfer = type.getSelectedItemPosition() == 2;
            toLabel.setVisibility(transfer ? View.VISIBLE : View.GONE);
            toAccount.setVisibility(transfer ? View.VISIBLE : View.GONE);
            if (type.getSelectedItemPosition() == 0 && category.getText().toString().trim().isEmpty()) {
                category.setText("餐饮");
            } else if (type.getSelectedItemPosition() == 1) {
                category.setText("工资");
            } else if (transfer) {
                category.setText("账户转账");
            }
        };
        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                visibilityUpdater.onClick(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        visibilityUpdater.onClick(type);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("新增账单")
                .setView(wrapScroll(form))
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                long cents = parseCents(amount.getText().toString());
                long discountCents = discount.getText().toString().trim().isEmpty()
                        ? 0L : parseCents(discount.getText().toString());
                String dbType = type.getSelectedItemPosition() == 0 ? LedgerDb.TYPE_EXPENSE
                        : type.getSelectedItemPosition() == 1 ? LedgerDb.TYPE_INCOME
                        : LedgerDb.TYPE_TRANSFER;
                LedgerDb.Account source = (LedgerDb.Account) fromAccount.getSelectedItem();
                Long targetId = null;
                if (LedgerDb.TYPE_TRANSFER.equals(dbType)) {
                    LedgerDb.Account target = (LedgerDb.Account) toAccount.getSelectedItem();
                    targetId = target.id;
                }
                db.addTransaction(currentLedgerId, dbType, category.getText().toString(), cents,
                        source.id, targetId, bookkeeper.getText().toString(), tags.getText().toString(),
                        reimbursable.isChecked(), discountCents, includeBudget.isChecked(),
                        note.getText().toString(), parseDate(occurredAt.getText().toString()));
                dialog.dismiss();
                toast("账单已保存");
                showScreen(currentScreen);
            } catch (RuntimeException error) {
                toast(error.getMessage());
            }
        }));
        dialog.show();
    }

    private Spinner accountSpinner(List<LedgerDb.Account> accounts) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<LedgerDb.Account> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, accounts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private LinearLayout metricCard(String label, String value, int color) {
        LinearLayout card = card();
        card.addView(text(label, 13, MUTED, false));
        card.addView(text(value, 21, color, true));
        return card;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        button.setAllCaps(false);
        button.setBackground(rounded(INDIGO, dp(12)));
        button.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(rounded(Color.WHITE, dp(14)));
        card.setElevation(dp(1));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private void addPageTitle(String title, String subtitle) {
        content.addView(text(title, 24, TEXT, true));
        content.addView(text(subtitle, 13, MUTED, false));
        content.addView(space(dp(14)));
    }

    private void addSectionTitle(String title) {
        TextView view = text(title, 16, TEXT, true);
        view.setPadding(0, dp(4), 0, dp(8));
        content.addView(view);
    }

    private void addEmpty(String message) {
        LinearLayout card = card();
        TextView view = text(message, 14, MUTED, false);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), dp(20), dp(8), dp(20));
        card.addView(view);
        content.addView(card);
    }

    private LinearLayout form() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(12));
        return form;
    }

    private ScrollView wrapScroll(View view) {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(view);
        return scroll;
    }

    private TextView label(String value) {
        TextView label = text(value, 13, MUTED, true);
        label.setPadding(0, dp(10), 0, dp(4));
        return label;
    }

    private EditText input(String hint, int inputType) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setInputType(inputType);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        editText.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        editText.setPadding(dp(10), dp(8), dp(10), dp(8));
        editText.setBackground(rounded(Color.rgb(242, 243, 248), dp(9)));
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return editText;
    }

    private Spinner spinner(String[] items) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private View space(int height) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(8), height));
        return view;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private String formatMoney(long cents) {
        BigDecimal value = BigDecimal.valueOf(cents, 2);
        return currentCurrency() + " " + value.toPlainString();
    }

    private String currentCurrency() {
        for (LedgerDb.Ledger ledger : ledgers) {
            if (ledger.id == currentLedgerId) {
                return ledger.currency;
            }
        }
        return "CNY";
    }

    private long parseCents(String raw) {
        String clean = raw == null ? "" : raw.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("请输入金额");
        }
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
            if (date == null) {
                throw new ParseException("empty", 0);
            }
            return date.getTime();
        } catch (ParseException error) {
            throw new IllegalArgumentException("时间格式应为 yyyy-MM-dd HH:mm");
        }
    }

    private String formatDate(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(timestamp));
    }

    private String typeName(String type) {
        if (LedgerDb.TYPE_EXPENSE.equals(type)) {
            return "支出";
        }
        if (LedgerDb.TYPE_INCOME.equals(type)) {
            return "收入";
        }
        return "转账";
    }

    private StringBuilder appendDot(StringBuilder builder) {
        if (builder.length() > 0) {
            builder.append(" · ");
        }
        return builder;
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message == null ? "操作失败" : message, Toast.LENGTH_SHORT).show();
    }
}
