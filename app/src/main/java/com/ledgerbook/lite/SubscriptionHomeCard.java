package com.ledgerbook.lite;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;

/** Shows active subscription cost and renewal reminders. */
public final class SubscriptionHomeCard {
    private SubscriptionHomeCard() {}

    public static View create(Activity activity, LedgerDb db,
                              long ledgerId, Runnable onChanged) {
        SubscriptionRepository repository = new SubscriptionRepository(db);
        SubscriptionRepository.Snapshot snapshot = repository.snapshot(
                ledgerId, LocalDate.now(), 7);
        LedgerDb.Ledger ledger = db.getLedger(ledgerId);
        String currency = ledger == null ? "CNY" : ledger.currency;
        LinearLayout card = V13Ui.card(activity,
                snapshot.dueCount > 0 ? CartoonStyle.SOFT_PEACH : CartoonStyle.SOFT_SKY);
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("订阅管理，" + snapshot.dueCount + " 项到期");

        LinearLayout heading = new LinearLayout(activity);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(V13Ui.text(activity, "🗓️ 订阅", 17,
                CartoonStyle.INK, true), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        heading.addView(V13Ui.text(activity, "管理 ›", 13,
                CartoonStyle.MUTED, true));
        card.addView(heading);

        String message;
        if (snapshot.values.isEmpty()) {
            message = "还没有订阅，可把最近一笔会员或软件支出设为订阅。";
        } else {
            message = "月均 " + V13Ui.money(currency, snapshot.monthlyCents)
                    + " · 年度预计 " + V13Ui.money(currency, snapshot.annualCents)
                    + "\n已到期 " + snapshot.dueCount
                    + " · 7天内续费 " + snapshot.upcomingCount;
        }
        TextView detail = V13Ui.text(activity, message, 13,
                CartoonStyle.MUTED, false);
        detail.setLineSpacing(0f, 1.16f);
        detail.setPadding(0, V13Ui.dp(activity, 6), 0, 0);
        card.addView(detail);
        card.setOnClickListener(v -> SubscriptionManagerDialog.show(
                activity, db, ledgerId, onChanged));
        return card;
    }
}
