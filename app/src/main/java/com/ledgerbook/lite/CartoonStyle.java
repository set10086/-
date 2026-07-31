package com.ledgerbook.lite;

public final class CartoonStyle {
    private CartoonStyle() {
    }

    public static final int BACKGROUND = 0xFFFFF8E8;
    public static final int SURFACE = 0xFFFFFFFF;
    public static final int INK = 0xFF3E3A35;
    public static final int MUTED = 0xFF7C746B;
    public static final int OUTLINE = 0xFF554D44;
    public static final int CREAM_YELLOW = 0xFFFFD66B;
    public static final int SOFT_YELLOW = 0xFFFFEDB2;
    public static final int PEACH = 0xFFFFA69A;
    public static final int SOFT_PEACH = 0xFFFFE0DA;
    public static final int SKY = 0xFF8CCBFF;
    public static final int SOFT_SKY = 0xFFDCEFFF;
    public static final int AVOCADO = 0xFF91CF82;
    public static final int SOFT_GREEN = 0xFFDFF2DA;
    public static final int LAVENDER = 0xFFB9A8F7;
    public static final int SOFT_LAVENDER = 0xFFEAE3FF;

    public static final int INCOME = 0xFF4EAA71;
    public static final int EXPENSE = 0xFFE66D62;
    public static final int TRANSFER = 0xFF5A91D8;

    public static final int CARD_RADIUS_DP = 24;
    public static final int BUTTON_RADIUS_DP = 18;

    public static int transactionColor(String type) {
        if (LedgerDb.TYPE_EXPENSE.equals(type)) {
            return EXPENSE;
        }
        if (LedgerDb.TYPE_INCOME.equals(type)) {
            return INCOME;
        }
        return TRANSFER;
    }

    public static int transactionTint(String type) {
        if (LedgerDb.TYPE_EXPENSE.equals(type)) {
            return SOFT_PEACH;
        }
        if (LedgerDb.TYPE_INCOME.equals(type)) {
            return SOFT_GREEN;
        }
        return SOFT_SKY;
    }

    public static String transactionIcon(String type, String category) {
        String value = category == null ? "" : category;
        if (LedgerDb.TYPE_TRANSFER.equals(type)) {
            return "↔";
        }
        if (LedgerDb.TYPE_INCOME.equals(type)) {
            if (value.contains("工资")) return "💰";
            if (value.contains("奖金") || value.contains("红包")) return "🎁";
            if (value.contains("利息") || value.contains("理财")) return "🌱";
            return "🌟";
        }
        if (value.contains("餐") || value.contains("吃") || value.contains("饮")) return "🍜";
        if (value.contains("交通") || value.contains("车") || value.contains("油")) return "🚌";
        if (value.contains("购物") || value.contains("衣") || value.contains("日用")) return "🛍";
        if (value.contains("住房") || value.contains("房租") || value.contains("物业")) return "🏠";
        if (value.contains("娱乐") || value.contains("游戏") || value.contains("电影")) return "🎮";
        if (value.contains("医疗") || value.contains("药")) return "🩹";
        if (value.contains("学习") || value.contains("书")) return "📚";
        if (value.contains("旅行") || value.contains("酒店")) return "🧳";
        return "🧾";
    }

    public static String accountIcon(String type) {
        if ("现金账户".equals(type)) return "👛";
        if ("储蓄账户".equals(type)) return "🏦";
        if ("信用卡账户".equals(type)) return "💳";
        if ("投资账户".equals(type)) return "🌱";
        return "🧺";
    }

    public static int accountTint(String type) {
        if ("现金账户".equals(type)) return SOFT_YELLOW;
        if ("储蓄账户".equals(type)) return SOFT_SKY;
        if ("信用卡账户".equals(type)) return SOFT_PEACH;
        if ("投资账户".equals(type)) return SOFT_GREEN;
        return SOFT_LAVENDER;
    }
}
