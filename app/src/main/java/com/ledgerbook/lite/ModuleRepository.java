package com.ledgerbook.lite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Provides ordered, persisted access to optional V1.5 product modules. */
public final class ModuleRepository {
    public static final String QUICK_ENTRY = "quick_entry";
    public static final String BUDGET = "budget";
    public static final String DATA_MANAGEMENT = "data_management";
    public static final String TEMPLATES = "templates";
    public static final String RECURRING = "recurring";
    public static final String SUBSCRIPTIONS = "subscriptions";
    public static final String PRIVACY_LOCK = "privacy_lock";

    public interface Backend {
        List<Module> load();
        void setEnabled(String key, boolean enabled, long now);
        void swapOrder(String firstKey, int firstOrder,
                       String secondKey, int secondOrder, long now);
    }

    public static final class Module {
        public final String key;
        public final boolean enabled;
        public final int sortOrder;

        public Module(String key, boolean enabled, int sortOrder) {
            this.key = requireKey(key);
            this.enabled = enabled;
            this.sortOrder = sortOrder;
        }
    }

    private final Backend backend;

    public ModuleRepository(Backend backend) {
        if (backend == null) throw new IllegalArgumentException("backend cannot be null");
        this.backend = backend;
    }

    public ModuleRepository(LedgerDb db) {
        this(new AndroidBackend(db));
    }

    public List<Module> list() {
        List<Module> values = new ArrayList<>(backend.load());
        values.sort(Comparator.comparingInt((Module value) -> value.sortOrder)
                .thenComparing(value -> value.key));
        return values;
    }

    public boolean isEnabled(String key) {
        String clean = requireKey(key);
        for (Module module : list()) {
            if (module.key.equals(clean)) return module.enabled;
        }
        return false;
    }

    public void setEnabled(String key, boolean enabled) {
        String clean = requireExisting(key);
        backend.setEnabled(clean, enabled, System.currentTimeMillis());
    }

    /** Moves a module one position. direction must be -1 (up) or 1 (down). */
    public void move(String key, int direction) {
        if (direction != -1 && direction != 1) {
            throw new IllegalArgumentException("direction must be -1 or 1");
        }
        String clean = requireKey(key);
        List<Module> values = list();
        int index = indexOf(values, clean);
        if (index < 0) throw new IllegalArgumentException("未知模块：" + clean);
        int target = index + direction;
        if (target < 0 || target >= values.size()) return;
        Module current = values.get(index);
        Module neighbor = values.get(target);
        backend.swapOrder(current.key, current.sortOrder,
                neighbor.key, neighbor.sortOrder, System.currentTimeMillis());
    }

    public static String titleFor(String key) {
        switch (requireKey(key)) {
            case QUICK_ENTRY: return "极速记账";
            case BUDGET: return "预算";
            case DATA_MANAGEMENT: return "数据管理";
            case TEMPLATES: return "记账模板";
            case RECURRING: return "周期记账";
            case SUBSCRIPTIONS: return "订阅管理";
            case PRIVACY_LOCK: return "隐私锁";
            default: return key;
        }
    }

    public static String descriptionFor(String key) {
        switch (requireKey(key)) {
            case QUICK_ENTRY: return "金额、分类、保存的紧凑记账路径";
            case BUDGET: return "总预算与分类预算执行情况";
            case DATA_MANAGEMENT: return "导出、备份、恢复和安全快照";
            case TEMPLATES: return "保存常用账单并一键复用";
            case RECURRING: return "到期生成待确认账单";
            case SUBSCRIPTIONS: return "续费日期和年度成本提醒";
            case PRIVACY_LOCK: return "PIN 与自动锁定保护";
            default: return "可选功能模块";
        }
    }

    private String requireExisting(String key) {
        String clean = requireKey(key);
        if (indexOf(list(), clean) < 0) throw new IllegalArgumentException("未知模块：" + clean);
        return clean;
    }

    private static int indexOf(List<Module> modules, String key) {
        for (int index = 0; index < modules.size(); index++) {
            if (modules.get(index).key.equals(key)) return index;
        }
        return -1;
    }

    private static String requireKey(String key) {
        String clean = key == null ? "" : key.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("module key cannot be empty");
        return clean;
    }

    private static final class AndroidBackend implements Backend {
        private final LedgerDb helper;

        AndroidBackend(LedgerDb helper) {
            if (helper == null) throw new IllegalArgumentException("db cannot be null");
            this.helper = helper;
        }

        @Override public List<Module> load() {
            List<Module> result = new ArrayList<>();
            try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                    "SELECT module_key, enabled, sort_order FROM app_modules "
                            + "ORDER BY sort_order, module_key", null)) {
                while (cursor.moveToNext()) {
                    result.add(new Module(cursor.getString(0), cursor.getInt(1) == 1,
                            cursor.getInt(2)));
                }
            }
            return result;
        }

        @Override public void setEnabled(String key, boolean enabled, long now) {
            ContentValues values = new ContentValues();
            values.put("enabled", enabled ? 1 : 0);
            values.put("updated_at", now);
            int changed = helper.getWritableDatabase().update(
                    "app_modules", values, "module_key=?", new String[]{key});
            if (changed != 1) throw new IllegalArgumentException("未知模块：" + key);
        }

        @Override public void swapOrder(String firstKey, int firstOrder,
                                        String secondKey, int secondOrder, long now) {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                if (updateOrder(db, firstKey, secondOrder, now) != 1
                        || updateOrder(db, secondKey, firstOrder, now) != 1) {
                    throw new IllegalStateException("模块排序保存失败");
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        private static int updateOrder(SQLiteDatabase db, String key, int order, long now) {
            ContentValues values = new ContentValues();
            values.put("sort_order", order);
            values.put("updated_at", now);
            return db.update("app_modules", values,
                    "module_key=?", new String[]{key});
        }
    }
}
