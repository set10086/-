package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Calendar;

public final class WheelDateTimeDialog {
    public interface Listener {
        void onDateTime(long timestamp);
    }

    private WheelDateTimeDialog() {
    }

    public static void show(Activity activity, long initialTimestamp, Listener listener) {
        Calendar initial = Calendar.getInstance();
        initial.setTimeInMillis(initialTimestamp);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 12), dp(activity, 6), dp(activity, 12), dp(activity, 4));

        LinearLayout dateRow = new LinearLayout(activity);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setGravity(Gravity.CENTER);
        NumberPicker year = picker(activity, initial.get(Calendar.YEAR) - 5,
                initial.get(Calendar.YEAR) + 10, initial.get(Calendar.YEAR), "年");
        NumberPicker month = picker(activity, 1, 12, initial.get(Calendar.MONTH) + 1, "月");
        NumberPicker day = picker(activity, 1,
                DateWheelRules.daysInMonth(initial.get(Calendar.YEAR), initial.get(Calendar.MONTH) + 1),
                initial.get(Calendar.DAY_OF_MONTH), "日");
        dateRow.addView(year, weight());
        dateRow.addView(month, weight());
        dateRow.addView(day, weight());
        root.addView(label(activity, "日期"));
        root.addView(dateRow);

        LinearLayout timeRow = new LinearLayout(activity);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER);
        NumberPicker hour = picker(activity, 0, 23, initial.get(Calendar.HOUR_OF_DAY), "时");
        NumberPicker minute = picker(activity, 0, 59, initial.get(Calendar.MINUTE), "分");
        timeRow.addView(hour, weight());
        timeRow.addView(minute, weight());
        root.addView(label(activity, "时间"));
        root.addView(timeRow);

        NumberPicker.OnValueChangeListener updateDays = (picker, oldValue, newValue) -> {
            int max = DateWheelRules.daysInMonth(year.getValue(), month.getValue());
            int current = day.getValue();
            day.setMaxValue(max);
            if (current > max) day.setValue(max);
        };
        year.setOnValueChangedListener(updateDays);
        month.setOnValueChangedListener(updateDays);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("选择记账时间")
                .setView(root)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (d, which) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year.getValue());
                    selected.set(Calendar.MONTH, month.getValue() - 1);
                    selected.set(Calendar.DAY_OF_MONTH, day.getValue());
                    selected.set(Calendar.HOUR_OF_DAY, hour.getValue());
                    selected.set(Calendar.MINUTE, minute.getValue());
                    selected.set(Calendar.SECOND, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    listener.onDateTime(selected.getTimeInMillis());
                })
                .create();
        dialog.show();
    }

    private static NumberPicker picker(Activity activity, int min, int max, int value, String suffix) {
        NumberPicker picker = new NumberPicker(activity);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(Math.max(min, Math.min(max, value)));
        picker.setWrapSelectorWheel(true);
        picker.setFormatter(number -> String.format(java.util.Locale.CHINA, "%02d%s", number, suffix));
        return picker;
    }

    private static TextView label(Activity activity, String value) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setTextColor(CartoonStyle.MUTED);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(dp(activity, 6), dp(activity, 8), 0, 0);
        return view;
    }

    private static LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, dpStatic(150), 1f);
    }

    private static int dpStatic(int value) {
        return value * 3;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
