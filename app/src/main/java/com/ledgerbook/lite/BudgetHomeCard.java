package com.ledgerbook.lite;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.YearMonth;

/** Creates the live budget summary shown on the home page. */
public final class BudgetHomeCard {
    private BudgetHomeCard() {
    }

    public static View create(Activity activity, LedgerDb db,
                              long ledgerId, Runnable onChanged) {
        BudgetRepository repository = new BudgetRepository(db);
        YearMonth month = YearMonth.now();
        LocalDate today = LocalDate.now();
        BudgetRepository.MonthSnapshot snapshot = repository.snapshot(
                ledgerId, month.toString(), today.getDayOfMonth(), month.lengthOfMonth());
        LedgerDb.Ledger ledger = db.getLedger(ledgerId);
        String currency = ledger == null ? "CNY" : ledger.currency;

        LinearLayout card = V13Ui.card(activity, tint(snapshot.total.status.level));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("本月预算，点击管理");

        LinearLayout heading = new LinearLayout(activity);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = V13Ui.text(activity, "🎯 本月预算", 17,
                CartoonStyle.INK, true);
        heading.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView action = V13Ui.text(activity,
                snapshot.total.budget.amountCents > 0L ? "管理 ›" : "去设置 ›",
                13, CartoonStyle.MUTED, true);
        heading.addView(action);
        card.addView(heading);

        BudgetRules.Status status = snapshot.total.status;
        String message;
        if (status.level == BudgetRules.Level.NONE) {
            message = "设置月度总预算后，可查看剩余额度和日均可用金额。";
        } else if (status.level == BudgetRules.Level.OVER) {
            message = "已用 " + V13Ui.money(currency, status.spentCents)
                    + " / " + V13Ui.money(currency, status.limitCents)
                    + " · 超出 " + V13Ui.money(currency, status.overCents);
        } else {
            message = "已用 " + V13Ui.money(currency, status.spentCents)
                    + " / " + V13Ui.money(currency, status.limitCents)
                    + " · 剩余 " + V13Ui.money(currency, status.remainingCents)
                    + "\n日均可用 " + V13Ui.money(currency, status.dailyAvailableCents)
                    + " · " + status.progressPercent + "%";
        }
        TextView detail = V13Ui.text(activity, message, 13,
                CartoonStyle.MUTED, false);
        detail.setLineSpacing(0f, 1.16f);
        detail.setPadding(0, V13Ui.dp(activity, 6), 0, 0);
        card.addView(detail);

        card.setOnClickListener(v -> BudgetManagerDialog.show(
                activity, db, ledgerId, onChanged));
        return card;
    }

    private static int tint(BudgetRules.Level level) {
        if (level == BudgetRules.Level.OVER) return CartoonStyle.SOFT_PEACH;
        if (level == BudgetRules.Level.WARNING) return CartoonStyle.SOFT_YELLOW;
        if (level == BudgetRules.Level.SAFE) return CartoonStyle.SOFT_GREEN;
        return CartoonStyle.SOFT_SKY;
    }
}
