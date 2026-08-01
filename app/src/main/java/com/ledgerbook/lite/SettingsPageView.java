package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class SettingsPageView extends ScrollView {
    public interface Listener {
        void onLedgerSelected(long ledgerId);
        void onOpenLedgerManagement();
        void onOpenAccounts();
        void onSettingsChanged();
    }

    private final Activity activity;
    private final LedgerDb db;
    private final AppSettings settings;
    private final Listener listener;
    private final long currentLedgerId;
    private final LinearLayout content;

    public SettingsPageView(Activity activity, LedgerDb db, AppSettings settings,
                            long currentLedgerId, Listener listener) {
        super(activity);
        this.activity = activity;
        this.db = db;
        this.settings = settings;
        this.currentLedgerId = currentLedgerId;
        this.listener = listener;
        setFillViewport(true);
        setBackgroundColor(CartoonStyle.BACKGROUND);

        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(V13Ui.dp(activity, 14), V13Ui.dp(activity, 10),
                V13Ui.dp(activity, 14), V13Ui.dp(activity, 100));
        addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        render();
    }

    private void render() {
        content.removeAllViews();
        TextView heading = V13Ui.text(activity, "⚙️  设置小屋", 24, CartoonStyle.INK, true);
        content.addView(heading);
        TextView subheading = V13Ui.text(activity,
                "把常用偏好集中收好，记账时少点几步", 13, CartoonStyle.MUTED, false);
        content.addView(subheading);
        content.addView(V13Ui.gap(activity, 14));

        addThemeCard();
        content.addView(V13Ui.gap(activity, 10));
        addDefaultLedgerCard();
        content.addView(V13Ui.gap(activity, 10));
        addDefaultAccountCard();
        content.addView(V13Ui.gap(activity, 10));
        addBookkeeperCard();
        content.addView(V13Ui.gap(activity, 10));
        addBehaviorCard();
        content.addView(V13Ui.gap(activity, 10));
        addManagementCard();
        content.addView(V13Ui.gap(activity, 10));
        addAboutCard();
    }

    private void addThemeCard() {
        LinearLayout card = V13Ui.card(activity, CartoonStyle.SOFT_YELLOW);
        card.addView(V13Ui.text(activity, "🖍️  原创蜡笔家庭手账主题", 17,
                CartoonStyle.INK, true));
        TextView description = V13Ui.text(activity,
                "奶油纸张、贴纸卡片和生活化图标。采用原创设计语言，不包含动漫官方角色或素材。",
                13, CartoonStyle.MUTED, false);
        description.setLineSpacing(0f, 1.15f);
        description.setPadding(0, V13Ui.dp(activity, 5), 0, 0);
        card.addView(description);
        content.addView(card);
    }

    private void addDefaultLedgerCard() {
        LinearLayout card = V13Ui.card(activity, CartoonStyle.SOFT_PEACH);
        card.addView(V13Ui.text(activity, "📒  默认账本", 17, CartoonStyle.INK, true));
        card.addView(description("每次打开软件时优先进入这个账本。"));
        List<LedgerDb.Ledger> ledgers = db.getLedgers();
        Spinner spinner = new Spinner(activity);
        spinner.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, ledgers));
        long saved = settings.defaultLedgerId();
        long desired = containsLedger(ledgers, saved) ? saved : currentLedgerId;
        int position = ledgerPosition(ledgers, desired);
        spinner.setSelection(Math.max(0, position));
        boolean[] ready = {false};
        spinner.post(() -> ready[0] = true);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int selected, long id) {
                if (!ready[0] || selected < 0 || selected >= ledgers.size()) return;
                LedgerDb.Ledger ledger = ledgers.get(selected);
                settings.setDefaultLedgerId(ledger.id);
                if (listener != null) listener.onLedgerSelected(ledger.id);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        card.addView(spinner);
        content.addView(card);
    }

    private void addDefaultAccountCard() {
        LinearLayout card = V13Ui.card(activity, CartoonStyle.SOFT_SKY);
        card.addView(V13Ui.text(activity, "👛  默认记账账户", 17, CartoonStyle.INK, true));
        card.addView(description("打开记账弹窗时优先选择该账户。"));
        List<LedgerDb.Account> accounts = db.getAccounts(currentLedgerId);
        if (accounts.isEmpty()) {
            card.addView(description("当前账本还没有账户。"));
        } else {
            List<String> labels = new ArrayList<>();
            for (LedgerDb.Account account : accounts) {
                labels.add(CartoonStyle.accountIcon(account.type) + "  " + account.name
                        + " · " + account.type);
            }
            Spinner spinner = new Spinner(activity);
            spinner.setAdapter(new ArrayAdapter<>(activity,
                    android.R.layout.simple_spinner_dropdown_item, labels));
            int position = accountPosition(accounts, settings.defaultAccountId(currentLedgerId));
            spinner.setSelection(Math.max(0, position));
            boolean[] ready = {false};
            spinner.post(() -> ready[0] = true);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view,
                                                     int selected, long id) {
                    if (!ready[0] || selected < 0 || selected >= accounts.size()) return;
                    settings.setDefaultAccountId(currentLedgerId, accounts.get(selected).id);
                    if (listener != null) listener.onSettingsChanged();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) { }
            });
            card.addView(spinner);
        }
        content.addView(card);
    }

    private void addBookkeeperCard() {
        LinearLayout card = V13Ui.card(activity, CartoonStyle.SOFT_GREEN);
        card.addView(V13Ui.text(activity, "🙂  默认记账人", 17, CartoonStyle.INK, true));
        card.addView(description("新建账单时自动带入，也可以在记账时临时更换。"));
        List<String> keepers = InputCatalog.mergeBookkeepers(
                new java.util.LinkedHashSet<>(db.getBookkeepers(currentLedgerId)));
        Spinner spinner = new Spinner(activity);
        spinner.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, keepers));
        int position = keepers.indexOf(settings.defaultBookkeeper());
        spinner.setSelection(Math.max(0, position));
        boolean[] ready = {false};
        spinner.post(() -> ready[0] = true);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int selected, long id) {
                if (!ready[0] || selected < 0 || selected >= keepers.size()) return;
                settings.setDefaultBookkeeper(keepers.get(selected));
                if (listener != null) listener.onSettingsChanged();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        card.addView(spinner);
        content.addView(card);
    }

    private void addBehaviorCard() {
        LinearLayout card = V13Ui.card(activity, CartoonStyle.SOFT_LAVENDER);
        card.addView(V13Ui.text(activity, "✨  使用习惯", 17, CartoonStyle.INK, true));
        card.addView(description("调整启动位置和页面反馈。"));

        Spinner startup = new Spinner(activity);
        String[] pages = {"首页", "日历", "账户", "统计", "设置"};
        startup.setAdapter(new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_dropdown_item, pages));
        startup.setSelection(settings.startupPage());
        boolean[] ready = {false};
        startup.post(() -> ready[0] = true);
        startup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int selected, long id) {
                if (!ready[0]) return;
                settings.setStartupPage(selected);
                if (listener != null) listener.onSettingsChanged();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        TextView startupLabel = V13Ui.text(activity, "启动后显示", 13,
                CartoonStyle.MUTED, true);
        startupLabel.setPadding(0, V13Ui.dp(activity, 8), 0, 0);
        card.addView(startupLabel);
        card.addView(startup);

        CheckBox animations = new CheckBox(activity);
        animations.setText("启用侧栏动画");
        animations.setTextColor(CartoonStyle.INK);
        animations.setChecked(settings.animationsEnabled());
        animations.setOnCheckedChangeListener((button, checked) -> {
            settings.setAnimationsEnabled(checked);
            if (listener != null) listener.onSettingsChanged();
        });
        card.addView(animations);

        CheckBox returnHome = new CheckBox(activity);
        returnHome.setText("保存账单后返回首页");
        returnHome.setTextColor(CartoonStyle.INK);
        returnHome.setChecked(settings.returnHomeAfterSave());
        returnHome.setOnCheckedChangeListener((button, checked) -> {
            settings.setReturnHomeAfterSave(checked);
            if (listener != null) listener.onSettingsChanged();
        });
        card.addView(returnHome);
        content.addView(card);
    }

    private void addManagementCard() {
        LinearLayout card = V13Ui.card(activity, CartoonStyle.SURFACE);
        card.addView(V13Ui.text(activity, "🧰  管理入口", 17, CartoonStyle.INK, true));
        card.addView(description("账本、账户和分类资源统一从这里进入。"));
        TextView ledgers = V13Ui.button(activity, "📒  账本管理", CartoonStyle.SOFT_YELLOW);
        ledgers.setOnClickListener(v -> {
            if (listener != null) listener.onOpenLedgerManagement();
        });
        card.addView(ledgers, buttonParams());
        card.addView(V13Ui.gap(activity, 7));
        TextView accounts = V13Ui.button(activity, "👛  账户管理", CartoonStyle.SOFT_SKY);
        accounts.setOnClickListener(v -> {
            if (listener != null) listener.onOpenAccounts();
        });
        card.addView(accounts, buttonParams());
        card.addView(V13Ui.gap(activity, 7));
        TextView categories = V13Ui.button(activity, "🗂️  查看分类图标库", CartoonStyle.SOFT_PEACH);
        categories.setOnClickListener(v -> showCategorySummary());
        card.addView(categories, buttonParams());
        content.addView(card);
    }

    private void addAboutCard() {
        LinearLayout card = V13Ui.card(activity, CartoonStyle.SURFACE);
        card.addView(V13Ui.text(activity, "ℹ️  关于", 17, CartoonStyle.INK, true));
        card.addView(description("LedgerBook Lite 1.4.0\n"
                + "数据仅保存在当前手机的本地 SQLite 数据库中。\n"
                + "V1.4 不包含云同步、指纹锁或 Excel 导出。"));
        content.addView(card);
    }

    private void showCategorySummary() {
        List<InputCatalog.Group> expense = InputCatalog.groups(LedgerDb.TYPE_EXPENSE);
        List<InputCatalog.Option> expenseOptions = InputCatalog.categories(LedgerDb.TYPE_EXPENSE);
        List<InputCatalog.Group> income = InputCatalog.groups(LedgerDb.TYPE_INCOME);
        List<InputCatalog.Option> incomeOptions = InputCatalog.categories(LedgerDb.TYPE_INCOME);
        new AlertDialog.Builder(activity)
                .setTitle("🗂️ 分类图标库")
                .setMessage("支出分类：" + expense.size() + " 个分组，"
                        + (expenseOptions.size() - 1) + " 个具体图标\n"
                        + "收入分类：" + income.size() + " 个分组，"
                        + (incomeOptions.size() - 1) + " 个具体图标\n\n"
                        + "记账时点击分类即可搜索并选择。")
                .setPositiveButton("知道了", null)
                .show();
    }

    private TextView description(String value) {
        TextView view = V13Ui.text(activity, value, 13, CartoonStyle.MUTED, false);
        view.setLineSpacing(0f, 1.15f);
        view.setPadding(0, V13Ui.dp(activity, 4), 0, V13Ui.dp(activity, 4));
        return view;
    }

    private LinearLayout.LayoutParams buttonParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 48));
    }

    private static boolean containsLedger(List<LedgerDb.Ledger> values, long id) {
        for (LedgerDb.Ledger ledger : values) if (ledger.id == id) return true;
        return false;
    }

    private static int ledgerPosition(List<LedgerDb.Ledger> values, long id) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).id == id) return index;
        }
        return 0;
    }

    private static int accountPosition(List<LedgerDb.Account> values, long id) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).id == id) return index;
        }
        return 0;
    }
}
