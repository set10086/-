package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Interactive module switchboard. All changes are persisted immediately. */
public final class ModuleCenterDialog {
    private ModuleCenterDialog() {
    }

    public static void show(Activity activity, ModuleRepository repository, Runnable onChanged) {
        if (activity == null) throw new IllegalArgumentException("activity cannot be null");
        if (repository == null) throw new IllegalArgumentException("repository cannot be null");

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 12), dp(activity, 8),
                dp(activity, 12), dp(activity, 18));

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("🧩 模块中心")
                .setView(scroll)
                .setPositiveButton("完成", null)
                .create();

        Runnable[] render = new Runnable[1];
        render[0] = () -> renderModules(activity, content, repository, onChanged, render[0]);
        render[0].run();
        dialog.show();
    }

    private static void renderModules(Activity activity, LinearLayout content,
                                      ModuleRepository repository, Runnable onChanged,
                                      Runnable rerender) {
        content.removeAllViews();
        TextView explanation = V13Ui.text(activity,
                "默认保持简洁。关闭模块只隐藏入口，不会删除已经保存的数据。",
                13, CartoonStyle.MUTED, false);
        explanation.setLineSpacing(0f, 1.15f);
        explanation.setPadding(dp(activity, 4), 0, dp(activity, 4), dp(activity, 10));
        content.addView(explanation);

        List<ModuleRepository.Module> modules = repository.list();
        for (int index = 0; index < modules.size(); index++) {
            ModuleRepository.Module module = modules.get(index);
            LinearLayout card = V13Ui.card(activity, cardFill(index));

            LinearLayout heading = new LinearLayout(activity);
            heading.setOrientation(LinearLayout.HORIZONTAL);
            heading.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout labels = new LinearLayout(activity);
            labels.setOrientation(LinearLayout.VERTICAL);
            TextView title = V13Ui.text(activity,
                    moduleIcon(module.key) + "  " + ModuleRepository.titleFor(module.key),
                    16, CartoonStyle.INK, true);
            TextView description = V13Ui.text(activity,
                    ModuleRepository.descriptionFor(module.key),
                    12, CartoonStyle.MUTED, false);
            description.setPadding(0, dp(activity, 2), dp(activity, 8), 0);
            labels.addView(title);
            labels.addView(description);
            heading.addView(labels, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            CheckBox enabled = new CheckBox(activity);
            enabled.setChecked(module.enabled);
            enabled.setContentDescription(ModuleRepository.titleFor(module.key)
                    + (module.enabled ? "，已启用" : "，已关闭"));
            enabled.setMinWidth(dp(activity, 48));
            enabled.setMinHeight(dp(activity, 48));
            enabled.setOnCheckedChangeListener((button, checked) -> {
                try {
                    repository.setEnabled(module.key, checked);
                    button.setContentDescription(ModuleRepository.titleFor(module.key)
                            + (checked ? "，已启用" : "，已关闭"));
                    if (onChanged != null) onChanged.run();
                } catch (RuntimeException error) {
                    button.setOnCheckedChangeListener(null);
                    button.setChecked(!checked);
                    Toast.makeText(activity, safeMessage(error), Toast.LENGTH_SHORT).show();
                }
            });
            heading.addView(enabled, new LinearLayout.LayoutParams(
                    dp(activity, 52), dp(activity, 52)));
            card.addView(heading);

            LinearLayout order = new LinearLayout(activity);
            order.setOrientation(LinearLayout.HORIZONTAL);
            order.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            order.setPadding(0, dp(activity, 7), 0, 0);
            TextView position = V13Ui.text(activity,
                    "显示顺序 " + (index + 1), 12, CartoonStyle.MUTED, false);
            order.addView(position, new LinearLayout.LayoutParams(
                    0, dp(activity, 40), 1f));

            TextView up = orderButton(activity, "↑ 上移", index > 0);
            up.setOnClickListener(v -> move(activity, repository, module.key,
                    -1, onChanged, rerender));
            order.addView(up, new LinearLayout.LayoutParams(
                    dp(activity, 76), dp(activity, 40)));
            order.addView(V13Ui.gap(activity, 1),
                    new LinearLayout.LayoutParams(dp(activity, 7), 1));
            TextView down = orderButton(activity, "↓ 下移", index < modules.size() - 1);
            down.setOnClickListener(v -> move(activity, repository, module.key,
                    1, onChanged, rerender));
            order.addView(down, new LinearLayout.LayoutParams(
                    dp(activity, 76), dp(activity, 40)));
            card.addView(order);

            content.addView(card, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            content.addView(V13Ui.gap(activity, 8));
        }
    }

    private static void move(Activity activity, ModuleRepository repository, String key,
                             int direction, Runnable onChanged, Runnable rerender) {
        try {
            repository.move(key, direction);
            if (onChanged != null) onChanged.run();
            rerender.run();
        } catch (RuntimeException error) {
            Toast.makeText(activity, safeMessage(error), Toast.LENGTH_SHORT).show();
        }
    }

    private static TextView orderButton(Activity activity, String label, boolean enabled) {
        TextView button = V13Ui.text(activity, label, 13, CartoonStyle.INK, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(V13Ui.panel(activity, CartoonStyle.SURFACE,
                0xFFE1CDB4, 1, 14));
        button.setClickable(enabled);
        button.setFocusable(enabled);
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.38f);
        return button;
    }

    private static int cardFill(int index) {
        int[] fills = {CartoonStyle.SOFT_YELLOW, CartoonStyle.SOFT_PEACH,
                CartoonStyle.SOFT_SKY, CartoonStyle.SOFT_GREEN,
                CartoonStyle.SOFT_LAVENDER};
        return fills[index % fills.length];
    }

    private static String moduleIcon(String key) {
        switch (key) {
            case ModuleRepository.QUICK_ENTRY: return "⚡";
            case ModuleRepository.BUDGET: return "🎯";
            case ModuleRepository.DATA_MANAGEMENT: return "💾";
            case ModuleRepository.TEMPLATES: return "📌";
            case ModuleRepository.RECURRING: return "🔁";
            case ModuleRepository.SUBSCRIPTIONS: return "🗓️";
            case ModuleRepository.PRIVACY_LOCK: return "🔒";
            default: return "🧩";
        }
    }

    private static String safeMessage(RuntimeException error) {
        String value = error.getMessage();
        return value == null || value.trim().isEmpty() ? "操作失败" : value;
    }

    private static int dp(Activity activity, int value) {
        return V13Ui.dp(activity, value);
    }
}
