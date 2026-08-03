package com.ledgerbook.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Interactive local data management with explicit restore confirmation. */
public final class DataManagementDialog {
    private DataManagementDialog() {}

    public static void show(Activity activity, LedgerDb db, long ledgerId, Runnable onRestored) {
        DataManagementService service = new DataManagementService(activity, db);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(V13Ui.dp(activity, 12), V13Ui.dp(activity, 8),
                V13Ui.dp(activity, 12), V13Ui.dp(activity, 18));
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("💾 数据管理")
                .setView(scroll)
                .setPositiveButton("完成", null)
                .create();
        Runnable[] render = new Runnable[1];
        render[0] = () -> render(activity, content, service, ledgerId, onRestored, render[0]);
        render[0].run();
        dialog.show();
    }

    private static void render(Activity activity, LinearLayout content,
                               DataManagementService service, long ledgerId,
                               Runnable onRestored, Runnable rerender) {
        content.removeAllViews();
        LinearLayout actions = V13Ui.card(activity, CartoonStyle.SOFT_GREEN);
        actions.addView(V13Ui.text(activity, "本地导出与备份", 17, CartoonStyle.INK, true));
        actions.addView(description(activity,
                "文件保存在应用的 Documents/LedgerBook 目录，不会上传网络。"));

        TextView csv = V13Ui.button(activity, "导出 CSV", CartoonStyle.SURFACE);
        csv.setOnClickListener(v -> {
            try {
                File file = service.exportCsv(ledgerId);
                showPath(activity, "CSV 已导出", file);
            } catch (RuntimeException | IOException error) {
                toast(activity, error);
            }
        });
        actions.addView(csv, buttonParams(activity));
        actions.addView(V13Ui.gap(activity, 7));

        TextView backup = V13Ui.button(activity, "创建 JSON 备份", CartoonStyle.SOFT_YELLOW);
        backup.setOnClickListener(v -> {
            try {
                File file = service.createJsonBackup();
                showPath(activity, "JSON 备份已创建", file);
                rerender.run();
            } catch (RuntimeException | IOException error) {
                toast(activity, error);
            }
        });
        actions.addView(backup, buttonParams(activity));
        content.addView(actions);
        content.addView(V13Ui.gap(activity, 12));

        List<File> backups = service.listBackups();
        content.addView(V13Ui.text(activity, "可恢复备份（" + backups.size() + "）",
                16, CartoonStyle.INK, true));
        content.addView(V13Ui.gap(activity, 7));
        if (backups.isEmpty()) {
            LinearLayout empty = V13Ui.card(activity, CartoonStyle.SURFACE);
            empty.addView(description(activity, "尚无 JSON 备份。"));
            content.addView(empty);
            return;
        }

        for (File file : backups) {
            LinearLayout card = V13Ui.card(activity,
                    file.getName().contains("safety")
                            ? CartoonStyle.SOFT_SKY : CartoonStyle.SOFT_LAVENDER);
            card.addView(V13Ui.text(activity,
                    file.getName().contains("safety") ? "🛡️ 安全快照" : "📦 完整备份",
                    15, CartoonStyle.INK, true));
            card.addView(description(activity,
                    formatDate(file.lastModified()) + " · " + readableSize(file.length())
                            + "\n" + file.getName()));
            TextView restore = V13Ui.button(activity, "恢复此备份", CartoonStyle.SOFT_PEACH);
            restore.setOnClickListener(v -> new AlertDialog.Builder(activity)
                    .setTitle("恢复此备份？")
                    .setMessage("当前数据将被替换。恢复前会自动创建安全快照；恢复失败会回滚事务。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确认恢复", (ignored, which) -> {
                        try {
                            service.restoreJsonBackup(file);
                            Toast.makeText(activity, "数据恢复完成",
                                    Toast.LENGTH_LONG).show();
                            if (onRestored != null) onRestored.run();
                            rerender.run();
                        } catch (RuntimeException | IOException error) {
                            toast(activity, error);
                        }
                    }).show());
            card.addView(restore, buttonParams(activity));
            content.addView(card);
            content.addView(V13Ui.gap(activity, 8));
        }
    }

    private static TextView description(Activity activity, String value) {
        TextView view = V13Ui.text(activity, value, 13, CartoonStyle.MUTED, false);
        view.setLineSpacing(0f, 1.15f);
        view.setPadding(0, V13Ui.dp(activity, 4), 0, V13Ui.dp(activity, 8));
        return view;
    }

    private static LinearLayout.LayoutParams buttonParams(Activity activity) {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, V13Ui.dp(activity, 46));
    }

    private static void showPath(Activity activity, String title, File file) {
        new AlertDialog.Builder(activity).setTitle(title)
                .setMessage(file.getAbsolutePath())
                .setPositiveButton("知道了", null).show();
    }

    private static String formatDate(long time) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(time));
    }

    private static String readableSize(long size) {
        if (size < 1024L) return size + " B";
        if (size < 1024L * 1024L) return String.format(Locale.ROOT, "%.1f KB", size / 1024d);
        return String.format(Locale.ROOT, "%.1f MB", size / 1024d / 1024d);
    }

    private static void toast(Activity activity, Exception error) {
        String message = error.getMessage();
        Toast.makeText(activity,
                message == null || message.trim().isEmpty() ? "数据操作失败" : message,
                Toast.LENGTH_LONG).show();
    }
}
