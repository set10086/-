package com.ledgerbook.lite;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class V13Ui {
    private V13Ui() {
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context context, String value, int sp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    static TextView button(Context context, String value, int fill) {
        TextView view = text(context, value, 15, CartoonStyle.INK, true);
        view.setGravity(Gravity.CENTER);
        if (value != null && value.startsWith("📒")) {
            view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            view.setSingleLine(true);
            view.setEllipsize(TextUtils.TruncateAt.END);
            view.setPadding(dp(context, 10), dp(context, 9), dp(context, 62), dp(context, 9));
        } else {
            view.setPadding(dp(context, 12), dp(context, 9), dp(context, 12), dp(context, 9));
        }
        view.setBackground(panel(context, fill, 0xFFE4D5C2, 1, 18));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    static LinearLayout card(Context context, int fill) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        card.setBackground(panel(context, fill, 0xFFE5D7C3, 1, 20));
        return card;
    }

    static GradientDrawable panel(Context context, int fill, int stroke, int strokeDp, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(context, strokeDp), stroke);
        return drawable;
    }

    static View gap(Context context, int heightDp) {
        View gap = new View(context);
        gap.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, heightDp)));
        return gap;
    }

    static String money(String currency, long cents) {
        String unit = currency == null || currency.trim().isEmpty() ? "" : currency.trim() + " ";
        return unit + BigDecimal.valueOf(cents, 2).toPlainString();
    }

    static String dateTime(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    static String day(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(millis));
    }

    static String typeName(String type) {
        if (LedgerDb.TYPE_EXPENSE.equals(type)) return "支出";
        if (LedgerDb.TYPE_INCOME.equals(type)) return "收入";
        return "转账";
    }

    static TextView transactionRow(Context context, LedgerDb.TxnView txn, boolean showLedger) {
        String account = txn.accountName;
        if (LedgerDb.TYPE_TRANSFER.equals(txn.type)) account += " → " + txn.toAccountName;
        String meta = account + " · " + V13Ui.dateTime(txn.occurredAt);
        if (showLedger) meta = txn.ledgerName + " · " + meta;
        String prefix = LedgerDb.TYPE_EXPENSE.equals(txn.type) ? "−"
                : LedgerDb.TYPE_INCOME.equals(txn.type) ? "+" : "";
        TextView row = text(context,
                txn.category + "\n" + meta + "\n" + prefix + money(txn.currency, txn.amountCents),
                14, CartoonStyle.INK, true);
        Drawable icon = CrayonIconView.drawable(txn.type, txn.category);
        int iconSize = dp(context, 48);
        icon.setBounds(0, 0, iconSize, iconSize);
        row.setCompoundDrawables(icon, null, null, null);
        row.setCompoundDrawablePadding(dp(context, 10));
        row.setLineSpacing(0f, 1.14f);
        row.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        row.setBackground(panel(context, CartoonStyle.SURFACE, 0xFFE5D7C3, 1, 17));
        return row;
    }
}
