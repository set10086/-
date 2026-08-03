package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Manages recurrence rules and requires explicit confirmation before posting. */
public final class RecurringManagerDialog {
    private RecurringManagerDialog() {
    }

    public static void show(Activity activity, LedgerDb db,
                            long ledgerId, Runnable onChanged) {
        RecurringRepository repository = new RecurringRepository(db);
        repository.materializeDue(ledgerId, LocalDate.now());

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 12), dp(activity, 8),
                dp(activity, 12), dp(activity, 18));
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("🔁 周期记账")
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
                               RecurringRepository repository, LedgerDb db,
                               long ledgerId, Runnable onChanged, Runnable rerender) {
        repository.materializeDue(ledgerId, LocalDate.now());
        content.removeAllViews();

        TextView add = V13Ui.button(activity,
                "＋ 将最近一笔设为周期", CartoonStyle.SOFT_YELLOW);
        add.setOnClickListener(v -> createFromLatest(
                activity, repository, db, ledgerId, onChanged, rerender));
        content.addView(add, fullButton(activity));
        content.addView(V13Ui.gap(activity, 12));

        List<RecurringRepository.Pending> pending = repository.listPending(ledgerId);
        section(activity, content, "待确认（" + pending.size() + "）");
        if (pending.isEmpty()) {
            LinearLayout empty = V13Ui.card(activity, CartoonStyle.SOFT_GREEN);
            empty.addView(title(activity, "当前没有到期项目"));
            empty.addView(detail(activity,
                    "周期任务到期后会先出现在这里，不会自动改变账户余额。"));
            content.addView(empty);
        } else {
            for (RecurringRepository.Pending item : pending) {
                RecurringRepository.Rule rule = repository.getRule(ledgerId, item.ruleId);
                LinearLayout card = V13Ui.card(activity, CartoonStyle.SOFT_PEACH);
                card.addView(title(activity,
                        InputCatalog.iconFor(rule.type, rule.category)
                                + "  " + rule.name));
                LedgerDb.Ledger ledger = db.getLedger(ledgerId);
                String currency = ledger == null ? "CNY" : ledger.currency;
                card.addView(detail(activity,
                        item.dueDate + " · " + rule.category + " · "
                                + V13Ui.money(currency, rule.amountCents)));
                LinearLayout actions = horizontal(activity);
                TextView confirm = V13Ui.button(activity,
                        "确认记账", CartoonStyle.SOFT_GREEN);
                confirm.setOnClickListener(v -> confirmPosting(activity,
                        repository, db, ledgerId, item, rule, onChanged, rerender));
                actions.addView(confirm, weighted(activity));
                actions.addView(horizontalGap(activity));
                TextView skip = V13Ui.button(activity, "跳过", CartoonStyle.SURFACE);
                skip.setOnClickListener(v -> new AlertDialog.Builder(activity)
                        .setTitle("跳过本次周期账单？")
                        .setMessage(rule.name + " · " + item.dueDate)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("跳过", (ignored, which) -> {
                            try {
                                repository.skip(item.id);
                                changed(onChanged, rerender);
                            } catch (RuntimeException error) {
                                toast(activity, error);
                            }
                        }).show());
                actions.addView(skip, weighted(activity));
                card.addView(actions);
                content.addView(card);
                content.addView(V13Ui.gap(activity, 8));
            }
        }

        content.addView(V13Ui.gap(activity, 14));
        List<RecurringRepository.Rule> rules = repository.listRules(ledgerId);
        section(activity, content, "周期规则（" + rules.size() + "）");
        if (rules.isEmpty()) {
            LinearLayout empty = V13Ui.card(activity, CartoonStyle.SURFACE);
            empty.addView(title(activity, "还没有周期规则"));
            empty.addView(detail(activity,
                    "可把最近一笔房租、工资、保险或会员账单设为周期。"));
            content.addView(empty);
            return;
        }

        for (RecurringRepository.Rule rule : rules) {
            LinearLayout card = V13Ui.card(activity,
                    rule.enabled ? CartoonStyle.SOFT_SKY : CartoonStyle.SURFACE);
            LinearLayout heading = horizontal(activity);
            LinearLayout labels = new LinearLayout(activity);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.addView(title(activity, rule.name));
            labels.addView(detail(activity,
                    frequencyLabel(rule.frequency, rule.interval)
                            + " · 从 " + rule.startDate + " 开始"));
            heading.addView(labels, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            CheckBox enabled = new CheckBox(activity);
            enabled.setChecked(rule.enabled);
            enabled.setContentDescription(rule.name
                    + (rule.enabled ? "，已启用" : "，已暂停"));
            enabled.setOnCheckedChangeListener((button, checked) -> {
                try {
                    repository.setEnabled(rule.id, checked);
                    changed(onChanged, rerender);
                } catch (RuntimeException error) {
                    toast(activity, error);
                }
            });
            heading.addView(enabled, new LinearLayout.LayoutParams(
                    dp(activity, 52), dp(activity, 52)));
            card.addView(heading);

            TextView remove = V13Ui.button(activity,
                    "删除周期规则", CartoonStyle.SOFT_PEACH);
            remove.setOnClickListener(v -> new AlertDialog.Builder(activity)
                    .setTitle("删除周期规则？")
                    .setMessage("会同时删除该规则尚未处理的待确认项目，不会删除已生成账单。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除", (ignored, which) -> {
                        try {
                            repository.deleteRule(rule.id);
                            changed(onChanged, rerender);
                        } catch (RuntimeException error) {
                            toast(activity, error);
                        }
                    }).show());
            card.addView(remove, fullButton(activity));
            content.addView(card);
            content.addView(V13Ui.gap(activity, 8));
        }
    }

    private static void createFromLatest(Activity activity,
                                         RecurringRepository repository,
                                         LedgerDb db, long ledgerId,
                                         Runnable onChanged, Runnable rerender) {
        List<LedgerDb.Txn> recent = db.getRecentTransactions(ledgerId, 1);
        if (recent.isEmpty()) {
            Toast.makeText(activity, "当前账本还没有可用账单",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        LedgerDb.Txn transaction = recent.get(0);
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(activity, 18), dp(activity, 4),
                dp(activity, 18), dp(activity, 4));

        EditText name = new EditText(activity);
        name.setSingleLine(true);
        name.setInputType(InputType.TYPE_CLASS_TEXT);
        name.setHint("周期名称");
        name.setText(leaf(transaction.category));
        name.setSelection(name.getText().length());
        form.addView(label(activity, "名称"));
        form.addView(name);

        Spinner frequency = new Spinner(activity);
        String[] frequencyLabels = {"每天", "每周", "每月", "每年"};
        frequency.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, frequencyLabels));
        frequency.setSelection(2);
        form.addView(label(activity, "周期"));
        form.addView(frequency);

        NumberPicker interval = new NumberPicker(activity);
        interval.setMinValue(1);
        interval.setMaxValue(99);
        interval.setValue(1);
        form.addView(label(activity, "每几个周期执行一次"));
        form.addView(interval);

        TextView explanation = detail(activity,
                "开始日期：今天\n到期后生成待确认项目，确认后才会记账。") ;
        explanation.setPadding(0, dp(activity, 8), 0, 0);
        form.addView(explanation);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("创建周期记账")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("创建", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        RecurringRules.Frequency selected = frequencyAt(
                                frequency.getSelectedItemPosition());
                        repository.createFromTransaction(ledgerId,
                                name.getText().toString(), transaction,
                                selected, interval.getValue(), LocalDate.now(),
                                null, 0);
                        repository.materializeDue(ledgerId, LocalDate.now());
                        dialog.dismiss();
                        changed(onChanged, rerender);
                    } catch (RuntimeException error) {
                        toast(activity, error);
                    }
                }));
        dialog.show();
    }

    private static void confirmPosting(Activity activity,
                                       RecurringRepository repository,
                                       LedgerDb db, long ledgerId,
                                       RecurringRepository.Pending pending,
                                       RecurringRepository.Rule rule,
                                       Runnable onChanged, Runnable rerender) {
        try {
            RecurringRules.Snapshot snapshot = RecurringRules.instantiate(
                    rule, db.getAccounts(ledgerId), pending.dueDate,
                    ZoneId.systemDefault());
            LedgerDb.Ledger ledger = db.getLedger(ledgerId);
            String currency = ledger == null ? "CNY" : ledger.currency;
            new AlertDialog.Builder(activity)
                    .setTitle("确认生成周期账单？")
                    .setMessage(rule.name + "\n" + pending.dueDate + " · "
                            + rule.category + "\n"
                            + V13Ui.money(currency, rule.amountCents))
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确认记账", (ignored, which) -> {
                        try {
                            long transactionId = db.addTransaction(ledgerId,
                                    snapshot.type, snapshot.category,
                                    snapshot.amountCents, snapshot.accountId,
                                    snapshot.toAccountId, snapshot.bookkeeper,
                                    snapshot.tags, snapshot.reimbursable,
                                    snapshot.discountCents, snapshot.includeBudget,
                                    snapshot.note, snapshot.occurredAt);
                            repository.markPosted(pending.id, transactionId);
                            Toast.makeText(activity, "周期账单已保存",
                                    Toast.LENGTH_SHORT).show();
                            changed(onChanged, rerender);
                        } catch (RuntimeException error) {
                            toast(activity, error);
                        }
                    }).show();
        } catch (RuntimeException error) {
            toast(activity, error);
        }
    }

    private static RecurringRules.Frequency frequencyAt(int index) {
        if (index == 0) return RecurringRules.Frequency.DAILY;
        if (index == 1) return RecurringRules.Frequency.WEEKLY;
        if (index == 3) return RecurringRules.Frequency.YEARLY;
        return RecurringRules.Frequency.MONTHLY;
    }

    private static String frequencyLabel(RecurringRules.Frequency frequency, int interval) {
        String unit;
        switch (frequency) {
            case DAILY: unit = "天"; break;
            case WEEKLY: unit = "周"; break;
            case YEARLY: unit = "年"; break;
            default: unit = "月"; break;
        }
        return interval == 1 ? "每" + unit : "每 " + interval + " " + unit;
    }

    private static void section(Activity activity, LinearLayout content, String text) {
        TextView title = V13Ui.text(activity, text, 16, CartoonStyle.INK, true);
        title.setPadding(dp(activity, 2), 0, 0, dp(activity, 7));
        content.addView(title);
    }

    private static TextView title(Activity activity, String text) {
        return V13Ui.text(activity, text, 16, CartoonStyle.INK, true);
    }

    private static TextView detail(Activity activity, String text) {
        TextView view = V13Ui.text(activity, text, 13, CartoonStyle.MUTED, false);
        view.setLineSpacing(0f, 1.15f);
        view.setPadding(0, dp(activity, 3), 0, dp(activity, 7));
        return view;
    }

    private static TextView label(Activity activity, String text) {
        TextView view = V13Ui.text(activity, text, 13, CartoonStyle.MUTED, true);
        view.setPadding(0, dp(activity, 8), 0, dp(activity, 2));
        return view;
    }

    private static LinearLayout horizontal(Activity activity) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private static LinearLayout.LayoutParams fullButton(Activity activity) {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 46));
    }

    private static LinearLayout.LayoutParams weighted(Activity activity) {
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
        String value = error.getMessage();
        Toast.makeText(activity,
                value == null || value.trim().isEmpty() ? "周期操作失败" : value,
                Toast.LENGTH_SHORT).show();
    }

    private static int dp(Activity activity, int value) {
        return V13Ui.dp(activity, value);
    }
}
