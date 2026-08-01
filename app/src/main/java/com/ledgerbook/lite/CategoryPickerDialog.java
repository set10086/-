package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class CategoryPickerDialog {
    public interface Listener {
        void onCategory(String icon, String label);
    }

    private CategoryPickerDialog() {
    }

    public static void show(Activity activity, String transactionType, Listener listener) {
        List<InputCatalog.Group> groups = InputCatalog.groups(transactionType);
        if (groups.isEmpty()) {
            Toast.makeText(activity, "暂无可用分类", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));

        EditText search = new EditText(activity);
        search.setHint("搜索具体分类");
        search.setSingleLine(true);
        search.setTextColor(CartoonStyle.INK);
        search.setHintTextColor(CartoonStyle.MUTED);
        search.setPadding(dp(activity, 14), dp(activity, 9), dp(activity, 14), dp(activity, 9));
        search.setBackground(panel(activity, CartoonStyle.SURFACE, 16));
        root.addView(search, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 50)));
        root.addView(space(activity, 8));

        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.HORIZONTAL);

        ScrollView groupScroll = new ScrollView(activity);
        groupScroll.setFillViewport(true);
        LinearLayout groupList = new LinearLayout(activity);
        groupList.setOrientation(LinearLayout.VERTICAL);
        groupScroll.addView(groupList, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(groupScroll, new LinearLayout.LayoutParams(
                dp(activity, 102), dp(activity, 430)));
        body.addView(horizontalSpace(activity, 8));

        ScrollView optionScroll = new ScrollView(activity);
        GridLayout optionGrid = new GridLayout(activity);
        optionGrid.setColumnCount(4);
        optionGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        optionScroll.addView(optionGrid, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(optionScroll, new LinearLayout.LayoutParams(
                0, dp(activity, 430), 1f));
        root.addView(body);
        root.addView(space(activity, 8));

        TextView customButton = button(activity, "自定义分类", CartoonStyle.SOFT_PEACH, 15);
        root.addView(customButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 48)));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(LedgerDb.TYPE_INCOME.equals(transactionType)
                        ? "选择收入分类" : LedgerDb.TYPE_TRANSFER.equals(transactionType)
                        ? "选择转账分类" : "选择支出分类")
                .setView(root)
                .setNegativeButton("取消", null)
                .create();

        InputCatalog.Group[] selected = {groups.get(0)};
        Runnable[] renderGroups = new Runnable[1];
        java.util.function.Consumer<List<InputCatalog.Option>> renderOptions = options -> {
            optionGrid.removeAllViews();
            if (options.isEmpty()) {
                TextView empty = text(activity, "没有找到相关分类", 14,
                        CartoonStyle.MUTED, true);
                empty.setGravity(Gravity.CENTER);
                GridLayout.LayoutParams emptyParams = new GridLayout.LayoutParams();
                emptyParams.width = GridLayout.LayoutParams.MATCH_PARENT;
                emptyParams.height = dp(activity, 120);
                emptyParams.columnSpec = GridLayout.spec(0, 4);
                optionGrid.addView(empty, emptyParams);
                return;
            }
            int[] fills = {CartoonStyle.SOFT_YELLOW, CartoonStyle.SOFT_PEACH,
                    CartoonStyle.SOFT_SKY, CartoonStyle.SOFT_GREEN,
                    CartoonStyle.SOFT_LAVENDER};
            int index = 0;
            for (InputCatalog.Option option : options) {
                if (option.custom) continue;
                LinearLayout item = optionItem(activity, transactionType, option,
                        fills[index % fills.length]);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = dp(activity, 92);
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                params.setMargins(dp(activity, 3), dp(activity, 3),
                        dp(activity, 3), dp(activity, 3));
                optionGrid.addView(item, params);
                item.setOnClickListener(v -> {
                    listener.onCategory(option.icon, option.label);
                    dialog.dismiss();
                });
                index++;
            }
        };

        renderGroups[0] = () -> {
            groupList.removeAllViews();
            for (InputCatalog.Group group : groups) {
                boolean active = group == selected[0];
                LinearLayout item = groupItem(activity, transactionType, group,
                        active ? CartoonStyle.CREAM_YELLOW : CartoonStyle.SURFACE);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 76));
                params.setMargins(0, 0, 0, dp(activity, 5));
                groupList.addView(item, params);
                item.setOnClickListener(v -> {
                    selected[0] = group;
                    search.setText("");
                    renderGroups[0].run();
                    renderOptions.accept(group.options);
                    optionScroll.smoothScrollTo(0, 0);
                });
            }
        };

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim();
                renderOptions.accept(query.isEmpty()
                        ? selected[0].options : InputCatalog.search(transactionType, query));
                optionScroll.smoothScrollTo(0, 0);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        customButton.setVisibility(LedgerDb.TYPE_TRANSFER.equals(transactionType)
                ? View.GONE : View.VISIBLE);
        customButton.setOnClickListener(v -> showCustom(activity, listener, dialog));

        renderGroups[0].run();
        renderOptions.accept(selected[0].options);
        dialog.show();
    }

    private static LinearLayout optionItem(Activity activity, String transactionType,
                                           InputCatalog.Option option, int fill) {
        LinearLayout item = new LinearLayout(activity);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(activity, 3), dp(activity, 5), dp(activity, 3), dp(activity, 5));
        item.setBackground(panel(activity, fill, 16));
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription(option.group + "，" + option.name + "，点击选择");

        CrayonIconView icon = new CrayonIconView(activity);
        icon.setCategory(transactionType, option.label);
        item.addView(icon, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));

        TextView label = text(activity, option.name, 12, CartoonStyle.INK, true);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        item.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return item;
    }

    private static LinearLayout groupItem(Activity activity, String transactionType,
                                          InputCatalog.Group group, int fill) {
        LinearLayout item = new LinearLayout(activity);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(activity, 3), dp(activity, 5), dp(activity, 3), dp(activity, 5));
        item.setBackground(panel(activity, fill, 15));
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription(group.label + "分组，点击查看");

        CrayonIconView icon = new CrayonIconView(activity);
        icon.setCategory(transactionType, group.label);
        item.addView(icon, new LinearLayout.LayoutParams(dp(activity, 38), dp(activity, 38)));

        TextView label = text(activity, group.label, 12, CartoonStyle.INK, true);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        item.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return item;
    }

    private static void showCustom(Activity activity, Listener listener, AlertDialog parent) {
        EditText input = new EditText(activity);
        input.setHint("输入自定义分类名称");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(true);
        input.setPadding(dp(activity, 18), dp(activity, 8), dp(activity, 18), dp(activity, 8));
        AlertDialog custom = new AlertDialog.Builder(activity)
                .setTitle("自定义分类")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", null)
                .create();
        custom.setOnShowListener(ignored -> custom.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(activity, "请输入分类名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    listener.onCategory("", value);
                    custom.dismiss();
                    parent.dismiss();
                }));
        custom.show();
    }

    private static TextView button(Activity activity, String value, int fill, int sp) {
        TextView view = text(activity, value, sp, CartoonStyle.INK, true);
        view.setGravity(Gravity.CENTER);
        view.setBackground(panel(activity, fill, 17));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private static TextView text(Activity activity, String value, int sp, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static GradientDrawable panel(Activity activity, int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(activity, radiusDp));
        drawable.setStroke(dp(activity, 1), 0xFFE6D3BB);
        return drawable;
    }

    private static View space(Activity activity, int heightDp) {
        View view = new View(activity);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, heightDp)));
        return view;
    }

    private static View horizontalSpace(Activity activity, int widthDp) {
        View view = new View(activity);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, widthDp), 1));
        return view;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
