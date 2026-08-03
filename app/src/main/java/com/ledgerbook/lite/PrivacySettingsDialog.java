package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

/** Creates, changes, removes, and configures the local PIN lock. */
public final class PrivacySettingsDialog {
    private PrivacySettingsDialog() {
    }

    public static void show(Activity activity, PinStore store, Runnable onChanged) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(V13Ui.dp(activity, 12), V13Ui.dp(activity, 8),
                V13Ui.dp(activity, 12), V13Ui.dp(activity, 18));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("🔒 隐私与安全")
                .setView(content)
                .setPositiveButton("完成", null)
                .create();
        Runnable[] render = new Runnable[1];
        render[0] = () -> render(activity, content, store, onChanged, render[0]);
        render[0].run();
        dialog.show();
    }

    private static void render(Activity activity, LinearLayout content,
                               PinStore store, Runnable onChanged, Runnable rerender) {
        content.removeAllViews();
        LinearLayout status = V13Ui.card(activity,
                store.isConfigured() ? CartoonStyle.SOFT_GREEN : CartoonStyle.SOFT_YELLOW);
        status.addView(V13Ui.text(activity,
                store.isConfigured() ? "PIN 已启用" : "尚未设置 PIN",
                17, CartoonStyle.INK, true));
        TextView detail = V13Ui.text(activity,
                store.isConfigured()
                        ? "PIN 以加盐 PBKDF2 摘要保存，不保留明文。"
                        : "设置后，应用从后台超过锁定时间返回时需要验证。",
                13, CartoonStyle.MUTED, false);
        detail.setPadding(0, V13Ui.dp(activity, 4), 0, V13Ui.dp(activity, 8));
        status.addView(detail);

        TextView primary = V13Ui.button(activity,
                store.isConfigured() ? "修改 PIN" : "设置 PIN",
                CartoonStyle.SURFACE);
        primary.setOnClickListener(v -> {
            if (store.isConfigured()) {
                verifyCurrent(activity, store, "验证当前 PIN",
                        () -> promptNewPin(activity, store, onChanged, rerender));
            } else {
                promptNewPin(activity, store, onChanged, rerender);
            }
        });
        status.addView(primary, buttonParams(activity));

        if (store.isConfigured()) {
            status.addView(V13Ui.gap(activity, 7));
            TextView remove = V13Ui.button(activity, "移除 PIN", CartoonStyle.SOFT_PEACH);
            remove.setOnClickListener(v -> verifyCurrent(activity, store,
                    "验证后移除 PIN", () -> new AlertDialog.Builder(activity)
                            .setTitle("移除 PIN？")
                            .setMessage("移除后应用将不再自动锁定。")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("移除", (ignored, which) -> {
                                store.removePin();
                                changed(onChanged, rerender);
                            }).show()));
            status.addView(remove, buttonParams(activity));
        }
        content.addView(status);
        content.addView(V13Ui.gap(activity, 10));

        LinearLayout timeoutCard = V13Ui.card(activity, CartoonStyle.SOFT_SKY);
        timeoutCard.addView(V13Ui.text(activity,
                "自动锁定时间", 17, CartoonStyle.INK, true));
        TextView timeout = V13Ui.button(activity,
                timeoutLabel(store.getTimeoutSeconds()) + " ›", CartoonStyle.SURFACE);
        timeout.setOnClickListener(v -> showTimeoutChoice(
                activity, store, timeout, onChanged));
        timeoutCard.addView(timeout, buttonParams(activity));
        content.addView(timeoutCard);
    }

    private static void promptNewPin(Activity activity, PinStore store,
                                     Runnable onChanged, Runnable rerender) {
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(V13Ui.dp(activity, 20), 0,
                V13Ui.dp(activity, 20), 0);
        EditText first = PinUnlockDialog.pinInput(activity, "输入新 PIN");
        EditText second = PinUnlockDialog.pinInput(activity, "再次输入 PIN");
        form.addView(first, fieldParams(activity));
        form.addView(V13Ui.gap(activity, 6));
        form.addView(second, fieldParams(activity));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("设置 PIN")
                .setMessage("PIN 必须为 4 至 8 位数字。")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    char[] one = first.getText().toString().toCharArray();
                    char[] two = second.getText().toString().toCharArray();
                    try {
                        PinSecurity.validatePin(one);
                        if (!Arrays.equals(one, two)) {
                            throw new IllegalArgumentException("两次输入的 PIN 不一致");
                        }
                        store.savePin(one);
                        dialog.dismiss();
                        changed(onChanged, rerender);
                    } catch (RuntimeException error) {
                        Toast.makeText(activity, message(error), Toast.LENGTH_SHORT).show();
                    } finally {
                        Arrays.fill(one, '\0');
                        Arrays.fill(two, '\0');
                    }
                }));
        dialog.show();
    }

    private static void verifyCurrent(Activity activity, PinStore store,
                                      String title, Runnable onVerified) {
        EditText input = PinUnlockDialog.pinInput(activity, "当前 PIN");
        input.setPadding(V13Ui.dp(activity, 20), V13Ui.dp(activity, 8),
                V13Ui.dp(activity, 20), V13Ui.dp(activity, 8));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("验证", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    char[] pin = input.getText().toString().toCharArray();
                    try {
                        PinStore.Verification result = store.verify(pin);
                        if (result.status == PinStore.Status.SUCCESS) {
                            dialog.dismiss();
                            onVerified.run();
                        } else if (result.status == PinStore.Status.LOCKED) {
                            long seconds = Math.max(1L,
                                    (result.remainingMillis + 999L) / 1000L);
                            Toast.makeText(activity,
                                    "尝试次数过多，请 " + seconds + " 秒后再试",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            input.setText("");
                            Toast.makeText(activity, "PIN 不正确", Toast.LENGTH_SHORT).show();
                        }
                    } finally {
                        Arrays.fill(pin, '\0');
                    }
                }));
        dialog.show();
    }

    private static void showTimeoutChoice(Activity activity, PinStore store,
                                          TextView row, Runnable onChanged) {
        int[] values = {0, 30, 60, 300, 900};
        String[] labels = {
                "立即锁定", "30 秒", "1 分钟", "5 分钟", "15 分钟"
        };
        int selected = 2;
        for (int index = 0; index < values.length; index++) {
            if (values[index] == store.getTimeoutSeconds()) selected = index;
        }
        new AlertDialog.Builder(activity)
                .setTitle("自动锁定时间")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    store.setTimeoutSeconds(values[which]);
                    row.setText(timeoutLabel(values[which]) + " ›");
                    if (onChanged != null) onChanged.run();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private static String timeoutLabel(int seconds) {
        if (seconds == 0) return "立即锁定";
        if (seconds == 30) return "30 秒后锁定";
        if (seconds == 60) return "1 分钟后锁定";
        if (seconds == 300) return "5 分钟后锁定";
        return "15 分钟后锁定";
    }

    private static LinearLayout.LayoutParams buttonParams(Activity activity) {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 46));
    }

    private static LinearLayout.LayoutParams fieldParams(Activity activity) {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 54));
    }

    private static void changed(Runnable onChanged, Runnable rerender) {
        if (onChanged != null) onChanged.run();
        rerender.run();
    }

    private static String message(RuntimeException error) {
        String value = error.getMessage();
        return value == null || value.trim().isEmpty() ? "PIN 操作失败" : value;
    }
}
