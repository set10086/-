package com.ledgerbook.lite;

import java.util.Locale;

/** Pure semantic description used by the Android crayon icon renderer. */
public final class CrayonIconSpec {
    public final String family;
    public final String mark;

    private CrayonIconSpec(String family, String mark) {
        this.family = family;
        this.mark = mark;
    }

    public static CrayonIconSpec forCategory(String transactionType, String category) {
        String value = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        if (LedgerDb.TYPE_TRANSFER.equals(transactionType)) return new CrayonIconSpec("transfer", "↔");
        if (LedgerDb.TYPE_INCOME.equals(transactionType)) {
            if (has(value, "工资", "薪", "退休", "养老金", "津贴")) return new CrayonIconSpec("income-salary", "¥");
            if (has(value, "奖金", "红包", "礼金")) return new CrayonIconSpec("income-gift", "+");
            if (has(value, "理财", "利息", "投资", "分红", "基金", "股票")) return new CrayonIconSpec("income-finance", "%");
            if (has(value, "退款", "报销", "赔偿", "补贴")) return new CrayonIconSpec("income-refund", "↩");
            return new CrayonIconSpec("income-other", "+");
        }
        if (has(value, "餐饮", "早餐", "午餐", "晚餐", "零食", "饮料", "咖啡", "水果", "蔬菜", "外卖", "酒水", "食品"))
            return new CrayonIconSpec("food", "饭");
        if (has(value, "交通", "公交", "地铁", "打车", "出租", "车", "油", "停车", "高速", "机票", "火车", "船"))
            return new CrayonIconSpec("transport", "车");
        if (has(value, "住房", "房租", "房贷", "物业", "居住", "家具", "家电", "厨房", "浴室"))
            return new CrayonIconSpec("home", "家");
        if (has(value, "医疗", "药", "医院", "体检", "牙", "眼科", "医美", "护理"))
            return new CrayonIconSpec("medical", "+");
        if (has(value, "教育", "学习", "学费", "书", "培训", "考试", "学校", "文具"))
            return new CrayonIconSpec("education", "学");
        if (has(value, "美妆", "口红", "面膜", "香水", "护肤", "美容", "化妆", "美甲", "乳液", "面霜"))
            return new CrayonIconSpec("beauty", "美");
        if (has(value, "家人", "父母", "老人", "孩子", "宝宝", "亲友", "恋爱", "伴侣"))
            return new CrayonIconSpec("family", "爱");
        if (has(value, "购物", "网购", "日用", "超市", "衣", "鞋", "包", "饰品", "珠宝"))
            return new CrayonIconSpec("shopping", "购");
        if (has(value, "理财", "金融", "保险", "社保", "公积金", "税", "债", "还款", "借款", "贷款"))
            return new CrayonIconSpec("finance", "¥");
        if (has(value, "旅行", "旅游", "酒店", "住宿", "出差", "景点", "签证", "行李"))
            return new CrayonIconSpec("travel", "行");
        if (has(value, "娱乐", "电影", "游戏", "唱歌", "演出", "彩票", "会员", "直播"))
            return new CrayonIconSpec("entertainment", "玩");
        if (has(value, "宠物", "猫", "狗", "水族", "兽医"))
            return new CrayonIconSpec("pet", "宠");
        if (has(value, "运动", "健身", "球", "跑步", "游泳", "瑜伽", "骑行"))
            return new CrayonIconSpec("sport", "动");
        if (has(value, "园艺", "花", "绿植", "盆栽", "多肉", "园林"))
            return new CrayonIconSpec("garden", "叶");
        if (has(value, "装修", "油漆", "地板", "水路", "电路", "五金", "工具", "维修", "人工费", "建材"))
            return new CrayonIconSpec("tools", "修");
        if (has(value, "数码", "手机", "电脑", "相机", "耳机", "平板", "通讯", "宽带", "网络"))
            return new CrayonIconSpec("digital", "机");
        if (has(value, "办公", "工作", "打印", "耗材", "文件", "会议"))
            return new CrayonIconSpec("office", "办");
        if (has(value, "社交", "朋友", "人情", "礼物", "聚会", "请客", "捐赠", "党费", "团费"))
            return new CrayonIconSpec("social", "友");
        if (has(value, "水费", "电费", "燃气", "话费", "天气", "供暖", "物业费"))
            return new CrayonIconSpec("utilities", "用");
        if (has(value, "服装", "上衣", "裤", "裙", "内衣", "帽", "鞋袜"))
            return new CrayonIconSpec("clothing", "衣");
        return new CrayonIconSpec("other", firstMark(category));
    }

    private static boolean has(String value, String... keys) {
        for (String key : keys) if (value.contains(key)) return true;
        return false;
    }

    private static String firstMark(String category) {
        if (category == null || category.trim().isEmpty()) return "记";
        String clean = category.trim();
        int slash = Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('／'));
        if (slash >= 0 && slash + 1 < clean.length()) clean = clean.substring(slash + 1);
        int cp = clean.codePointAt(0);
        return new String(Character.toChars(cp));
    }
}
