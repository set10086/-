package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;

public final class AmountCalculatorDialog {
    public interface Listener {
        void onAmount(long cents);
    }

    private AmountCalculatorDialog() {
    }

    public static void show(Activity activity, String title, long initialCents, Listener listener) {
        StringBuilder expression = new StringBuilder(initialCents > 0
                ? BigDecimal.valueOf(initialCents, 2).stripTrailingZeros().toPlainString() : "");

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 18), dp(activity, 12), dp(activity, 18), dp(activity, 8));

        TextView expressionView = text(activity, expression.length() == 0 ? "0" : expression.toString(), 27, true);
        expressionView.setGravity(Gravity.END);
        expressionView.setPadding(dp(activity, 12), dp(activity, 14), dp(activity, 12), dp(activity, 4));
        expressionView.setBackground(panel(CartoonStyle.SURFACE, 18));
        root.addView(expressionView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 66)));

        TextView resultView = text(activity, "= 0.00", 17, true);
        resultView.setTextColor(CartoonStyle.INCOME);
        resultView.setGravity(Gravity.END);
        resultView.setPadding(0, dp(activity, 8), dp(activity, 6), dp(activity, 10));
        root.addView(resultView);

        String[][] keys = {
                {"7", "8", "9", "÷"},
                {"4", "5", "6", "×"},
                {"1", "2", "3", "−"},
                {"0", ".", "⌫", "+"},
                {"清空", "00", "", ""}
        };

        for (String[] rowKeys : keys) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (String key : rowKeys) {
                if (key.isEmpty()) {
                    row.addView(new View(activity), new LinearLayout.LayoutParams(0, dp(activity, 54), 1f));
                    continue;
                }
                Button button = new Button(activity);
                button.setText(key);
                button.setTextSize(TypedValue.COMPLEX_UNIT_SP, key.length() > 1 ? 15 : 20);
                button.setTextColor(CartoonStyle.INK);
                button.setAllCaps(false);
                int fill = isOperator(key) ? CartoonStyle.SOFT_PEACH : CartoonStyle.SOFT_YELLOW;
                button.setBackground(panel(fill, 17));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(activity, 54), 1f);
                params.setMargins(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 4));
                row.addView(button, params);
                button.setOnClickListener(v -> {
                    applyKey(expression, key);
                    expressionView.setText(expression.length() == 0 ? "0" : expression.toString());
                    updatePreview(expression, resultView);
                });
            }
            root.addView(row);
        }
        updatePreview(expression, resultView);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(root)
                .setNegativeButton("取消", null)
                .setPositiveButton("完成", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                BigDecimal value = AmountExpression.evaluate(expression.toString());
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("金额必须大于零");
                }
                long cents = value.movePointRight(2).longValueExact();
                listener.onAmount(cents);
                dialog.dismiss();
            } catch (RuntimeException error) {
                Toast.makeText(activity, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
    }

    private static void applyKey(StringBuilder expression, String key) {
        if ("清空".equals(key)) {
            expression.setLength(0);
            return;
        }
        if ("⌫".equals(key)) {
            if (expression.length() > 0) expression.deleteCharAt(expression.length() - 1);
            return;
        }
        if (expression.length() >= 40) return;
        if (isOperator(key)) {
            if (expression.length() == 0) return;
            char last = expression.charAt(expression.length() - 1);
            if (isOperator(String.valueOf(last))) expression.deleteCharAt(expression.length() - 1);
            expression.append(key);
            return;
        }
        if (".".equals(key)) {
            if (expression.length() == 0 || isOperator(String.valueOf(expression.charAt(expression.length() - 1)))) {
                expression.append("0.");
                return;
            }
            int index = expression.length() - 1;
            while (index >= 0 && !isOperator(String.valueOf(expression.charAt(index)))) {
                if (expression.charAt(index) == '.') return;
                index--;
            }
            expression.append('.');
            return;
        }
        expression.append(key);
    }

    private static void updatePreview(StringBuilder expression, TextView view) {
        if (expression.length() == 0 || isOperator(String.valueOf(expression.charAt(expression.length() - 1)))) {
            view.setText("= --");
            return;
        }
        try {
            view.setText("= " + AmountExpression.evaluate(expression.toString()).toPlainString());
        } catch (RuntimeException error) {
            view.setText("= --");
        }
    }

    private static boolean isOperator(String key) {
        return "+".equals(key) || "−".equals(key) || "×".equals(key) || "÷".equals(key)
                || "-".equals(key) || "*".equals(key) || "/".equals(key);
    }

    private static TextView text(Activity activity, String value, int sp, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(CartoonStyle.INK);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static GradientDrawable panel(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp * 3f);
        drawable.setStroke(1, 0xFFE6D3BB);
        return drawable;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
