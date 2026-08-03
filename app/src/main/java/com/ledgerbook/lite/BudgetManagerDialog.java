package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.YearMonth;

/** Lets users create, edit, and remove total/category budgets for the current month. */
public final class BudgetManagerDialog {
    private BudgetManagerDialog() {
    }

    public static void show(Activity activity, LedgerDb db, long ledgerId, Runnable onChanged) {
        BudgetRepository repository = new BudgetRepository(db);
        LedgerDb.Ledger ledger = db.getLedger(ledgerId);
        String currency = ledger == null ? "CNY" : ledger.currency;
        YearMonth month = YearMonth.now();

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 12), dp(activity, 8),
                dp(activity, 12), dp(activity, 18));
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("🎯 " + month.getYear() + "年" + month.getMonthValue() + "月预算")
                .setView(scroll)
                .setPositiveButton("完成", null)
                .create();

        Runnable[] render = new Runnable[1];
        render[0] = () -> render(activity, content, repository, ledgerId,
                month, currency, onChanged, render[0]);
        render[0].run();
        dialog.show();
    }

    private static void render(Activity activity, LinearLayout content,
                               BudgetRepository repository, long ledgerId,
                               YearMonth month, String currency,
                               Runnable onChanged, Runnable rerender) {
        content.removeAllViews();
        LocalDate today = LocalDate.now();
        int day = today.getYear() == month.getYear()
                && today.getMonthValue() == month.getMonthValue()
                ? today.getDayOfMonth() : 1;
        BudgetRepository.MonthSnapshot snapshot = repository.snapshot(
                ledgerId, month.toString(), day, month.lengthOfMonth());

        LinearLayout totalCard = V13Ui.card(activity, tint(snapshot.total.status.level));
        totalCard.addView(title(activity, "本月总预算"));
        totalCard.addView(detail(activity, summary(currency, snapshot.total.status)));
        TextView setTotal = V13Ui.button(activity,
                snapshot.total.budget.amountCents > 0L ? "修改总预算" : "设置总预算",
                CartoonStyle.SURFACE);
        setTotal.setOnClickListener(v -> AmountCalculatorDialog.show(activity,
                "设置本月总预算", snapshot.total.budget.amountCents, cents -> {
                    try {
                        repository.setTotalBudget(ledgerId, month.toString(), cents);
                        changed(onChanged, rerender);
                    } catch (RuntimeException error) {
                        toast(activity, error);
                    }
                }));
        totalCard.addView(setTotal, buttonParams(activity));
        if (snapshot.total.budget.amountCents > 0L) {
            totalCard.addView(V13Ui.gap(activity, 6));
            TextView remove = V13Ui.button(activity, "清除总预算", CartoonStyle.SOFT_PEACH);
            remove.setOnClickListener(v -> new AlertDialog.Builder(activity)
                    .setTitle("清除本月总预算？")
                    .setMessage("分类预算不会被删除。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("清除", (ignored, which) -> {
                        repository.removeTotalBudget(ledgerId, month.toString());
                        changed(onChanged, rerender);
                    }).show());
            totalCard.addView(remove, buttonParams(activity));
        }
        content.addView(totalCard);
        content.addView(V13Ui.gap(activity, 10));

        TextView addCategory = V13Ui.button(activity,
                "＋ 新增分类预算", CartoonStyle.SOFT_YELLOW);
        addCategory.setOnClickListener(v -> CategoryPickerDialog.show(
                activity, LedgerDb.TYPE_EXPENSE, (icon, label) -> {
                    BudgetRepository.Budget existing = find(snapshot, label);
                    long initial = existing == null ? 0L : existing.amountCents;
                    AmountCalculatorDialog.show(activity,
                            "设置“" + leaf(label) + "”预算", initial, cents -> {
                                try {
                                    repository.setCategoryBudget(
                                            ledgerId, month.toString(), label, cents);
                                    changed(onChanged, rerender);
                                } catch (RuntimeException error) {
                                    toast(activity, error);
                                }
                            });
                }));
        content.addView(addCategory, buttonParams(activity));
        content.addView(V13Ui.gap(activity, 12));

        if (snapshot.categories.isEmpty()) {
            LinearLayout empty = V13Ui.card(activity, CartoonStyle.SURFACE);
            empty.addView(title(activity, "还没有分类预算"));
            empty.addView(detail(activity, "可为餐饮、交通等分类单独设置额度。"));
            content.addView(empty);
            return;
        }

        for (BudgetRepository.Progress progress : snapshot.categories) {
            BudgetRepository.Budget budget = progress.budget;
            LinearLayout card = V13Ui.card(activity, tint(progress.status.level));
            TextView categoryTitle = title(activity,
                    InputCatalog.iconFor(LedgerDb.TYPE_EXPENSE, budget.categoryKey)
                            + "  " + budget.categoryKey);
            card.addView(categoryTitle);
            card.addView(detail(activity, summary(currency, progress.status)));

            LinearLayout actions = new LinearLayout(activity);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            TextView edit = V13Ui.button(activity, "修改", CartoonStyle.SURFACE);
            edit.setOnClickListener(v -> AmountCalculatorDialog.show(activity,
                    "修改“" + leaf(budget.categoryKey) + "”预算",
                    budget.amountCents, cents -> {
                        try {
                            repository.setCategoryBudget(ledgerId, month.toString(),
                                    budget.categoryKey, cents);
                            changed(onChanged, rerender);
                        } catch (RuntimeException error) {
                            toast(activity, error);
                        }
                    }));
            actions.addView(edit, weightedButton(activity));
            actions.addView(horizontalGap(activity));
            TextView remove = V13Ui.button(activity, "删除", CartoonStyle.SOFT_PEACH);
            remove.setOnClickListener(v -> new AlertDialog.Builder(activity)
                    .setTitle("删除分类预算？")
                    .setMessage(budget.categoryKey + " · "
                            + V13Ui.money(currency, budget.amountCents))
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除", (ignored, which) -> {
                        repository.removeCategoryBudget(
                                ledgerId, month.toString(), budget.categoryKey);
                        changed(onChanged, rerender);
                    }).show());
            actions.addView(remove, weightedButton(activity));
            card.addView(actions);
            content.addView(card);
            content.addView(V13Ui.gap(activity, 8));
        }
    }

    private static BudgetRepository.Budget find(BudgetRepository.MonthSnapshot snapshot,
                                                String categoryKey) {
        for (BudgetRepository.Progress progress : snapshot.categories) {
            if (progress.budget.categoryKey.equals(categoryKey)) return progress.budget;
        }
        return null;
    }

    private static String summary(String currency, BudgetRules.Status status) {
        if (status.level == BudgetRules.Level.NONE) return "尚未设置预算";
        String second = status.overCents > 0L
                ? "已超出 " + V13Ui.money(currency, status.overCents)
                : "剩余 " + V13Ui.money(currency, status.remainingCents);
        return "额度 " + V13Ui.money(currency, status.limitCents)
                + " · 已用 " + V13Ui.money(currency, status.spentCents)
                + "（" + status.progressPercent + "%）\n"
                + second + " · 日均可用 "
                + V13Ui.money(currency, status.dailyAvailableCents);
    }

    private static int tint(BudgetRules.Level level) {
        if (level == BudgetRules.Level.OVER) return CartoonStyle.SOFT_PEACH;
        if (level == BudgetRules.Level.WARNING) return CartoonStyle.SOFT_YELLOW;
        if (level == BudgetRules.Level.SAFE) return CartoonStyle.SOFT_GREEN;
        return CartoonStyle.SURFACE;
    }

    private static TextView title(Activity activity, String value) {
        TextView view = V13Ui.text(activity, value, 16, CartoonStyle.INK, true);
        view.setPadding(0, 0, 0, dp(activity, 4));
        return view;
    }

    private static TextView detail(Activity activity, String value) {
        TextView view = V13Ui.text(activity, value, 13, CartoonStyle.MUTED, false);
        view.setLineSpacing(0f, 1.16f);
        view.setPadding(0, 0, 0, dp(activity, 8));
        return view;
    }

    private static LinearLayout.LayoutParams buttonParams(Activity activity) {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 46));
    }

    private static LinearLayout.LayoutParams weightedButton(Activity activity) {
        return new LinearLayout.LayoutParams(0, dp(activity, 44), 1f);
    }

    private static TextView horizontalGap(Activity activity) {
        TextView gap = new TextView(activity);
        gap.setWidth(dp(activity, 7));
        return gap;
    }

    private static String leaf(String category) {
        int slash = category == null ? -1 : category.lastIndexOf('/');
        return slash >= 0 && slash < category.length() - 1
                ? category.substring(slash + 1) : category;
    }

    private static void changed(Runnable onChanged, Runnable rerender) {
        if (onChanged != null) onChanged.run();
        rerender.run();
    }

    private static void toast(Activity activity, RuntimeException error) {
        String message = error.getMessage();
        Toast.makeText(activity,
                message == null || message.trim().isEmpty() ? "预算操作失败" : message,
                Toast.LENGTH_SHORT).show();
    }

    private static int dp(Activity activity, int value) {
        return V13Ui.dp(activity, value);
    }
}
