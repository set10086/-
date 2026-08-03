package com.ledgerbook.lite;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.List;

/** Shows due recurring items without posting them automatically. */
public final class RecurringHomeCard {
    private RecurringHomeCard() {
    }

    public static View create(Activity activity, LedgerDb db,
                              long ledgerId, Runnable onChanged) {
        RecurringRepository repository = new RecurringRepository(db);
        repository.materializeDue(ledgerId, LocalDate.now());
        List<RecurringRepository.Pending> pending = repository.listPending(ledgerId);
        List<RecurringRepository.Rule> rules = repository.listRules(ledgerId);

        LinearLayout card = V13Ui.card(activity,
                pending.isEmpty() ? CartoonStyle.SOFT_SKY : CartoonStyle.SOFT_PEACH);
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("周期记账，" + pending.size() + " 项待确认");

        LinearLayout heading = new LinearLayout(activity);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = V13Ui.text(activity, "🔁 周期记账", 17,
                CartoonStyle.INK, true);
        heading.addView(title, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView action = V13Ui.text(activity, "管理 ›", 13,
                CartoonStyle.MUTED, true);
        heading.addView(action);
        card.addView(heading);

        String message;
        if (!pending.isEmpty()) {
            RecurringRepository.Pending first = pending.get(0);
            RecurringRepository.Rule rule = repository.getRule(ledgerId, first.ruleId);
            message = pending.size() + " 项待确认 · 最近 " + first.dueDate
                    + "\n" + rule.name + " · 点击确认或跳过";
        } else if (rules.isEmpty()) {
            message = "还没有周期规则，可把最近一笔设为房租、工资或订阅周期。";
        } else {
            int active = 0;
            for (RecurringRepository.Rule rule : rules) if (rule.enabled) active++;
            message = active + " 个规则运行中 · 当前没有到期项目";
        }
        TextView detail = V13Ui.text(activity, message, 13,
                CartoonStyle.MUTED, false);
        detail.setLineSpacing(0f, 1.16f);
        detail.setPadding(0, V13Ui.dp(activity, 6), 0, 0);
        card.addView(detail);

        card.setOnClickListener(v -> RecurringManagerDialog.show(
                activity, db, ledgerId, onChanged));
        return card;
    }
}
