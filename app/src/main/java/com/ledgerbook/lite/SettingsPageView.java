package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.IntConsumer;

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
        setClickable(true);
        setFocusable(true);

        content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(V13Ui.dp(activity, 14), V13Ui.dp(activity, 10),
                V13Ui.dp(activity, 14), V13Ui.dp(activity, 110));
        addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        render();
    }

    private void render() {
        content.removeAllViews();
        content.addView(V13Ui.text(activity, "⚙️  设置小屋", 24, CartoonStyle.INK, true));
        content.addView(V13Ui.text(activity,
                "整行均可点击，当前值会显示在设置项下方", 13, CartoonStyle.MUTED, false));
        content.addView(V13Ui.gap(activity, 14));

        addAppearanceCard();
        gap();
        addDefaultsCard();
        gap();
        addBehaviorCard();
        gap();
        addManagementCard();
        gap();
        addAboutCard();
    }

    private void addAppearanceCard() {
        LinearLayout card = card("🖍️  外观与图标", CartoonStyle.SOFT_YELLOW,
                "采用原创 Canvas 蜡笔图标，分类含义和分组参考你提供的软件截图。");
        TextView theme = choiceRow("主题风格", "蜡笔家庭手账");
        theme.setOnClickListener(v -> new AlertDialog.Builder(activity)
                .setTitle("蜡笔家庭手账")
                .setMessage("奶油纸张、粗线条、贴纸卡片和语义化手绘图标。图标由应用实时绘制，不依赖 Emoji 字体。")
                .setPositiveButton("知道了", null)
                .show());
        card.addView(theme, rowParams());

        TextView library = choiceRow("分类图标库", "点击浏览支出与收入图标");
        library.setOnClickListener(v -> showIconLibraryChoice());
        card.addView(library, rowParamsWithTop());
        content.addView(card);
    }

    private void addDefaultsCard() {
        LinearLayout card = card("📌  记账默认值", CartoonStyle.SOFT_PEACH,
                "这些选项会直接影响下次打开软件和新建账单。");

        List<LedgerDb.Ledger> ledgers = db.getLedgers();
        List<String> ledgerLabels = new ArrayList<>();
        for (LedgerDb.Ledger ledger : ledgers) ledgerLabels.add(ledger.name);
        long configuredLedger = settings.defaultLedgerId();
        int ledgerIndex = ledgerPosition(ledgers,
                containsLedger(ledgers, configuredLedger) ? configuredLedger : currentLedgerId);
        TextView ledgerRow = choiceRow("默认账本",
                ledgerLabels.isEmpty() ? "暂无账本" : ledgerLabels.get(Math.max(0, ledgerIndex)));
        ledgerRow.setOnClickListener(v -> showSingleChoice("选择默认账本", ledgerLabels,
                ledgerIndex, selected -> {
                    if (selected < 0 || selected >= ledgers.size()) return;
                    LedgerDb.Ledger ledger = ledgers.get(selected);
                    settings.setDefaultLedgerId(ledger.id);
                    setChoiceValue(ledgerRow, "默认账本", ledger.name);
                    if (listener != null) listener.onLedgerSelected(ledger.id);
                }));
        card.addView(ledgerRow, rowParams());

        List<LedgerDb.Account> accounts = db.getAccounts(currentLedgerId);
        List<String> accountLabels = new ArrayList<>();
        for (LedgerDb.Account account : accounts) {
            accountLabels.add(account.name + " · " + account.type);
        }
        int accountIndex = accountPosition(accounts, settings.defaultAccountId(currentLedgerId));
        String accountValue = accounts.isEmpty() ? "当前账本暂无账户"
                : accountLabels.get(Math.max(0, accountIndex));
        TextView accountRow = choiceRow("默认记账账户", accountValue);
        accountRow.setEnabled(!accounts.isEmpty());
        accountRow.setAlpha(accounts.isEmpty() ? 0.55f : 1f);
        accountRow.setOnClickListener(v -> showSingleChoice("选择默认账户", accountLabels,
                accountIndex, selected -> {
                    if (selected < 0 || selected >= accounts.size()) return;
                    LedgerDb.Account account = accounts.get(selected);
                    settings.setDefaultAccountId(currentLedgerId, account.id);
                    setChoiceValue(accountRow, "默认记账账户", accountLabels.get(selected));
                    changed();
                }));
        card.addView(accountRow, rowParamsWithTop());

        List<String> keepers = InputCatalog.mergeBookkeepers(
                new LinkedHashSet<>(db.getBookkeepers(currentLedgerId)));
        int keeperIndex = Math.max(0, keepers.indexOf(settings.defaultBookkeeper()));
        TextView keeperRow = choiceRow("默认记账人",
                keepers.isEmpty() ? "本人" : keepers.get(keeperIndex));
        keeperRow.setOnClickListener(v -> showSingleChoice("选择默认记账人", keepers,
                keeperIndex, selected -> {
                    if (selected < 0 || selected >= keepers.size()) return;
                    String value = keepers.get(selected);
                    settings.setDefaultBookkeeper(value);
                    setChoiceValue(keeperRow, "默认记账人", value);
                    changed();
                }));
        card.addView(keeperRow, rowParamsWithTop());
        content.addView(card);
    }

    private void addBehaviorCard() {
        LinearLayout card = card("✨  使用习惯", CartoonStyle.SOFT_LAVENDER,
                "启动页面、侧栏反馈和保存后的页面去向。");

        String[] pages = {"首页", "日历", "账户", "统计", "设置"};
        int pageIndex = Math.max(0, Math.min(pages.length - 1, settings.startupPage()));
        TextView startup = choiceRow("启动后显示", pages[pageIndex]);
        startup.setOnClickListener(v -> showSingleChoice("选择启动页面", asList(pages),
                pageIndex, selected -> {
                    settings.setStartupPage(selected);
                    setChoiceValue(startup, "启动后显示", pages[selected]);
                    changed();
                }));
        card.addView(startup, rowParams());

        CheckBox animations = toggleRow("启用侧栏动画",
                "关闭后侧栏会立即打开或关闭", settings.animationsEnabled(), checked -> {
                    settings.setAnimationsEnabled(checked);
                    changed();
                });
        card.addView(animations, rowParamsWithTop());

        CheckBox returnHome = toggleRow("保存账单后返回首页",
                "关闭时保留当前页面", settings.returnHomeAfterSave(), checked -> {
                    settings.setReturnHomeAfterSave(checked);
                    changed();
                });
        card.addView(returnHome, rowParamsWithTop());
        content.addView(card);
    }

    private void addManagementCard() {
        LinearLayout card = card("🧰  管理入口", CartoonStyle.SOFT_SKY,
                "点击整行进入对应管理功能。");

        TextView ledgers = choiceRow("账本管理", "新增、切换和整理账本");
        ledgers.setOnClickListener(v -> {
            if (listener != null) listener.onOpenLedgerManagement();
        });
        card.addView(ledgers, rowParams());

        TextView accounts = choiceRow("账户管理", "新增账户并查看余额");
        accounts.setOnClickListener(v -> {
            if (listener != null) listener.onOpenAccounts();
        });
        card.addView(accounts, rowParamsWithTop());
        content.addView(card);
    }

    private void addAboutCard() {
        LinearLayout card = card("ℹ️  关于", CartoonStyle.SURFACE,
                "LedgerBook Lite 1.5.0\n数据保存在当前手机的本地 SQLite 数据库。");
        TextView diagnostic = choiceRow("设置交互自检", "点击后应立即弹出确认窗口");
        diagnostic.setOnClickListener(v -> new AlertDialog.Builder(activity)
                .setTitle("设置交互正常")
                .setMessage("当前设置页已经接收到点击事件。")
                .setPositiveButton("确定", null)
                .show());
        card.addView(diagnostic, rowParams());
        content.addView(card);
    }

    private LinearLayout card(String title, int fill, String description) {
        LinearLayout card = V13Ui.card(activity, fill);
        card.addView(V13Ui.text(activity, title, 17, CartoonStyle.INK, true));
        TextView detail = V13Ui.text(activity, description, 13, CartoonStyle.MUTED, false);
        detail.setLineSpacing(0f, 1.15f);
        detail.setPadding(0, V13Ui.dp(activity, 4), 0, V13Ui.dp(activity, 8));
        card.addView(detail);
        return card;
    }

    private TextView choiceRow(String title, String value) {
        TextView row = V13Ui.text(activity, "", 15, CartoonStyle.INK, true);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLineSpacing(0f, 1.14f);
        row.setPadding(V13Ui.dp(activity, 14), V13Ui.dp(activity, 10),
                V13Ui.dp(activity, 14), V13Ui.dp(activity, 10));
        row.setBackground(V13Ui.panel(activity, CartoonStyle.SURFACE,
                0xFFE1CDB4, 1, 16));
        row.setClickable(true);
        row.setFocusable(true);
        row.setMinHeight(V13Ui.dp(activity, 62));
        setChoiceValue(row, title, value);
        return row;
    }

    private void setChoiceValue(TextView row, String title, String value) {
        row.setText(title + "  ›\n" + (value == null ? "" : value));
        row.setContentDescription(title + "，当前值：" + value + "，点击修改");
    }

    private CheckBox toggleRow(String title, String description, boolean checked,
                               java.util.function.Consumer<Boolean> onChanged) {
        CheckBox row = new CheckBox(activity);
        row.setText(title + "\n" + description);
        row.setTextSize(15);
        row.setTextColor(CartoonStyle.INK);
        row.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(V13Ui.dp(activity, 12), V13Ui.dp(activity, 9),
                V13Ui.dp(activity, 12), V13Ui.dp(activity, 9));
        row.setBackground(V13Ui.panel(activity, CartoonStyle.SURFACE,
                0xFFE1CDB4, 1, 16));
        row.setChecked(checked);
        row.setMinHeight(V13Ui.dp(activity, 64));
        row.setOnCheckedChangeListener((button, value) -> onChanged.accept(value));
        return row;
    }

    private void showSingleChoice(String title, List<String> labels, int selected,
                                  IntConsumer callback) {
        if (labels == null || labels.isEmpty()) {
            Toast.makeText(activity, "暂无可选项", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = labels.toArray(new String[0]);
        AlertDialog[] dialog = new AlertDialog[1];
        dialog[0] = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setSingleChoiceItems(items, Math.max(0, Math.min(selected, items.length - 1)),
                        (whichDialog, which) -> {
                            callback.accept(which);
                            dialog[0].dismiss();
                        })
                .setNegativeButton("取消", null)
                .create();
        dialog[0].show();
    }

    private void showIconLibraryChoice() {
        showSingleChoice("浏览分类图标", asList(new String[]{"支出分类", "收入分类"}), 0,
                selected -> CategoryPickerDialog.show(activity,
                        selected == 0 ? LedgerDb.TYPE_EXPENSE : LedgerDb.TYPE_INCOME,
                        (icon, label) -> Toast.makeText(activity,
                                "已预览：" + label, Toast.LENGTH_SHORT).show()));
    }

    private void changed() {
        if (listener != null) listener.onSettingsChanged();
    }

    private void gap() {
        content.addView(V13Ui.gap(activity, 10));
    }

    private LinearLayout.LayoutParams rowParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams rowParamsWithTop() {
        LinearLayout.LayoutParams params = rowParams();
        params.topMargin = V13Ui.dp(activity, 7);
        return params;
    }

    private static List<String> asList(String[] values) {
        List<String> result = new ArrayList<>();
        java.util.Collections.addAll(result, values);
        return result;
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
