package com.ledgerbook.lite;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Creates up to three reusable template shortcuts for the home page. */
public final class TemplateHomeCard {
    private TemplateHomeCard() {
    }

    public static View create(Activity activity, LedgerDb db,
                              long ledgerId, Runnable onChanged) {
        TemplateRepository repository = new TemplateRepository(db);
        List<TemplateRepository.Template> templates = repository.list(ledgerId);
        LinearLayout card = V13Ui.card(activity, CartoonStyle.SOFT_LAVENDER);
        card.addView(V13Ui.text(activity, "📌 常用模板", 17,
                CartoonStyle.INK, true));
        TextView hint = V13Ui.text(activity,
                templates.isEmpty() ? "保存常用账单后可从这里快速记账。"
                        : "点击模板，确认后按当前时间生成账单。",
                13, CartoonStyle.MUTED, false);
        hint.setPadding(0, V13Ui.dp(activity, 4), 0, V13Ui.dp(activity, 8));
        card.addView(hint);

        if (templates.isEmpty()) {
            TextView manage = V13Ui.button(activity, "添加第一个模板", CartoonStyle.SURFACE);
            manage.setOnClickListener(v -> TemplateManagerDialog.show(
                    activity, db, ledgerId, onChanged));
            card.addView(manage, buttonParams(activity));
            return card;
        }

        int count = Math.min(3, templates.size());
        for (int index = 0; index < count; index++) {
            TemplateRepository.Template template = templates.get(index);
            TextView shortcut = V13Ui.button(activity,
                    InputCatalog.iconFor(template.type, template.category)
                            + "  " + template.name,
                    CartoonStyle.SURFACE);
            shortcut.setOnClickListener(v -> TemplateManagerDialog.confirmUse(
                    activity, db, template, onChanged));
            card.addView(shortcut, buttonParams(activity));
            card.addView(V13Ui.gap(activity, 6));
        }
        TextView manage = V13Ui.button(activity, "管理模板 ›", CartoonStyle.SOFT_YELLOW);
        manage.setOnClickListener(v -> TemplateManagerDialog.show(
                activity, db, ledgerId, onChanged));
        card.addView(manage, buttonParams(activity));
        return card;
    }

    private static LinearLayout.LayoutParams buttonParams(Activity activity) {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 46));
    }
}
