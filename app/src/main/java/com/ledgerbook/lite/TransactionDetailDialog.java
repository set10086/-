package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class TransactionDetailDialog {
    private TransactionDetailDialog() {
    }

    public static void show(Activity activity, LedgerDb db, LedgerDb.TxnView txn,
                            Runnable onDeleted) {
        if (txn == null) {
            Toast.makeText(activity, "账单不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(V13Ui.dp(activity, 18), V13Ui.dp(activity, 8),
                V13Ui.dp(activity, 18), V13Ui.dp(activity, 16));

        String account = txn.accountName;
        if (LedgerDb.TYPE_TRANSFER.equals(txn.type)) account += " → " + txn.toAccountName;
        add(content, "类型", V13Ui.typeName(txn.type));
        add(content, "分类", CartoonStyle.transactionIcon(txn.type, txn.category) + "  " + txn.category);
        add(content, "金额", V13Ui.money(txn.currency, txn.amountCents));
        if (txn.discountCents > 0) add(content, "优惠", V13Ui.money(txn.currency, txn.discountCents));
        add(content, "账本", txn.ledgerName + " · " + txn.currency);
        add(content, "账户", account);
        add(content, "时间", V13Ui.dateTime(txn.occurredAt));
        add(content, "记账人", txn.bookkeeper);
        if (!txn.tags.trim().isEmpty()) add(content, "标签", txn.tags);
        if (!txn.note.trim().isEmpty()) add(content, "备注", txn.note);
        add(content, "报销", txn.reimbursable ? "需要报销" : "不报销");
        add(content, "预算", txn.includeBudget ? "计入预算" : "不计入预算");

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("账单详情")
                .setView(scroll)
                .setNegativeButton("关闭", null)
                .setPositiveButton("删除", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(CartoonStyle.EXPENSE));
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(CartoonStyle.EXPENSE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v ->
                    new AlertDialog.Builder(activity)
                            .setTitle("删除这笔账？")
                            .setMessage("删除后会恢复相关账户余额，此操作无法撤销。")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("删除", (confirm, which) -> {
                                try {
                                    db.deleteTransaction(txn.id);
                                    dialog.dismiss();
                                    if (onDeleted != null) onDeleted.run();
                                } catch (RuntimeException error) {
                                    Toast.makeText(activity, error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }).show());
        });
        dialog.show();
    }

    private static void add(LinearLayout parent, String label, String value) {
        LinearLayout card = V13Ui.card(parent.getContext(), CartoonStyle.SURFACE);
        TextView title = V13Ui.text(parent.getContext(), label, 12, CartoonStyle.MUTED, true);
        TextView body = V13Ui.text(parent.getContext(), value, 16, CartoonStyle.INK, true);
        card.addView(title);
        card.addView(body);
        parent.addView(card);
        parent.addView(V13Ui.gap(parent.getContext(), 8));
    }
}
