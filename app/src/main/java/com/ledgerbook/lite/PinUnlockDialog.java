package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;

/** Blocks app access until the configured PIN is verified. */
public final class PinUnlockDialog {
    private PinUnlockDialog() {
    }

    public static void show(Activity activity, PinStore store, Runnable onUnlocked) {
        if (activity == null || store == null) {
            throw new IllegalArgumentException("activity and store are required");
        }
        if (!store.isConfigured()) {
            store.markUnlocked();
            if (onUnlocked != null) onUnlocked.run();
            return;
        }

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(V13Ui.dp(activity, 20), V13Ui.dp(activity, 4),
                V13Ui.dp(activity, 20), V13Ui.dp(activity, 4));
        TextView message = V13Ui.text(activity,
                "输入 4 至 8 位数字 PIN 解锁", 13, CartoonStyle.MUTED, false);
        content.addView(message);
        EditText input = pinInput(activity, "PIN");
        content.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 54)));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("🔒 LedgerBook 已锁定")
                .setView(content)
                .setPositiveButton("解锁", null)
                .create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(ignored -> {
            input.requestFocus();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                char[] pin = input.getText().toString().toCharArray();
                try {
                    PinStore.Verification result = store.verify(pin);
                    if (result.status == PinStore.Status.SUCCESS) {
                        dialog.dismiss();
                        if (onUnlocked != null) onUnlocked.run();
                        return;
                    }
                    if (result.status == PinStore.Status.LOCKED) {
                        long seconds = Math.max(1L,
                                (result.remainingMillis + 999L) / 1000L);
                        message.setText("尝试次数过多，请 " + seconds + " 秒后再试");
                        input.setEnabled(false);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        input.postDelayed(() -> {
                            input.setEnabled(true);
                            input.setText("");
                            message.setText("请输入 PIN 解锁");
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            input.requestFocus();
                        }, result.remainingMillis);
                    } else {
                        message.setText("PIN 不正确，请重试");
                        input.setText("");
                        input.requestFocus();
                    }
                } catch (RuntimeException error) {
                    message.setText(error.getMessage() == null
                            ? "PIN 验证失败" : error.getMessage());
                    input.setText("");
                } finally {
                    Arrays.fill(pin, '\0');
                }
            });
        });
        dialog.show();
    }

    static EditText pinInput(Activity activity, String hint) {
        EditText input = new EditText(activity);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setPadding(V13Ui.dp(activity, 14), V13Ui.dp(activity, 8),
                V13Ui.dp(activity, 14), V13Ui.dp(activity, 8));
        return input;
    }
}
