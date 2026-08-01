package com.ledgerbook.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class InputCatalog {
    private static final List<Group> EXPENSE_GROUPS =
            Collections.unmodifiableList(buildExpenseGroups());
    private static final List<Group> INCOME_GROUPS =
            Collections.unmodifiableList(buildIncomeGroups());
    private static final List<Group> TRANSFER_GROUPS =
            Collections.singletonList(new Group("↔", "账户转账",
                    Collections.singletonList(new Option(
                            "↔", "账户转账", "账户转账", "账户转账", false))));

    private InputCatalog() {
    }

    public static List<Group> groups(String transactionType) {
        if (LedgerDb.TYPE_TRANSFER.equals(transactionType)) return TRANSFER_GROUPS;
        if (LedgerDb.TYPE_INCOME.equals(transactionType)) return INCOME_GROUPS;
        return EXPENSE_GROUPS;
    }

    public static List<Option> categories(String transactionType) {
        List<Option> result = new ArrayList<>();
        for (Group group : groups(transactionType)) result.addAll(group.options);
        if (!LedgerDb.TYPE_TRANSFER.equals(transactionType)) {
            result.add(new Option("✏️", "自定义", "自定义", "自定义", true));
        }
        return result;
    }

    public static List<Option> search(String transactionType, String query) {
        String clean = safe(query).trim().toLowerCase(Locale.ROOT);
        if (clean.isEmpty()) return categories(transactionType);
        List<Option> result = new ArrayList<>();
        for (Option option : categories(transactionType)) {
            if (option.name.toLowerCase(Locale.ROOT).contains(clean)
                    || option.group.toLowerCase(Locale.ROOT).contains(clean)
                    || option.label.toLowerCase(Locale.ROOT).contains(clean)) {
                result.add(option);
            }
        }
        return result;
    }

    public static String iconFor(String transactionType, String category) {
        String clean = safe(category).trim();
        if (LedgerDb.TYPE_TRANSFER.equals(transactionType)) return "↔";
        for (Group group : groups(transactionType)) {
            if (group.label.equals(clean)) return group.icon;
            for (Option option : group.options) {
                if (option.label.equals(clean) || option.name.equals(clean)) return option.icon;
            }
        }
        if (LedgerDb.TYPE_INCOME.equals(transactionType)) {
            if (clean.contains("工资") || clean.contains("薪")) return "💰";
            if (clean.contains("奖金") || clean.contains("红包")) return "🧧";
            if (clean.contains("理财") || clean.contains("利息")) return "📈";
            if (clean.contains("报销") || clean.contains("退款")) return "🧾";
            return "🌟";
        }
        if (clean.contains("餐") || clean.contains("吃") || clean.contains("饮")) return "🍽️";
        if (clean.contains("交通") || clean.contains("车") || clean.contains("油")) return "🚗";
        if (clean.contains("购物") || clean.contains("衣") || clean.contains("日用")) return "🛍️";
        if (clean.contains("房") || clean.contains("物业") || clean.contains("居住")) return "🏠";
        if (clean.contains("娱乐") || clean.contains("游戏") || clean.contains("电影")) return "🎮";
        if (clean.contains("医疗") || clean.contains("药")) return "🩺";
        if (clean.contains("学习") || clean.contains("教育") || clean.contains("书")) return "📚";
        if (clean.contains("旅行") || clean.contains("出差") || clean.contains("酒店")) return "🧳";
        return "🧾";
    }

    public static List<String> mergeBookkeepers(Set<String> history) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.add("本人");
        merged.add("家人");
        merged.add("伴侣");
        merged.add("孩子");
        if (history != null) {
            for (String value : history) {
                if (value != null && !value.trim().isEmpty()) merged.add(value.trim());
            }
        }
        return new ArrayList<>(merged);
    }

    private static List<Group> buildExpenseGroups() {
        List<Group> result = new ArrayList<>();
        addGroup(result, "⭐", "默认",
                "💼|办公", "🍪|零食", "📱|通讯", "🔧|修车", "🚇|交通", "🤝|社交",
                "👪|亲友", "🐾|宠物", "🏠|住房", "🎓|学费", "🧒|孩子", "🅿️|停车",
                "🚰|水费", "📦|快递", "🧓|老人", "🥬|蔬菜", "🎮|娱乐", "🏃|运动",
                "🎟️|彩票", "🛒|网购", "🌐|宽带", "🧻|日用品");
        addGroup(result, "💄", "美妆",
                "👝|美妆袋", "🫙|面霜", "🪞|化妆镜", "🎀|头饰", "💅|美甲",
                "💧|精华液", "🧴|护发素", "🎨|眼影", "🖌️|化妆刷", "🪮|梳子",
                "🧖|SPA", "🫧|洗面奶", "💋|唇彩", "✨|美容仪", "🤲|护手霜",
                "👁️|睫毛膏", "🌸|护肤品", "✒️|眼线笔", "🧁|粉底", "🥛|乳液",
                "🌹|香水", "🏥|医美", "🖍️|眉笔", "💄|口红", "🎭|面膜");
        addGroup(result, "👨‍👩‍👧", "家人",
                "👵|父母", "👦|男孩", "🐕|狗", "👧|女孩", "👩|女人", "👶|宝宝",
                "🧑‍🤝‍🧑|朋友", "👨|男人", "💕|恋爱", "🐈|猫", "🐾|宠物用品");
        addGroup(result, "📚", "教育",
                "📚|书籍", "🧑‍🏫|培训", "🏫|学费", "📝|考试", "✏️|文具",
                "🎒|学校", "🧠|学习");
        addGroup(result, "🧺", "其他",
                "👥|团费", "🤲|捐赠", "🎖️|党费", "🛡️|保险", "💼|工作",
                "🕘|临时支出", "🧾|其他");
        addGroup(result, "🌿", "园艺",
                "🪴|花盆", "🛠️|铲子", "🚿|洒水壶", "🌿|绿植", "🌵|多肉",
                "⛏️|锄头", "🌱|盆栽", "🌷|鲜花", "🍴|园艺叉", "🧰|园艺工具");
        addGroup(result, "🧱", "装修",
                "👷|人工费", "🧱|辅材", "🎨|油漆涂料", "🍳|厨房", "🚰|水路改造",
                "🖼️|装饰品", "🏢|外墙", "⚡|电路改造", "🚿|浴室柜", "🪵|地板",
                "🛋️|家具", "📐|设计费", "🪟|窗帘", "💡|灯具", "🛁|卫浴",
                "🔌|开关插座");
        addGroup(result, "🧻", "日常",
                "🧂|调料", "🌐|网费", "📦|快递", "💐|鲜花", "🩸|卫生用品",
                "🦷|牙膏", "☎️|话费", "🧻|卷纸", "🛒|购物", "🧼|纸巾",
                "💇|理发", "🧽|湿纸巾", "🔥|水电煤", "😴|眼罩", "🧹|清洁",
                "🪥|牙刷");
        addGroup(result, "🚗", "汽车",
                "🚨|罚款", "🛣️|过路费", "🛞|轮胎", "🫧|洗车", "📋|年检",
                "🛡️|车险", "🔋|充电", "🔧|维修保养", "⛽|加油", "🚘|车贷");
        addGroup(result, "🎒", "校园",
                "📖|课本", "🧑‍🏫|补习", "🏫|学杂费", "✏️|文具", "💻|网课",
                "🍱|校园餐", "🚌|校车");
        addGroup(result, "🎮", "娱乐",
                "🎁|打赏", "🏸|羽毛球", "🥳|聚会", "🎬|电影", "🎤|KTV",
                "🧩|拼图", "🎾|网球", "⛳|高尔夫", "🎟️|演唱会", "🏀|篮球",
                "🀄|麻将", "🏃|运动", "🏞️|景区门票", "🎵|音乐", "🏋️|健身房",
                "🧘|瑜伽", "🏊|游泳", "🎱|台球", "🎮|游戏", "🎡|游乐场",
                "🎣|钓鱼", "⚽|足球", "✈️|旅游", "🎸|乐器");
        addGroup(result, "🎁", "人情",
                "💞|亲密付", "🧧|红包", "🍽️|请客", "🎁|礼物", "🙏|孝心",
                "💐|探望", "💒|婚礼");
        addGroup(result, "🏢", "物业",
                "🏦|房贷", "🏢|物业费", "♨️|取暖费", "🔥|燃气", "🗑️|垃圾费",
                "🅿️|停车费", "🚰|水费", "💡|电费", "🔑|房租", "🔨|房屋维修");
        addGroup(result, "🧳", "出差",
                "💵|差旅津贴", "🧳|出差", "🥂|宴请招待", "🏨|酒店住宿",
                "✈️|机票", "🚄|高铁", "🚕|市内交通");
        addGroup(result, "🍽️", "餐饮",
                "🍰|甜品", "🧋|奶茶", "🍚|米面", "🍦|冰淇淋", "🫗|食用油",
                "☕|咖啡", "🍲|晚餐", "🥛|牛奶", "🍽️|三餐", "🍜|方便面",
                "🍪|零食", "🧁|蛋糕", "🛵|外卖", "🥤|饮料", "🥣|餐饮",
                "🍎|水果", "🥐|早餐", "🥬|蔬菜", "🍞|面包", "🥩|肉类",
                "🍱|午餐", "🍷|酒", "🌙|夜宵", "🚬|烟", "🍢|烧烤", "🍲|火锅");
        addGroup(result, "🩺", "医疗",
                "🩺|就诊", "💊|药品", "🏥|住院", "😷|口罩", "🌿|保健",
                "🧪|抗原试剂", "🧴|消毒用品", "🦷|牙科", "🩻|体检",
                "👓|眼科", "🩹|医疗");
        addGroup(result, "✈️", "旅行",
                "✈️|机票", "🚄|火车票", "🏨|住宿", "🧳|行李", "🗺️|攻略",
                "🎫|门票", "📸|摄影", "🛍️|纪念品", "🚕|当地交通", "🍜|当地美食");
        return result;
    }

    private static List<Group> buildIncomeGroups() {
        List<Group> result = new ArrayList<>();
        addGroup(result, "💰", "工资收入",
                "💰|工资", "🧧|奖金", "📈|提成", "⏱️|加班费", "🎁|补贴",
                "🏠|生活费", "🪙|零花钱");
        addGroup(result, "📈", "投资理财",
                "🏦|利息收入", "🌱|理财", "📊|股票", "📉|基金", "🪙|黄金",
                "💹|分红", "📜|债券", "💱|外汇", "🏛️|公积金");
        addGroup(result, "💼", "经营副业",
                "🧑‍💻|兼职", "✨|外快", "📁|项目收入", "✍️|稿费", "🏘️|租金",
                "🤝|佣金", "🛍️|经营收入");
        addGroup(result, "🌟", "往来其他",
                "🧧|红包", "🧾|报销", "↩️|退款", "🤲|借入", "🤝|借出收回",
                "🎓|奖学金", "🌟|其他收入");
        return result;
    }

    private static void addGroup(List<Group> target, String icon, String label,
                                 String... entries) {
        List<Option> options = new ArrayList<>();
        for (String entry : entries) {
            int separator = entry.indexOf('|');
            if (separator <= 0 || separator >= entry.length() - 1) {
                throw new IllegalArgumentException("Invalid category entry: " + entry);
            }
            String optionIcon = entry.substring(0, separator);
            String name = entry.substring(separator + 1);
            options.add(new Option(optionIcon, label + "/" + name, name, label, false));
        }
        target.add(new Group(icon, label, Collections.unmodifiableList(options)));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Group {
        public final String icon;
        public final String label;
        public final List<Option> options;

        public Group(String icon, String label, List<Option> options) {
            this.icon = icon;
            this.label = label;
            this.options = options;
        }

        @Override
        public String toString() {
            return icon + "  " + label;
        }
    }

    public static final class Option {
        public final String icon;
        public final String label;
        public final String name;
        public final String group;
        public final boolean custom;

        public Option(String icon, String label, boolean custom) {
            this(icon, label, label, label, custom);
        }

        public Option(String icon, String label, String name, String group, boolean custom) {
            this.icon = icon;
            this.label = label;
            this.name = name;
            this.group = group;
            this.custom = custom;
        }

        @Override
        public String toString() {
            return icon + "  " + name;
        }
    }
}
