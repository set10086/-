package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Manages templates and posts a confirmed template as a new transaction. */
public final class TemplateManagerDialog {
    private TemplateManagerDialog() {
    }

    public static void show(Activity activity, LedgerDb db,
                            long ledgerId, Runnable onChanged) {
        TemplateRepository repository = new TemplateRepository(db);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 12), dp(activity, 8),
                dp(activity, 12), dp(activity, 18));
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("📌 记账模板")
                .setView(scroll)
                .setPositiveButton("完成", null)
                .create();
        Runnable[] render = new Runnable[1];
        render[0] = () -> render(activity, content, repository,
                db, ledgerId, onChanged, render[0]);
        render[0].run();
        dialog.show();
    }

    private static void render(Activity activity, LinearLayout content,
                               TemplateRepository repository, LedgerDb db,
                               long ledgerId, Runnable onChanged, Runnable rerender) {
        content.removeAllViews();
        TextView add = V13Ui.button(activity,
                "＋ 将最近一笔保存为模板", CartoonStyle.SOFT_YELLOW);
        add.setOnClickListener(v -> saveLatest(activity, repository,
                db, ledgerId, onChanged, rerender));
        content.addView(add, buttonParams(activity));
        content.addView(V13Ui.gap(activity, 10));

        List<TemplateRepository.Template> templates = repository.list(ledgerId);
        if (templates.isEmpty()) {
            LinearLayout empty = V13Ui.card(activity, CartoonStyle.SURFACE);
            empty.addView(V13Ui.text(activity, "暂无模板", 16,
                    CartoonStyle.INK, true));
            TextView detail = V13Ui.text(activity,
                    "先完成一笔常用账单，再把它保存为模板。",
                    13, CartoonStyle.MUTED, false);
            detail.setPadding(0, dp(activity, 4), 0, 0);
            empty.addView(detail);
            content.addView(empty);
            return;
        }

        LedgerDb.Ledger ledger = db.getLedger(ledgerId);
        String currency = ledger == null ? "CNY" : ledger.currency;
        for (TemplateRepository.Template template : templates) {
            LinearLayout card = V13Ui.card(activity, CartoonStyle.SOFT_LAVENDER);
            card.addView(V13Ui.text(activity,
                    InputCatalog.iconFor(template.type, template.category)
                            + "  " + template.name,
                    16, CartoonStyle.INK, true));
            TextView detail = V13Ui.text(activity,
                    template.category + " · "
                            + V13Ui.money(currency, template.amountCents)
                            + "\n" + accountName(db, ledgerId, template.accountId),
                    13, CartoonStyle.MUTED, false);
            detail.setLineSpacing(0f, 1.15f);
            detail.setPadding(0, dp(activity, 4), 0, dp(activity, 8));
            card.addView(detail);

            LinearLayout actions = new LinearLayout(activity);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            TextView use = V13Ui.button(activity, "使用模板", CartoonStyle.SOFT_GREEN);
            use.setOnClickListener(v -> confirmUse(
                    activity, db, template, onChanged));
            actions.addView(use, weighted(activity));
            actions.addView(gap(activity));
            TextView remove = V13Ui.button(activity, "删除", CartoonStyle.SOFT_PEACH);
            remove.setOnClickListener(v -> new AlertDialog.Builder(activity)
                    .setTitle("删除模板？")
                    .setMessage(template.name)
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除", (ignored, which) -> {
                        repository.delete(template.id);
                        if (onChanged != null) onChanged.run();
                        rerender.run();
                    }).show());
            actions.addView(remove, weighted(activity));
            card.addView(actions);
            content.addView(card);
            content.addView(V13Ui.gap(activity, 8));
        }
    }

    private static void saveLatest(Activity activity, TemplateRepository repository,
                                   LedgerDb db, long ledgerId,
                                   Runnable onChanged, Runnable rerender) {
        List<LedgerDb.Txn> recent = db.getRecentTransactions(ledgerId, 1);
        if (recent.isEmpty()) {
            Toast.makeText(activity, "当前账本还没有可保存的账单",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        LedgerDb.Txn transaction = recent.get(0);
        EditText name = new EditText(activity);
        name.setSingleLine(true);
        name.setInputType(InputType.TYPE_CLASS_TEXT);
        name.setHint("例如：工作日午餐");
        name.setText(leaf(transaction.category));
        name.setSelection(name.getText().length());
        name.setPadding(dp(activity, 18), dp(activity, 8),
                dp(activity, 18), dp(activity, 8));
        AlertDialog input = new AlertDialog.Builder(activity)
                .setTitle("保存记账模板")
                .setMessage("模板会保存最近一笔的金额、分类、账户和更多设置。")
                .setView(name)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        input.setOnShowListener(ignored -> input.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        repository.saveFromTransaction(
                                ledgerId, name.getText().toString(), transaction);
                        input.dismiss();
                        if (onChanged != null) onChanged.run();
                        rerender.run();
                    } catch (RuntimeException error) {
                        Toast.makeText(activity, message(error),
                                Toast.LENGTH_SHORT).show();
                    }
                }));
        input.show();
    }

    public static void confirmUse(Activity activity, LedgerDb db,
                                  TemplateRepository.Template template,
                                  Runnable onChanged) {
        try {
            List<LedgerDb.Account> accounts = db.getAccounts(template.ledgerId);
            TemplateRules.Snapshot snapshot = TemplateRules.instantiate(
                    template, accounts, System.currentTimeMillis());
            LedgerDb.Ledger ledger = db.getLedger(template.ledgerId);
            String currency = ledger == null ? "CNY" : ledger.currency;
            String account = accountName(db, template.ledgerId, snapshot.accountId);
            new AlertDialog.Builder(activity)
                    .setTitle("使用模板“" + template.name + "”？")
                    .setMessage(snapshot.category + "\n"
                            + V13Ui.money(currency, snapshot.amountCents)
                            + " · " + account + "\n发生时间将设为现在。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("记一笔", (ignored, which) -> {
                        try {
                            db.addTransaction(template.ledgerId, snapshot.type,
                                    snapshot.category, snapshot.amountCents,
                                    snapshot.accountId, snapshot.toAccountId,
                                    snapshot.bookkeeper, snapshot.tags,
                                    snapshot.reimbursable, snapshot.discountCents,
                                    snapshot.includeBudget, snapshot.note,
                                    snapshot.occurredAt);
                            Toast.makeText(activity, "模板账单已保存",
                                    Toast.LENGTH_SHORT).show();
                            if (onChanged != null) onChanged.run();
                        } catch (RuntimeException error) {
                            Toast.makeText(activity, message(error),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }).show();
        } catch (RuntimeException error) {
            Toast.makeText(activity, message(error), Toast.LENGTH_SHORT).show();
        }
    }

    private static String accountName(LedgerDb db, long ledgerId, long accountId) {
        for (LedgerDb.Account account : db.getAccounts(ledgerId)) {
            if (account.id == accountId) return account.name;
        }
        return "账户已变更，使用时自动回退";
    }

    private static String leaf(String category) {
        int slash = category == null ? -1 : category.lastIndexOf('/');
        return slash >= 0 && slash < category.length() - 1
                ? category.substring(slash + 1) : category;
    }

    private static String message(RuntimeException error) {
        String value = error.getMessage();
        return value == null || value.trim().isEmpty() ? "模板操作失败" : value;
    }

    private static LinearLayout.LayoutParams buttonParams(Activity activity) {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 48));
    }

    private static LinearLayout.LayoutParams weighted(Activity activity) {
        return new LinearLayout.LayoutParams(0, dp(activity, 44), 1f);
    }

    private static TextView gap(Activity activity) {
        TextView gap = new TextView(activity);
        gap.setWidth(dp(activity, 7));
        return gap;
    }

    private static int dp(Activity activity, int value) {
        return V13Ui.dp(activity, value);
    }
}
