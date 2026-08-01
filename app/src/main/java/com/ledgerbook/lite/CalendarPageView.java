package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CalendarPageView extends LinearLayout {
    public interface Listener {
        void onQuickAdd(long occurredAt);
        void onTransactionsChanged();
    }

    private final Activity activity;
    private final LedgerDb db;
    private final Listener listener;
    private final ZoneId zone = ZoneId.systemDefault();
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy 年 M 月", Locale.getDefault());
    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日", Locale.getDefault());
    private final TextView monthTitle;
    private final GridLayout grid;
    private final LinearLayout dayList;

    private long ledgerId = -1L;
    private YearMonth month = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();
    private float touchDownX;

    public CalendarPageView(Activity activity, LedgerDb db, Listener listener) {
        super(activity);
        this.activity = activity;
        this.db = db;
        this.listener = listener;
        setOrientation(VERTICAL);
        setBackgroundColor(CartoonStyle.BACKGROUND);
        setPadding(V13Ui.dp(activity, 12), V13Ui.dp(activity, 10),
                V13Ui.dp(activity, 12), 0);

        LinearLayout controls = new LinearLayout(activity);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        TextView previous = V13Ui.button(activity, "‹", CartoonStyle.SURFACE);
        previous.setTextSize(25);
        monthTitle = V13Ui.button(activity, month.format(monthFormatter), CartoonStyle.SOFT_YELLOW);
        monthTitle.setContentDescription("选择年份和月份");
        TextView today = V13Ui.button(activity, "今天", CartoonStyle.SOFT_GREEN);
        TextView next = V13Ui.button(activity, "›", CartoonStyle.SURFACE);
        next.setTextSize(25);
        controls.addView(previous, new LayoutParams(V13Ui.dp(activity, 48), V13Ui.dp(activity, 44)));
        controls.addView(monthTitle, new LayoutParams(0, V13Ui.dp(activity, 44), 1f));
        controls.addView(today, new LayoutParams(V13Ui.dp(activity, 62), V13Ui.dp(activity, 44)));
        controls.addView(next, new LayoutParams(V13Ui.dp(activity, 48), V13Ui.dp(activity, 44)));
        addView(controls);
        addView(V13Ui.gap(activity, 8));

        String[] week = {"一", "二", "三", "四", "五", "六", "日"};
        GridLayout weekHeader = new GridLayout(activity);
        weekHeader.setColumnCount(7);
        for (String label : week) {
            TextView view = V13Ui.text(activity, label, 12, CartoonStyle.MUTED, true);
            view.setGravity(Gravity.CENTER);
            weekHeader.addView(view, cellParams(weekHeader, V13Ui.dp(activity, 26)));
        }
        addView(weekHeader, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 26)));

        grid = new GridLayout(activity);
        grid.setColumnCount(7);
        grid.setRowCount(6);
        addView(grid, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 312)));
        addView(V13Ui.gap(activity, 8));

        ScrollView dayScroll = new ScrollView(activity);
        dayList = new LinearLayout(activity);
        dayList.setOrientation(VERTICAL);
        dayList.setPadding(0, 0, 0, V13Ui.dp(activity, 90));
        dayScroll.addView(dayList, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(dayScroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        previous.setOnClickListener(v -> setMonth(month.minusMonths(1)));
        monthTitle.setOnClickListener(v -> YearMonthPickerDialog.show(
                activity, month, this::setMonth));
        next.setOnClickListener(v -> setMonth(month.plusMonths(1)));
        today.setOnClickListener(v -> {
            selectedDate = LocalDate.now();
            setMonth(YearMonth.from(selectedDate));
        });
        setOnTouchListener((v, event) -> handleSwipe(event));
    }

    public void setLedger(long ledgerId) {
        this.ledgerId = ledgerId;
        refresh();
    }

    public void setMonth(YearMonth month) {
        if (month == null) return;
        this.month = month;
        if (!YearMonth.from(selectedDate).equals(month)) selectedDate = month.atDay(1);
        refresh();
    }

    public YearMonth getMonth() {
        return month;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void refresh() {
        if (ledgerId <= 0L) return;
        monthTitle.setText(month.format(monthFormatter));
        renderGrid();
        renderDay();
    }

    private void renderGrid() {
        grid.removeAllViews();
        TransactionQueryRules.Range range = TransactionQueryRules.monthBounds(month, zone);
        Map<Long, LedgerDb.DaySummary> summaryByDay = new HashMap<>();
        for (LedgerDb.DaySummary summary : db.getMonthDaySummaries(
                ledgerId, range.fromInclusive, range.toExclusive)) {
            summaryByDay.put(summary.epochDay, summary);
        }
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = TransactionQueryRules.monthGrid(month);
        for (LocalDate date : dates) {
            LedgerDb.DaySummary summary = summaryByDay.get(date.toEpochDay());
            StringBuilder label = new StringBuilder(String.valueOf(date.getDayOfMonth()));
            if (summary != null && summary.expenseCents > 0L) {
                label.append("\n−").append(TransactionQueryRules.compactAmount(summary.expenseCents));
            } else {
                label.append("\n ");
            }
            if (summary != null && summary.incomeCents > 0L) label.append(" 🟢");
            if (summary != null && summary.hasTransfer) label.append(" 🔵");
            TextView cell = V13Ui.text(activity, label.toString(), 10,
                    YearMonth.from(date).equals(month) ? CartoonStyle.INK : 0xFFB3AAA0, true);
            cell.setGravity(Gravity.CENTER);
            cell.setLineSpacing(0f, 1.0f);
            int fill = date.equals(selectedDate) ? CartoonStyle.SOFT_PEACH : Color.TRANSPARENT;
            int stroke = date.equals(today) ? CartoonStyle.OUTLINE : 0x00FFFFFF;
            int strokeDp = date.equals(today) ? 1 : 0;
            cell.setBackground(V13Ui.panel(activity, fill, stroke, strokeDp, 13));
            if (summary != null && summary.incomeCents > 0L) {
                cell.setContentDescription(date + "，支出" + summary.expenseCents + "分，有收入");
            } else {
                cell.setContentDescription(date + "，支出" + (summary == null ? 0 : summary.expenseCents) + "分");
            }
            cell.setOnClickListener(v -> {
                selectedDate = date;
                if (!YearMonth.from(date).equals(month)) month = YearMonth.from(date);
                refresh();
            });
            grid.addView(cell, cellParams(grid, V13Ui.dp(activity, 52)));
        }
    }

    private void renderDay() {
        dayList.removeAllViews();
        TransactionQueryRules.Range range = TransactionQueryRules.dayBounds(selectedDate, zone);
        List<LedgerDb.TxnView> rows = db.getTransactionsForDay(ledgerId, range.fromInclusive, range.toExclusive);
        long income = 0L;
        long expense = 0L;
        for (LedgerDb.TxnView txn : rows) {
            if (LedgerDb.TYPE_INCOME.equals(txn.type)) income += txn.amountCents;
            else if (LedgerDb.TYPE_EXPENSE.equals(txn.type)) expense += txn.amountCents;
        }
        TextView heading = V13Ui.text(activity,
                selectedDate.format(dayFormatter) + "\n收入 " + V13Ui.money(currentCurrency(), income)
                        + " · 支出 " + V13Ui.money(currentCurrency(), expense),
                15, CartoonStyle.INK, true);
        heading.setPadding(V13Ui.dp(activity, 4), V13Ui.dp(activity, 4),
                V13Ui.dp(activity, 4), V13Ui.dp(activity, 8));
        dayList.addView(heading);
        if (rows.isEmpty()) {
            TextView empty = V13Ui.text(activity, "🖍️  这天还没有记录", 15, CartoonStyle.MUTED, true);
            empty.setGravity(Gravity.CENTER);
            dayList.addView(empty, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 62)));
            TextView add = V13Ui.button(activity, "＋ 记一笔", CartoonStyle.CREAM_YELLOW);
            add.setOnClickListener(v -> quickAdd());
            dayList.addView(add, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 46)));
            return;
        }
        for (LedgerDb.TxnView txn : rows) {
            TextView row = V13Ui.transactionRow(activity, txn, false);
            row.setOnClickListener(v -> TransactionDetailDialog.show(activity, db, txn, () -> {
                if (listener != null) listener.onTransactionsChanged();
                refresh();
            }));
            row.setOnLongClickListener(v -> {
                confirmDelete(txn);
                return true;
            });
            dayList.addView(row);
            dayList.addView(V13Ui.gap(activity, 8));
        }
    }

    private void quickAdd() {
        LocalTime now = LocalTime.now();
        LocalDateTime value = LocalDateTime.of(selectedDate, now);
        if (listener != null) listener.onQuickAdd(value.atZone(zone).toInstant().toEpochMilli());
    }

    private void confirmDelete(LedgerDb.TxnView txn) {
        new AlertDialog.Builder(activity).setTitle("删除这笔账？")
                .setMessage("删除后会恢复相关账户余额。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    try {
                        db.deleteTransaction(txn.id);
                        if (listener != null) listener.onTransactionsChanged();
                        refresh();
                    } catch (RuntimeException error) {
                        Toast.makeText(activity, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private String currentCurrency() {
        LedgerDb.Ledger ledger = db.getLedger(ledgerId);
        return ledger == null ? "" : ledger.currency;
    }

    private GridLayout.LayoutParams cellParams(GridLayout parent, int height) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = height;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(V13Ui.dp(activity, 2), V13Ui.dp(activity, 1),
                V13Ui.dp(activity, 2), V13Ui.dp(activity, 1));
        return params;
    }

    private boolean handleSwipe(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchDownX = event.getX();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float delta = event.getX() - touchDownX;
            if (Math.abs(delta) > V13Ui.dp(activity, 70)) {
                setMonth(delta < 0 ? month.plusMonths(1) : month.minusMonths(1));
            }
            return true;
        }
        return true;
    }
}
