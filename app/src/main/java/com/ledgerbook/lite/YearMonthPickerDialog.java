package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.time.YearMonth;

public final class YearMonthPickerDialog {
    public interface Listener {
        void onSelected(YearMonth month);
    }

    private YearMonthPickerDialog() {
    }

    public static void show(Activity activity, YearMonth initial, Listener listener) {
        YearMonth now = YearMonth.now();
        YearMonth value = YearMonthPickerRules.clamp(initial == null ? now : initial, now.getYear());

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 18), dp(activity, 8), dp(activity, 18), dp(activity, 8));

        LinearLayout wheels = new LinearLayout(activity);
        wheels.setGravity(Gravity.CENTER);
        NumberPicker year = new NumberPicker(activity);
        year.setMinValue(YearMonthPickerRules.minYear(now.getYear()));
        year.setMaxValue(YearMonthPickerRules.maxYear(now.getYear()));
        year.setValue(value.getYear());
        year.setWrapSelectorWheel(false);
        year.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        NumberPicker month = new NumberPicker(activity);
        month.setMinValue(1);
        month.setMaxValue(12);
        month.setValue(value.getMonthValue());
        month.setDisplayedValues(new String[]{"01 月", "02 月", "03 月", "04 月", "05 月", "06 月",
                "07 月", "08 月", "09 月", "10 月", "11 月", "12 月"});
        month.setWrapSelectorWheel(true);
        month.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        LinearLayout yearBox = wheelBox(activity, "年份", year, CartoonStyle.SOFT_YELLOW);
        LinearLayout monthBox = wheelBox(activity, "月份", month, CartoonStyle.SOFT_SKY);
        LinearLayout.LayoutParams wheelParams = new LinearLayout.LayoutParams(
                0, dp(activity, 220), 1f);
        wheelParams.setMargins(dp(activity, 4), 0, dp(activity, 4), 0);
        wheels.addView(yearBox, wheelParams);
        wheels.addView(monthBox, wheelParams);
        root.addView(wheels);

        TextView hint = text(activity, "可直接滑动多年，确认后立即跳转", 12,
                CartoonStyle.MUTED, false);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 36)));

        LinearLayout shortcuts = new LinearLayout(activity);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        addShortcut(activity, shortcuts, "本月", CartoonStyle.SOFT_GREEN,
                () -> set(year, month, now));
        addShortcut(activity, shortcuts, "上月", CartoonStyle.SOFT_PEACH,
                () -> set(year, month, YearMonthPickerRules.previousMonth(now)));
        addShortcut(activity, shortcuts, "今年", CartoonStyle.SOFT_YELLOW,
                () -> set(year, month, YearMonthPickerRules.currentYearJanuary(now.getYear())));
        addShortcut(activity, shortcuts, "去年", CartoonStyle.SOFT_LAVENDER,
                () -> set(year, month, YearMonthPickerRules.previousYearJanuary(now.getYear())));
        root.addView(shortcuts, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 48)));

        new AlertDialog.Builder(activity)
                .setTitle("📅 选择年月")
                .setView(root)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> listener.onSelected(
                        YearMonth.of(year.getValue(), month.getValue())))
                .show();
    }

    private static LinearLayout wheelBox(Activity activity, String label,
                                         NumberPicker picker, int fill) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
        box.setBackground(panel(activity, fill, 19));
        TextView title = text(activity, label, 14, CartoonStyle.INK, true);
        title.setGravity(Gravity.CENTER);
        box.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 34)));
        box.addView(picker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return box;
    }

    private static void addShortcut(Activity activity, LinearLayout row, String label,
                                    int fill, Runnable action) {
        TextView button = text(activity, label, 13, CartoonStyle.INK, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(panel(activity, fill, 15));
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        params.setMargins(dp(activity, 3), dp(activity, 3), dp(activity, 3), dp(activity, 3));
        row.addView(button, params);
    }

    private static void set(NumberPicker year, NumberPicker month, YearMonth value) {
        year.setValue(value.getYear());
        month.setValue(value.getMonthValue());
    }

    private static TextView text(Activity activity, String value, int sp, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static GradientDrawable panel(Activity activity, int fill, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(activity, radiusDp));
        drawable.setStroke(dp(activity, 1), 0xFFE4D5C2);
        return drawable;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
