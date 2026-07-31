package com.ledgerbook.lite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class InputCatalog {
    private InputCatalog() {
    }

    public static List<Option> categories(String transactionType) {
        if (LedgerDb.TYPE_TRANSFER.equals(transactionType)) {
            return Arrays.asList(new Option("↔", "账户转账", false));
        }
        List<Option> result = new ArrayList<>();
        if (LedgerDb.TYPE_INCOME.equals(transactionType)) {
            result.add(new Option("💰", "工资", false));
            result.add(new Option("🧧", "奖金", false));
            result.add(new Option("🧑‍💻", "兼职", false));
            result.add(new Option("📈", "理财", false));
            result.add(new Option("🧾", "报销", false));
            result.add(new Option("🌟", "其他", false));
        } else {
            result.add(new Option("🍜", "餐饮", false));
            result.add(new Option("🚕", "交通", false));
            result.add(new Option("🛍️", "购物", false));
            result.add(new Option("🏠", "居住", false));
            result.add(new Option("🎮", "娱乐", false));
            result.add(new Option("💊", "医疗", false));
            result.add(new Option("📚", "教育", false));
            result.add(new Option("📱", "通讯", false));
            result.add(new Option("🎁", "人情", false));
            result.add(new Option("🧺", "其他", false));
        }
        result.add(new Option("✏️", "自定义", true));
        return result;
    }

    public static List<String> mergeBookkeepers(Set<String> history) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.add("本人");
        merged.add("家人");
        merged.add("伴侣");
        merged.add("孩子");
        if (history != null) {
            for (String value : history) {
                if (value != null && !value.trim().isEmpty()) {
                    merged.add(value.trim());
                }
            }
        }
        return new ArrayList<>(merged);
    }

    public static final class Option {
        public final String icon;
        public final String label;
        public final boolean custom;

        public Option(String icon, String label, boolean custom) {
            this.icon = icon;
            this.label = label;
            this.custom = custom;
        }

        @Override
        public String toString() {
            return icon + "  " + label;
        }
    }
}
