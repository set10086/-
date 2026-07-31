package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BillDrawerView extends LinearLayout {
    public interface Listener {
        void onClose();
        void onFilterStateChanged(boolean active);
        void onTransactionsChanged();
    }

    private static final long CURRENT_LEDGER = -2L;
    private static final int THIS_MONTH = 0;
    private static final int LAST_MONTH = 1;
    private static final int THIS_YEAR = 2;
    private static final int CUSTOM = 3;

    private final Activity activity;
    private final LedgerDb db;
    private final Listener listener;
    private final ZoneId zone = ZoneId.systemDefault();
    private final TextView timeButton;
    private final TextView ledgerButton;
    private final TextView detailButton;
    private final LinearLayout list;

    private long currentLedgerId;
    private long ledgerScope = CURRENT_LEDGER;
    private int timePreset = THIS_MONTH;
    private LocalDate customFrom = LocalDate.now().withDayOfMonth(1);
    private LocalDate customTo = LocalDate.now();
    private String type;
    private String category;
    private long accountId;
    private String bookkeeper;

    public BillDrawerView(Activity activity, LedgerDb db, long currentLedgerId, Listener listener) {
        super(activity);
        this.activity = activity;
        this.db = db;
        this.currentLedgerId = currentLedgerId;
        this.listener = listener;
        setOrientation(VERTICAL);
        setBackgroundColor(CartoonStyle.BACKGROUND);
        int side = V13Ui.dp(activity, 12);
        int top = V13Ui.dp(activity, 14);
        int bottom = V13Ui.dp(activity, 14);
        setPadding(side, top, side, bottom);
        setOnApplyWindowInsetsListener((view, insets) -> {
            setPadding(side, top + insets.getSystemWindowInsetTop(), side,
                    bottom + insets.getSystemWindowInsetBottom());
            return insets;
        });

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView close = V13Ui.button(activity, "×", CartoonStyle.SURFACE);
        close.setTextSize(22);
        close.setOnClickListener(v -> listener.onClose());
        header.addView(close, new LayoutParams(V13Ui.dp(activity, 48), V13Ui.dp(activity, 44)));
        TextView title = V13Ui.text(activity, "账单列表", 21, CartoonStyle.INK, true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LayoutParams(0, V13Ui.dp(activity, 44), 1f));
        detailButton = V13Ui.button(activity, "筛选", CartoonStyle.SOFT_SKY);
        detailButton.setOnClickListener(v -> showDetailFilters());
        header.addView(detailButton, new LayoutParams(V13Ui.dp(activity, 68), V13Ui.dp(activity, 44)));
        addView(header);
        addView(V13Ui.gap(activity, 8));

        LinearLayout scopes = new LinearLayout(activity);
        timeButton = V13Ui.button(activity, "本月", CartoonStyle.SOFT_YELLOW);
        ledgerButton = V13Ui.button(activity, "当前账本", CartoonStyle.SURFACE);
        timeButton.setOnClickListener(v -> showTimePicker());
        ledgerButton.setOnClickListener(v -> showLedgerPicker());
        scopes.addView(timeButton, new LayoutParams(0, V13Ui.dp(activity, 46), 1f));
        scopes.addView(new View(activity), new LayoutParams(V13Ui.dp(activity, 8), 1));
        scopes.addView(ledgerButton, new LayoutParams(0, V13Ui.dp(activity, 46), 1f));
        addView(scopes);
        addView(V13Ui.gap(activity, 8));

        ScrollView scroll = new ScrollView(activity);
        list = new LinearLayout(activity);
        list.setOrientation(VERTICAL);
        list.setPadding(0, 0, 0, V13Ui.dp(activity, 24));
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        refresh();
    }

    public void setCurrentLedger(long ledgerId) {
        currentLedgerId = ledgerId;
        if (ledgerScope == CURRENT_LEDGER) refresh();
    }

    public boolean hasActiveFilters() {
        return timePreset != THIS_MONTH || ledgerScope != CURRENT_LEDGER
                || type != null || category != null || accountId > 0L || bookkeeper != null;
    }

    public void refresh() {
        updateLabels();
        list.removeAllViews();
        TransactionFilter filter = buildFilter();
        List<LedgerDb.TxnView> rows;
        try {
            rows = db.getFilteredTransactions(filter, 0, 500);
        } catch (RuntimeException error) {
            showError(error.getMessage());
            return;
        }
        notifyFilterState();
        if (rows.isEmpty()) {
            TextView empty = V13Ui.text(activity,
                    "🧾\n当前筛选下没有账单\n" + filterSummary(),
                    15, CartoonStyle.MUTED, true);
            empty.setGravity(Gravity.CENTER);
            empty.setLineSpacing(0f, 1.25f);
            list.addView(empty, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 180)));
            TextView clear = V13Ui.button(activity, "清除筛选", CartoonStyle.CREAM_YELLOW);
            clear.setOnClickListener(v -> clearFilters());
            list.addView(clear, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 48)));
            return;
        }

        Map<String, long[]> totals = new LinkedHashMap<>();
        for (LedgerDb.TxnView txn : rows) {
            String day = V13Ui.day(txn.occurredAt);
            long[] values = totals.get(day);
            if (values == null) {
                values = new long[2];
                totals.put(day, values);
            }
            if (LedgerDb.TYPE_INCOME.equals(txn.type)) values[0] += txn.amountCents;
            else if (LedgerDb.TYPE_EXPENSE.equals(txn.type)) values[1] += txn.amountCents;
        }
        String shownDay = null;
        boolean crossLedger = resolvedLedgerId() == TransactionFilter.ALL_LEDGERS;
        for (LedgerDb.TxnView txn : rows) {
            String day = V13Ui.day(txn.occurredAt);
            if (!day.equals(shownDay)) {
                shownDay = day;
                String groupText;
                if (crossLedger) {
                    groupText = day + " · 跨账本（金额按各账本币种显示）";
                } else {
                    long[] values = totals.get(day);
                    groupText = day + "   收入 " + V13Ui.money(txn.currency, values[0])
                            + " · 支出 " + V13Ui.money(txn.currency, values[1]);
                }
                TextView group = V13Ui.text(activity, groupText,
                        13, CartoonStyle.MUTED, true);
                group.setPadding(V13Ui.dp(activity, 4), V13Ui.dp(activity, 10),
                        V13Ui.dp(activity, 4), V13Ui.dp(activity, 6));
                list.addView(group);
            }
            TextView row = V13Ui.transactionRow(activity, txn, crossLedger);
            row.setOnClickListener(v -> TransactionDetailDialog.show(activity, db, txn, () -> {
                listener.onTransactionsChanged();
                refresh();
            }));
            row.setOnLongClickListener(v -> {
                confirmDelete(txn);
                return true;
            });
            list.addView(row);
            list.addView(V13Ui.gap(activity, 7));
        }
    }

    private TransactionFilter buildFilter() {
        TransactionQueryRules.Range range;
        YearMonth now = YearMonth.now();
        if (timePreset == LAST_MONTH) range = TransactionQueryRules.previousMonthBounds(now, zone);
        else if (timePreset == THIS_YEAR) range = TransactionQueryRules.yearBounds(LocalDate.now().getYear(), zone);
        else if (timePreset == CUSTOM) {
            long from = customFrom.atStartOfDay(zone).toInstant().toEpochMilli();
            long to = customTo.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
            range = new TransactionQueryRules.Range(from, to);
        } else range = TransactionQueryRules.monthBounds(now, zone);
        return new TransactionFilter(resolvedLedgerId(), range.fromInclusive, range.toExclusive,
                type, category, accountId, bookkeeper);
    }

    private long resolvedLedgerId() {
        return ledgerScope == CURRENT_LEDGER ? currentLedgerId : ledgerScope;
    }

    private void showTimePicker() {
        String[] items = {"本月", "上月", "本年", "自定义日期范围"};
        new AlertDialog.Builder(activity).setTitle("选择时间")
                .setSingleChoiceItems(items, timePreset, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == CUSTOM) showCustomStart();
                    else {
                        timePreset = which;
                        refresh();
                    }
                }).setNegativeButton("取消", null).show();
    }

    private void showCustomStart() {
        LocalDate start = customFrom;
        new DatePickerDialog(activity, (picker, year, month, day) -> {
            LocalDate selected = LocalDate.of(year, month + 1, day);
            showCustomEnd(selected);
        }, start.getYear(), start.getMonthValue() - 1, start.getDayOfMonth()).show();
    }

    private void showCustomEnd(LocalDate start) {
        LocalDate end = customTo.isBefore(start) ? start : customTo;
        new DatePickerDialog(activity, (picker, year, month, day) -> {
            LocalDate selectedEnd = LocalDate.of(year, month + 1, day);
            if (selectedEnd.isBefore(start)) {
                Toast.makeText(activity, "结束日期不能早于开始日期", Toast.LENGTH_SHORT).show();
                return;
            }
            customFrom = start;
            customTo = selectedEnd;
            timePreset = CUSTOM;
            refresh();
        }, end.getYear(), end.getMonthValue() - 1, end.getDayOfMonth()).show();
    }

    private void showLedgerPicker() {
        List<LedgerDb.Ledger> ledgers = db.getLedgers();
        String[] items = new String[ledgers.size() + 2];
        items[0] = "当前账本";
        items[1] = "全部账本";
        for (int index = 0; index < ledgers.size(); index++) items[index + 2] = ledgers.get(index).name;
        new AlertDialog.Builder(activity).setTitle("选择账本范围")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) ledgerScope = CURRENT_LEDGER;
                    else if (which == 1) ledgerScope = TransactionFilter.ALL_LEDGERS;
                    else ledgerScope = ledgers.get(which - 2).id;
                    accountId = 0L;
                    refresh();
                }).setNegativeButton("取消", null).show();
    }

    private void showDetailFilters() {
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(VERTICAL);
        form.setPadding(V13Ui.dp(activity, 18), V13Ui.dp(activity, 8),
                V13Ui.dp(activity, 18), V13Ui.dp(activity, 8));
        Spinner typeSpinner = spinner(new String[]{"全部类型", "支出", "收入", "转账"});
        typeSpinner.setSelection(type == null ? 0 : LedgerDb.TYPE_EXPENSE.equals(type) ? 1
                : LedgerDb.TYPE_INCOME.equals(type) ? 2 : 3);
        Spinner categorySpinner = new Spinner(activity);
        Spinner accountSpinner = new Spinner(activity);
        Spinner keeperSpinner = new Spinner(activity);
        form.addView(field("账单类型", typeSpinner));
        form.addView(field("具体分类", categorySpinner));
        form.addView(field("账户", accountSpinner));
        form.addView(field("记账人", keeperSpinner));

        final List<String>[] categories = new List[]{new ArrayList<>()};
        Runnable updateCategories = () -> {
            categories[0] = categoryOptions(typeForPosition(typeSpinner.getSelectedItemPosition()));
            categorySpinner.setAdapter(new ArrayAdapter<>(activity,
                    android.R.layout.simple_spinner_dropdown_item, categories[0]));
            int selected = category == null ? 0 : categories[0].indexOf(category);
            categorySpinner.setSelection(Math.max(0, selected));
        };
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateCategories.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        updateCategories.run();

        List<LedgerDb.Account> accounts = db.getAccountsForScope(resolvedLedgerId());
        List<String> accountLabels = new ArrayList<>();
        accountLabels.add("全部账户");
        for (LedgerDb.Account account : accounts) {
            String ledgerPrefix = resolvedLedgerId() == TransactionFilter.ALL_LEDGERS
                    ? ledgerName(account.ledgerId) + " · " : "";
            accountLabels.add(ledgerPrefix + account.name);
        }
        accountSpinner.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, accountLabels));
        int accountPosition = 0;
        for (int index = 0; index < accounts.size(); index++) {
            if (accounts.get(index).id == accountId) accountPosition = index + 1;
        }
        accountSpinner.setSelection(accountPosition);

        List<String> keepers = db.getBookkeepers(resolvedLedgerId());
        List<String> keeperLabels = new ArrayList<>();
        keeperLabels.add("全部记账人");
        keeperLabels.addAll(keepers);
        keeperSpinner.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, keeperLabels));
        keeperSpinner.setSelection(bookkeeper == null ? 0 : Math.max(0, keeperLabels.indexOf(bookkeeper)));

        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("详细筛选")
                .setView(form).setNeutralButton("清除", null)
                .setNegativeButton("取消", null).setPositiveButton("应用", null).create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                type = null;
                category = null;
                accountId = 0L;
                bookkeeper = null;
                dialog.dismiss();
                refresh();
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                type = typeForPosition(typeSpinner.getSelectedItemPosition());
                category = categorySpinner.getSelectedItemPosition() <= 0 ? null
                        : categories[0].get(categorySpinner.getSelectedItemPosition());
                accountId = accountSpinner.getSelectedItemPosition() <= 0 ? 0L
                        : accounts.get(accountSpinner.getSelectedItemPosition() - 1).id;
                bookkeeper = keeperSpinner.getSelectedItemPosition() <= 0 ? null
                        : keeperLabels.get(keeperSpinner.getSelectedItemPosition());
                dialog.dismiss();
                refresh();
            });
        });
        dialog.show();
    }

    private List<String> categoryOptions(String selectedType) {
        List<String> result = new ArrayList<>();
        result.add("全部分类");
        if (selectedType == null) {
            for (InputCatalog.Option option : InputCatalog.categories(LedgerDb.TYPE_EXPENSE)) {
                if (!option.custom && !result.contains(option.label)) result.add(option.label);
            }
            for (InputCatalog.Option option : InputCatalog.categories(LedgerDb.TYPE_INCOME)) {
                if (!option.custom && !result.contains(option.label)) result.add(option.label);
            }
            result.add("账户转账");
        } else {
            for (InputCatalog.Option option : InputCatalog.categories(selectedType)) {
                if (!option.custom) result.add(option.label);
            }
        }
        return result;
    }

    private String typeForPosition(int position) {
        if (position == 1) return LedgerDb.TYPE_EXPENSE;
        if (position == 2) return LedgerDb.TYPE_INCOME;
        if (position == 3) return LedgerDb.TYPE_TRANSFER;
        return null;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(activity);
        spinner.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, values));
        return spinner;
    }

    private LinearLayout field(String label, View input) {
        LinearLayout group = new LinearLayout(activity);
        group.setOrientation(VERTICAL);
        TextView title = V13Ui.text(activity, label, 13, CartoonStyle.MUTED, true);
        title.setPadding(0, V13Ui.dp(activity, 7), 0, V13Ui.dp(activity, 2));
        group.addView(title);
        group.addView(input);
        return group;
    }

    private void clearFilters() {
        timePreset = THIS_MONTH;
        ledgerScope = CURRENT_LEDGER;
        type = null;
        category = null;
        accountId = 0L;
        bookkeeper = null;
        refresh();
    }

    private void updateLabels() {
        String[] times = {"本月", "上月", "本年", "自定义"};
        timeButton.setText(times[timePreset]);
        if (ledgerScope == CURRENT_LEDGER) ledgerButton.setText("当前账本");
        else if (ledgerScope == TransactionFilter.ALL_LEDGERS) ledgerButton.setText("全部账本");
        else {
            LedgerDb.Ledger ledger = db.getLedger(ledgerScope);
            ledgerButton.setText(ledger == null ? "当前账本" : ledger.name);
            if (ledger == null) {
                ledgerScope = CURRENT_LEDGER;
                accountId = 0L;
            }
        }
        detailButton.setText(type != null || category != null || accountId > 0L || bookkeeper != null
                ? "筛选 ●" : "筛选");
    }

    private String filterSummary() {
        return timeButton.getText() + " · " + ledgerButton.getText()
                + (type == null ? "" : " · " + V13Ui.typeName(type))
                + (category == null ? "" : " · " + category);
    }

    private void notifyFilterState() {
        if (listener != null) listener.onFilterStateChanged(hasActiveFilters());
    }

    private void confirmDelete(LedgerDb.TxnView txn) {
        new AlertDialog.Builder(activity).setTitle("删除这笔账？")
                .setMessage("删除后会恢复相关账户余额。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    try {
                        db.deleteTransaction(txn.id);
                        listener.onTransactionsChanged();
                        refresh();
                    } catch (RuntimeException error) {
                        Toast.makeText(activity, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private String ledgerName(long id) {
        LedgerDb.Ledger ledger = db.getLedger(id);
        return ledger == null ? "账本" : ledger.name;
    }

    private void showError(String message) {
        list.removeAllViews();
        TextView error = V13Ui.text(activity, "加载失败\n" + message,
                15, CartoonStyle.EXPENSE, true);
        error.setGravity(Gravity.CENTER);
        list.addView(error, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 150)));
    }
}
