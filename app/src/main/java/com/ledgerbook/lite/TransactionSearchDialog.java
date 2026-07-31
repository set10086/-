package com.ledgerbook.lite;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class TransactionSearchDialog {
    public interface Listener {
        void onTransactionsChanged();
    }

    private static final int PAGE_SIZE = 200;

    private TransactionSearchDialog() {
    }

    public static void show(Activity activity, LedgerDb db, long currentLedgerId, Listener listener) {
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CartoonStyle.BACKGROUND);
        root.setPadding(V13Ui.dp(activity, 14), V13Ui.dp(activity, 14),
                V13Ui.dp(activity, 14), V13Ui.dp(activity, 12));

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView close = V13Ui.button(activity, "‹", CartoonStyle.SURFACE);
        close.setTextSize(26);
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(V13Ui.dp(activity, 48), V13Ui.dp(activity, 44)));
        TextView title = V13Ui.text(activity, "搜索账单", 21, CartoonStyle.INK, true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, V13Ui.dp(activity, 44), 1f));
        header.addView(new View(activity), new LinearLayout.LayoutParams(V13Ui.dp(activity, 48), 1));
        root.addView(header);
        root.addView(V13Ui.gap(activity, 10));

        final boolean[] allLedgers = {false};
        LinearLayout scopes = new LinearLayout(activity);
        TextView current = V13Ui.button(activity, "当前账本", CartoonStyle.CREAM_YELLOW);
        TextView all = V13Ui.button(activity, "全部账本", CartoonStyle.SURFACE);
        scopes.addView(current, new LinearLayout.LayoutParams(0, V13Ui.dp(activity, 44), 1f));
        scopes.addView(new View(activity), new LinearLayout.LayoutParams(V13Ui.dp(activity, 8), 1));
        scopes.addView(all, new LinearLayout.LayoutParams(0, V13Ui.dp(activity, 44), 1f));
        root.addView(scopes);
        root.addView(V13Ui.gap(activity, 10));

        EditText query = new EditText(activity);
        query.setSingleLine(true);
        query.setHint("搜索分类、备注、标签、账户或记账人");
        query.setTextSize(16);
        query.setTextColor(CartoonStyle.INK);
        query.setHintTextColor(CartoonStyle.MUTED);
        query.setPadding(V13Ui.dp(activity, 15), V13Ui.dp(activity, 10),
                V13Ui.dp(activity, 15), V13Ui.dp(activity, 10));
        query.setBackground(V13Ui.panel(activity, CartoonStyle.SURFACE, 0xFFE2D3BE, 1, 18));
        root.addView(query, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 52)));
        root.addView(V13Ui.gap(activity, 10));

        ScrollView scroll = new ScrollView(activity);
        LinearLayout results = new LinearLayout(activity);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setPadding(0, 0, 0, V13Ui.dp(activity, 24));
        scroll.addView(results, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Handler handler = new Handler(Looper.getMainLooper());
        final int[] offset = {0};
        final Runnable[] searchTask = {null};
        final Runnable[] execute = {null};

        execute[0] = () -> {
            String text = query.getText().toString().trim();
            results.removeAllViews();
            offset[0] = 0;
            if (text.isEmpty()) {
                showMessage(activity, results, "🔎", "输入文字开始搜索",
                        "默认搜索当前账本，也可以切换到全部账本");
                return;
            }
            load(activity, db, results, currentLedgerId, allLedgers[0], text,
                    offset, listener, execute[0]);
        };

        View.OnClickListener scopeClick = v -> {
            allLedgers[0] = v == all;
            current.setBackground(V13Ui.panel(activity,
                    allLedgers[0] ? CartoonStyle.SURFACE : CartoonStyle.CREAM_YELLOW,
                    0xFFE2D3BE, 1, 18));
            all.setBackground(V13Ui.panel(activity,
                    allLedgers[0] ? CartoonStyle.CREAM_YELLOW : CartoonStyle.SURFACE,
                    0xFFE2D3BE, 1, 18));
            execute[0].run();
        };
        current.setOnClickListener(scopeClick);
        all.setOnClickListener(scopeClick);

        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchTask[0] != null) handler.removeCallbacks(searchTask[0]);
                searchTask[0] = execute[0];
                handler.postDelayed(searchTask[0], 300L);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setStatusBarColor(CartoonStyle.CREAM_YELLOW);
            window.setNavigationBarColor(CartoonStyle.BACKGROUND);
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnDismissListener(ignored -> {
            if (searchTask[0] != null) handler.removeCallbacks(searchTask[0]);
        });
        dialog.show();
        showMessage(activity, results, "🔎", "输入文字开始搜索",
                "默认搜索当前账本，也可以切换到全部账本");
        query.requestFocus();
        query.postDelayed(() -> {
            InputMethodManager manager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) manager.showSoftInput(query, InputMethodManager.SHOW_IMPLICIT);
        }, 180L);
    }

    private static void load(Activity activity, LedgerDb db, LinearLayout results,
                             long currentLedgerId, boolean allLedgers, String query,
                             int[] offset, Listener listener, Runnable refresh) {
        try {
            Long scope = allLedgers ? TransactionFilter.ALL_LEDGERS : currentLedgerId;
            List<LedgerDb.TxnView> rows = db.searchTransactions(scope, query, offset[0], PAGE_SIZE);
            if (rows.isEmpty() && offset[0] == 0) {
                showMessage(activity, results, "🧾", "没有找到相关账单", "换个关键词或搜索全部账本试试");
                return;
            }
            for (LedgerDb.TxnView txn : rows) {
                TextView row = V13Ui.transactionRow(activity, txn, allLedgers);
                row.setOnClickListener(v -> TransactionDetailDialog.show(activity, db, txn, () -> {
                    if (listener != null) listener.onTransactionsChanged();
                    refresh.run();
                }));
                results.addView(row);
                results.addView(V13Ui.gap(activity, 8));
            }
            offset[0] += rows.size();
            if (rows.size() == PAGE_SIZE) {
                TextView more = V13Ui.button(activity, "继续加载", CartoonStyle.SOFT_SKY);
                more.setOnClickListener(v -> {
                    results.removeView(more);
                    load(activity, db, results, currentLedgerId, allLedgers, query,
                            offset, listener, refresh);
                });
                results.addView(more, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 48)));
            }
        } catch (RuntimeException error) {
            Toast.makeText(activity, error.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void showMessage(Activity activity, LinearLayout parent,
                                    String icon, String title, String subtitle) {
        parent.removeAllViews();
        TextView message = V13Ui.text(activity, icon + "\n" + title + "\n" + subtitle,
                16, CartoonStyle.MUTED, true);
        message.setGravity(Gravity.CENTER);
        message.setLineSpacing(0f, 1.25f);
        message.setBackgroundColor(Color.TRANSPARENT);
        parent.addView(message, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 220)));
    }
}
