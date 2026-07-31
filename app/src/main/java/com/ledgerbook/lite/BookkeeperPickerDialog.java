package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BookkeeperPickerDialog {
    private static final String PREFS = "ledgerbook_bookkeepers";
    private static final String KEY_HISTORY = "history";

    public interface Listener {
        void onBookkeeper(String value);
    }

    private BookkeeperPickerDialog() {
    }

    public static void show(Activity activity, Listener listener) {
        SharedPreferences preferences = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        Set<String> stored = preferences.getStringSet(KEY_HISTORY, new LinkedHashSet<>());
        List<String> values = InputCatalog.mergeBookkeepers(stored);
        String[] labels = new String[values.size() + 1];
        for (int index = 0; index < values.size(); index++) {
            labels[index] = avatar(values.get(index)) + "  " + values.get(index);
        }
        labels[labels.length - 1] = "✏️  自定义记账人";

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("选择记账人")
                .setItems(labels, (d, which) -> {
                    if (which == values.size()) {
                        showCustom(activity, preferences, listener);
                    } else {
                        listener.onBookkeeper(values.get(which));
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
    }

    private static void showCustom(Activity activity, SharedPreferences preferences, Listener listener) {
        EditText input = new EditText(activity);
        input.setHint("输入姓名或称呼");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("新增记账人")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                Toast.makeText(activity, "请输入记账人名称", Toast.LENGTH_SHORT).show();
                return;
            }
            Set<String> history = new LinkedHashSet<>(preferences.getStringSet(KEY_HISTORY, new LinkedHashSet<>()));
            history.add(value);
            preferences.edit().putStringSet(KEY_HISTORY, history).apply();
            listener.onBookkeeper(value);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private static String avatar(String value) {
        if ("本人".equals(value)) return "🙂";
        if ("家人".equals(value)) return "🏡";
        if ("伴侣".equals(value)) return "💞";
        if ("孩子".equals(value)) return "🧸";
        return "👤";
    }
}
