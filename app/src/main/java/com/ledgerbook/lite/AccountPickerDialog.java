package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.List;

public final class AccountPickerDialog {
    public interface Listener {
        void onAccount(LedgerDb.Account account);
    }

    private AccountPickerDialog() {
    }

    public static void show(Activity activity, String title, List<LedgerDb.Account> accounts,
                            String currency, Long excludedAccountId, Listener listener) {
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 12));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setNegativeButton("取消", null)
                .create();

        for (LedgerDb.Account account : accounts) {
            if (excludedAccountId != null && account.id == excludedAccountId) continue;
            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
            card.setBackground(panel(CartoonStyle.accountTint(account.type)));

            TextView icon = text(activity, CartoonStyle.accountIcon(account.type), 28, false);
            icon.setGravity(Gravity.CENTER);
            card.addView(icon, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));

            LinearLayout middle = new LinearLayout(activity);
            middle.setOrientation(LinearLayout.VERTICAL);
            middle.setPadding(dp(activity, 10), 0, dp(activity, 8), 0);
            middle.addView(text(activity, account.name, 16, true));
            middle.addView(text(activity, account.type + (account.groupName.isEmpty() ? "" : " · " + account.groupName), 12, false));
            card.addView(middle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView amount = text(activity, currency + " " + BigDecimal.valueOf(account.balanceCents, 2).toPlainString(), 14, true);
            amount.setTextColor(account.balanceCents < 0 ? CartoonStyle.EXPENSE : CartoonStyle.INK);
            amount.setGravity(Gravity.END);
            card.addView(amount);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, dp(activity, 4), 0, dp(activity, 4));
            list.addView(card, params);
            card.setOnClickListener(v -> {
                listener.onAccount(account);
                dialog.dismiss();
            });
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setView(scroll);
        dialog.show();
    }

    private static TextView text(Activity activity, String value, int sp, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(CartoonStyle.INK);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static GradientDrawable panel(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(48f);
        drawable.setStroke(1, 0xFFE7D5BD);
        return drawable;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
