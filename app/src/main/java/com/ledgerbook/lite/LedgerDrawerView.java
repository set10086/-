package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class LedgerDrawerView extends LinearLayout {
    public interface Listener {
        void onLedgerSelected(long ledgerId);
        void onLedgerListChanged(long ledgerId);
        void onClose();
    }

    private final Activity activity;
    private final LedgerDb db;
    private final Listener listener;
    private final LinearLayout list;
    private long currentLedgerId;

    public LedgerDrawerView(Activity activity, LedgerDb db, long currentLedgerId, Listener listener) {
        super(activity);
        this.activity = activity;
        this.db = db;
        this.currentLedgerId = currentLedgerId;
        this.listener = listener;
        setOrientation(VERTICAL);
        setBackgroundColor(CartoonStyle.BACKGROUND);
        setPadding(V13Ui.dp(activity, 14), V13Ui.dp(activity, 14),
                V13Ui.dp(activity, 14), V13Ui.dp(activity, 14));

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = V13Ui.text(activity, "📒  我的账本", 21, CartoonStyle.INK, true);
        header.addView(title, new LayoutParams(0, V13Ui.dp(activity, 48), 1f));
        TextView close = V13Ui.button(activity, "×", CartoonStyle.SURFACE);
        close.setTextSize(22);
        close.setOnClickListener(v -> listener.onClose());
        header.addView(close, new LayoutParams(V13Ui.dp(activity, 48), V13Ui.dp(activity, 44)));
        addView(header);
        addView(V13Ui.gap(activity, 8));

        ScrollView scroll = new ScrollView(activity);
        list = new LinearLayout(activity);
        list.setOrientation(VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView manage = V13Ui.button(activity, "⚙  账本管理", CartoonStyle.CREAM_YELLOW);
        manage.setOnClickListener(v -> showManagement());
        addView(manage, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 52)));
        refresh();
    }

    public void setCurrentLedger(long ledgerId) {
        currentLedgerId = ledgerId;
        refresh();
    }

    public void refresh() {
        list.removeAllViews();
        List<LedgerDb.Ledger> ledgers = db.getLedgers();
        for (LedgerDb.Ledger ledger : ledgers) {
            LedgerDb.Summary summary = db.getCurrentMonthSummary(ledger.id);
            boolean current = ledger.id == currentLedgerId;
            LinearLayout card = V13Ui.card(activity,
                    current ? CartoonStyle.SOFT_YELLOW : CartoonStyle.SURFACE);
            String badge = current ? "  · 当前" : "";
            card.addView(V13Ui.text(activity, "📒  " + ledger.name + badge,
                    17, CartoonStyle.INK, true));
            card.addView(V13Ui.text(activity,
                    ledger.currency + "  收入 " + V13Ui.money(ledger.currency, summary.incomeCents)
                            + "  支出 " + V13Ui.money(ledger.currency, summary.expenseCents),
                    12, CartoonStyle.MUTED, false));
            card.setOnClickListener(v -> {
                currentLedgerId = ledger.id;
                listener.onLedgerSelected(ledger.id);
                listener.onClose();
            });
            list.addView(card);
            list.addView(V13Ui.gap(activity, 8));
        }
    }

    private void showManagement() {
        List<LedgerDb.Ledger> ledgers = db.getLedgers();
        String[] items = new String[ledgers.size() + 1];
        items[0] = "＋ 新建账本";
        for (int index = 0; index < ledgers.size(); index++) {
            items[index + 1] = "📒 " + ledgers.get(index).name + " · " + ledgers.get(index).currency;
        }
        new AlertDialog.Builder(activity).setTitle("账本管理")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) showCreate();
                    else showLedgerActions(ledgers.get(which - 1));
                }).setNegativeButton("关闭", null).show();
    }

    private void showCreate() {
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(VERTICAL);
        form.setPadding(V13Ui.dp(activity, 18), V13Ui.dp(activity, 8),
                V13Ui.dp(activity, 18), V13Ui.dp(activity, 8));
        EditText name = new EditText(activity);
        name.setHint("例如：家庭账本");
        Spinner currency = new Spinner(activity);
        String[] currencies = {"CNY", "JPY", "USD", "EUR", "GBP", "HKD"};
        currency.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, currencies));
        form.addView(label("账本名称"));
        form.addView(name);
        form.addView(label("本位币"));
        form.addView(currency);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("新建账本")
                .setView(form).setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        long id = db.addLedger(name.getText().toString(),
                                currency.getSelectedItem().toString());
                        currentLedgerId = id;
                        dialog.dismiss();
                        refresh();
                        listener.onLedgerListChanged(id);
                    } catch (RuntimeException error) {
                        toast(error.getMessage());
                    }
                }));
        dialog.show();
    }

    private void showLedgerActions(LedgerDb.Ledger ledger) {
        String[] actions = {"切换到账本", "重命名", "删除账本"};
        new AlertDialog.Builder(activity).setTitle(ledger.name)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        currentLedgerId = ledger.id;
                        listener.onLedgerSelected(ledger.id);
                        listener.onClose();
                    } else if (which == 1) {
                        showRename(ledger);
                    } else {
                        showDelete(ledger);
                    }
                }).setNegativeButton("取消", null).show();
    }

    private void showRename(LedgerDb.Ledger ledger) {
        EditText name = new EditText(activity);
        name.setInputType(InputType.TYPE_CLASS_TEXT);
        name.setText(ledger.name);
        name.setSelectAllOnFocus(true);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("重命名账本")
                .setView(name).setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        db.renameLedger(ledger.id, name.getText().toString());
                        dialog.dismiss();
                        refresh();
                        listener.onLedgerListChanged(currentLedgerId);
                    } catch (RuntimeException error) {
                        toast(error.getMessage());
                    }
                }));
        dialog.show();
    }

    private void showDelete(LedgerDb.Ledger ledger) {
        try {
            LedgerDb.LedgerCounts counts = db.getLedgerCounts(ledger.id);
            new AlertDialog.Builder(activity).setTitle("删除“" + ledger.name + "”？")
                    .setMessage("该账本包含 " + counts.accountCount + " 个账户和 "
                            + counts.transactionCount + " 笔账单。删除后无法恢复。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("继续", (dialog, which) ->
                            new AlertDialog.Builder(activity).setTitle("再次确认删除")
                                    .setMessage("账本、账户和账单将永久删除。")
                                    .setNegativeButton("取消", null)
                                    .setPositiveButton("确认删除", (confirm, ignored) -> {
                                        try {
                                            long next = db.deleteLedger(ledger.id);
                                            if (ledger.id == currentLedgerId) currentLedgerId = next;
                                            refresh();
                                            listener.onLedgerListChanged(currentLedgerId);
                                        } catch (RuntimeException error) {
                                            toast(error.getMessage());
                                        }
                                    }).show()).show();
        } catch (RuntimeException error) {
            toast(error.getMessage());
        }
    }

    private TextView label(String value) {
        TextView view = V13Ui.text(activity, value, 13, CartoonStyle.MUTED, true);
        view.setPadding(0, V13Ui.dp(activity, 8), 0, V13Ui.dp(activity, 2));
        return view;
    }

    private void toast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
}
