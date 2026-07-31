package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class CategoryPickerDialog {
    public interface Listener {
        void onCategory(String icon, String label);
    }

    private CategoryPickerDialog() {
    }

    public static void show(Activity activity, String transactionType, Listener listener) {
        List<InputCatalog.Option> options = InputCatalog.categories(transactionType);
        GridLayout grid = new GridLayout(activity);
        grid.setColumnCount(3);
        grid.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(LedgerDb.TYPE_INCOME.equals(transactionType) ? "选择收入分类" : "选择支出分类")
                .setNegativeButton("取消", null)
                .create();

        for (InputCatalog.Option option : options) {
            TextView item = new TextView(activity);
            item.setText(option.icon + "\n" + option.label);
            item.setGravity(Gravity.CENTER);
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            item.setTextColor(CartoonStyle.INK);
            item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            item.setPadding(dp(activity, 6), dp(activity, 12), dp(activity, 6), dp(activity, 12));
            item.setBackground(panel(option.custom ? CartoonStyle.SOFT_PEACH : CartoonStyle.SOFT_YELLOW));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(activity, 82);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 4));
            grid.addView(item, params);
            item.setOnClickListener(v -> {
                if (option.custom) {
                    showCustom(activity, listener, dialog);
                } else {
                    listener.onCategory(option.icon, option.label);
                    dialog.dismiss();
                }
            });
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(grid, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setView(scroll);
        dialog.show();
    }

    private static void showCustom(Activity activity, Listener listener, AlertDialog parent) {
        EditText input = new EditText(activity);
        input.setHint("输入自定义分类名称");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(true);
        input.setPadding(dp(activity, 18), dp(activity, 8), dp(activity, 18), dp(activity, 8));
        AlertDialog custom = new AlertDialog.Builder(activity)
                .setTitle("自定义分类")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", null)
                .create();
        custom.setOnShowListener(ignored -> custom.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                Toast.makeText(activity, "请输入分类名称", Toast.LENGTH_SHORT).show();
                return;
            }
            listener.onCategory("✏️", value);
            custom.dismiss();
            parent.dismiss();
        }));
        custom.show();
    }

    private static GradientDrawable panel(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(42f);
        drawable.setStroke(1, 0xFFE6D3BB);
        return drawable;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
