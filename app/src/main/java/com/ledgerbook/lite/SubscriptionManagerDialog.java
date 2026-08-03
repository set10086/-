package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
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

/** Manages subscriptions. Charges are posted only after explicit confirmation. */
public final class SubscriptionManagerDialog {
    private SubscriptionManagerDialog() {}

    public static void show(Activity activity, LedgerDb db, long ledgerId, Runnable onChanged) {
        SubscriptionRepository repository = new SubscriptionRepository(db);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 18));
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("🗓️ 订阅管理")
                .setView(scroll)
                .setPositiveButton("完成", null)
                .create();
        Runnable[] render = new Runnable[1];
        render[0] = () -> render(activity, content, repository, db, ledgerId, onChanged, render[0]);
        render[0].run();
        dialog.show();
    }

    private static void render(Activity activity, LinearLayout content,
                               SubscriptionRepository repository, LedgerDb db,
                               long ledgerId, Runnable onChanged, Runnable rerender) {
        content.removeAllViews();
        LedgerDb.Ledger ledger = db.getLedger(ledgerId);
        String currency = ledger == null ? "CNY" : ledger.currency;
        SubscriptionRepository.Snapshot snapshot = repository.snapshot(
                ledgerId, LocalDate.now(), 7);

        LinearLayout summary = V13Ui.card(activity,
                snapshot.dueCount > 0 ? CartoonStyle.SOFT_PEACH : CartoonStyle.SOFT_GREEN);
        summary.addView(title(activity, "订阅成本概览"));
        summary.addView(detail(activity,
                "月均 " + V13Ui.money(currency, snapshot.monthlyCents)
                        + " · 年度预计 " + V13Ui.money(currency, snapshot.annualCents)
                        + "\n已到期 " + snapshot.dueCount
                        + " · 7天内续费 " + snapshot.upcomingCount));
        content.addView(summary);
        content.addView(V13Ui.gap(activity, 8));

        TextView add = V13Ui.button(activity,
                "＋ 将最近一笔设为订阅", CartoonStyle.SOFT_YELLOW);
        add.setOnClickListener(v -> createFromLatest(
                activity, repository, db, ledgerId, onChanged, rerender));
        content.addView(add, fullButton(activity));
        content.addView(V13Ui.gap(activity, 12));

        if (snapshot.values.isEmpty()) {
            LinearLayout empty = V13Ui.card(activity, CartoonStyle.SURFACE);
            empty.addView(title(activity, "还没有订阅"));
            empty.addView(detail(activity, "可把最近一笔会员、云服务或软件支出设为订阅。"));
            content.addView(empty);
            return;
        }

        for (SubscriptionRepository.Subscription value : snapshot.values) {
            SubscriptionRules.Reminder reminder = SubscriptionRules.reminder(
                    value.nextChargeDate, value.active, LocalDate.now(), 7);
            int fill = reminder == SubscriptionRules.Reminder.DUE
                    ? CartoonStyle.SOFT_PEACH
                    : reminder == SubscriptionRules.Reminder.UPCOMING
                    ? CartoonStyle.SOFT_YELLOW : CartoonStyle.SOFT_SKY;
            LinearLayout card = V13Ui.card(activity, fill);
            LinearLayout heading = new LinearLayout(activity);
            heading.setOrientation(LinearLayout.HORIZONTAL);
            heading.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout labels = new LinearLayout(activity);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.addView(title(activity,
                    InputCatalog.iconFor(LedgerDb.TYPE_EXPENSE, value.category)
                            + "  " + value.name));
            SubscriptionRules.Cost cost = SubscriptionRules.cost(
                    value.amountCents, value.frequency, value.interval);
            labels.addView(detail(activity,
                    "下次 " + value.nextChargeDate + " · "
                            + V13Ui.money(currency, value.amountCents)
                            + "\n月均 " + V13Ui.money(currency, cost.monthlyCents)
                            + " · 年度 " + V13Ui.money(currency, cost.annualCents)));
            heading.addView(labels, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            CheckBox active = new CheckBox(activity);
            active.setChecked(value.active);
            active.setContentDescription(value.name + (value.active ? "，已启用" : "，已暂停"));
            active.setOnCheckedChangeListener((button, checked) -> {
                try {
                    repository.setActive(value.id, checked);
                    changed(onChanged, rerender);
                } catch (RuntimeException error) {
                    toast(activity, error);
                }
            });
            heading.addView(active, new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 52)));
            card.addView(heading);

            LinearLayout actions = new LinearLayout(activity);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            TextView post = V13Ui.button(activity, "确认记账", CartoonStyle.SOFT_GREEN);
            post.setEnabled(value.active);
            post.setAlpha(value.active ? 1f : 0.4f);
            post.setOnClickListener(v -> confirmPosting(
                    activity, repository, db, value, onChanged, rerender));
            actions.addView(post, weighted(activity));
            actions.addView(horizontalGap(activity));
            TextView remove = V13Ui.button(activity, "删除", CartoonStyle.SOFT_PEACH);
            remove.setOnClickListener(v -> new AlertDialog.Builder(activity)
                    .setTitle("删除订阅？")
                    .setMessage(value.name + "\n不会删除已经生成的历史账单。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除", (ignored, which) -> {
                        try {
                            repository.delete(value.id);
                            changed(onChanged, rerender);
                        } catch (RuntimeException error) {
                            toast(activity, error);
                        }
                    }).show());
            actions.addView(remove, weighted(activity));
            card.addView(actions);
            content.addView(card);
            content.addView(V13Ui.gap(activity, 8));
        }
    }

    private static void createFromLatest(Activity activity,
                                         SubscriptionRepository repository,
                                         LedgerDb db, long ledgerId,
                                         Runnable onChanged, Runnable rerender) {
        List<LedgerDb.Txn> recent = db.getRecentTransactions(ledgerId, 50);
        LedgerDb.Txn source = null;
        for (LedgerDb.Txn value : recent) {
            if (LedgerDb.TYPE_EXPENSE.equals(value.type)) { source = value; break; }
        }
        if (source == null) {
            Toast.makeText(activity, "当前账本没有可用支出", Toast.LENGTH_SHORT).show();
            return;
        }
        LedgerDb.Txn selectedSource = source;
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(activity, 18), dp(activity, 4), dp(activity, 18), dp(activity, 4));
        EditText name = new EditText(activity);
        name.setSingleLine(true);
        name.setInputType(InputType.TYPE_CLASS_TEXT);
        name.setHint("订阅名称");
        name.setText(leaf(source.category));
        name.setSelection(name.getText().length());
        form.addView(label(activity, "名称"));
        form.addView(name);

        Spinner frequency = new Spinner(activity);
        frequency.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"每周", "每月", "每年"}));
        frequency.setSelection(1);
        form.addView(label(activity, "扣费周期"));
        form.addView(frequency);

        NumberPicker interval = new NumberPicker(activity);
        interval.setMinValue(1); interval.setMaxValue(99); interval.setValue(1);
        form.addView(label(activity, "每几个周期扣费一次"));
        form.addView(interval);

        DatePicker date = new DatePicker(activity);
        LocalDate defaultDate = LocalDate.now().plusMonths(1);
        date.init(defaultDate.getYear(), defaultDate.getMonthValue() - 1,
                defaultDate.getDayOfMonth(), null);
        form.addView(label(activity, "下次扣费日期"));
        form.addView(date);

        EditText note = new EditText(activity);
        note.setSingleLine(false);
        note.setHint("备注（可选）");
        note.setText(source.note);
        form.addView(label(activity, "备注"));
        form.addView(note);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("创建订阅")
                .setMessage("订阅到期后只提醒，确认后才会生成账单。")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("创建", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        LocalDate next = LocalDate.of(date.getYear(),
                                date.getMonth() + 1, date.getDayOfMonth());
                        repository.createFromTransaction(ledgerId,
                                name.getText().toString(), selectedSource,
                                frequencyAt(frequency.getSelectedItemPosition()),
                                interval.getValue(), next, note.getText().toString());
                        dialog.dismiss();
                        changed(onChanged, rerender);
                    } catch (RuntimeException error) {
                        toast(activity, error);
                    }
                }));
        dialog.show();
    }

    private static void confirmPosting(Activity activity,
                                       SubscriptionRepository repository,
                                       LedgerDb db,
                                       SubscriptionRepository.Subscription value,
                                       Runnable onChanged, Runnable rerender) {
        try {
            SubscriptionRules.Snapshot snapshot = SubscriptionRules.instantiate(
                    value, db.getAccounts(value.ledgerId), ZoneId.systemDefault());
            LedgerDb.Ledger ledger = db.getLedger(value.ledgerId);
            String currency = ledger == null ? "CNY" : ledger.currency;
            new AlertDialog.Builder(activity)
                    .setTitle("确认订阅扣费？")
                    .setMessage(value.name + "\n" + value.nextChargeDate + " · "
                            + V13Ui.money(currency, value.amountCents)
                            + "\n确认后才会改变账户余额。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确认记账", (ignored, which) -> {
                        try {
                            db.addTransaction(value.ledgerId, LedgerDb.TYPE_EXPENSE,
                                    snapshot.category, snapshot.amountCents,
                                    snapshot.accountId, null, "本人", "订阅",
                                    false, 0L, true, snapshot.note, snapshot.occurredAt);
                            repository.advanceAfterPosting(value.id);
                            Toast.makeText(activity, "订阅账单已保存",
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

    private static SubscriptionRules.Frequency frequencyAt(int index) {
        if (index == 0) return SubscriptionRules.Frequency.WEEKLY;
        if (index == 2) return SubscriptionRules.Frequency.YEARLY;
        return SubscriptionRules.Frequency.MONTHLY;
    }
    private static TextView title(Activity a, String s) { return V13Ui.text(a, s, 16, CartoonStyle.INK, true); }
    private static TextView detail(Activity a, String s) { TextView v=V13Ui.text(a,s,13,CartoonStyle.MUTED,false);v.setLineSpacing(0f,1.15f);v.setPadding(0,dp(a,3),0,dp(a,7));return v; }
    private static TextView label(Activity a, String s) { TextView v=V13Ui.text(a,s,13,CartoonStyle.MUTED,true);v.setPadding(0,dp(a,8),0,dp(a,2));return v; }
    private static LinearLayout.LayoutParams fullButton(Activity a) { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(a,46)); }
    private static LinearLayout.LayoutParams weighted(Activity a) { return new LinearLayout.LayoutParams(0,dp(a,44),1f); }
    private static TextView horizontalGap(Activity a) { TextView v=new TextView(a);v.setWidth(dp(a,7));return v; }
    private static String leaf(String c) { int i=c==null?-1:c.lastIndexOf('/');return i>=0&&i<c.length()-1?c.substring(i+1):c; }
    private static void changed(Runnable a, Runnable b) { if(a!=null)a.run();b.run(); }
    private static void toast(Activity a, RuntimeException e) { String m=e.getMessage();Toast.makeText(a,m==null||m.trim().isEmpty()?"订阅操作失败":m,Toast.LENGTH_SHORT).show(); }
    private static int dp(Activity a, int v) { return V13Ui.dp(a,v); }
}
