package com.ledgerbook.lite;

/** Pure monthly budget calculations. */
public final class BudgetRules {
    public enum Level {
        NONE,
        SAFE,
        WARNING,
        OVER
    }

    public static final class Status {
        public final long limitCents;
        public final long spentCents;
        public final long remainingCents;
        public final long overCents;
        public final int progressPercent;
        public final int remainingDays;
        public final long dailyAvailableCents;
        public final Level level;

        Status(long limitCents, long spentCents, long remainingCents, long overCents,
               int progressPercent, int remainingDays, long dailyAvailableCents,
               Level level) {
            this.limitCents = limitCents;
            this.spentCents = spentCents;
            this.remainingCents = remainingCents;
            this.overCents = overCents;
            this.progressPercent = progressPercent;
            this.remainingDays = remainingDays;
            this.dailyAvailableCents = dailyAvailableCents;
            this.level = level;
        }
    }

    private BudgetRules() {
    }

    public static Status calculate(long limitCents, long spentCents,
                                   int dayOfMonth, int daysInMonth) {
        long safeLimit = Math.max(0L, limitCents);
        long safeSpent = Math.max(0L, spentCents);
        int safeDays = Math.max(1, daysInMonth);
        int safeDay = Math.max(1, Math.min(dayOfMonth, safeDays));
        int remainingDays = safeDays - safeDay + 1;
        if (safeLimit == 0L) {
            return new Status(0L, safeSpent, 0L, 0L,
                    0, remainingDays, 0L, Level.NONE);
        }
        long remaining = Math.max(0L, safeLimit - safeSpent);
        long over = Math.max(0L, safeSpent - safeLimit);
        long rawPercent = safeSpent > Long.MAX_VALUE / 100L
                ? Integer.MAX_VALUE : safeSpent * 100L / safeLimit;
        int progress = rawPercent > Integer.MAX_VALUE
                ? Integer.MAX_VALUE : (int) rawPercent;
        Level level = safeSpent >= safeLimit ? Level.OVER
                : safeSpent * 100L >= safeLimit * 80L ? Level.WARNING : Level.SAFE;
        long daily = remainingDays <= 0 ? 0L : remaining / remainingDays;
        return new Status(safeLimit, safeSpent, remaining, over,
                progress, remainingDays, daily, level);
    }

    /** A first-level key matches itself and descendants; a leaf key is exact. */
    public static boolean matchesCategory(String budgetKey, String transactionCategory) {
        String key = clean(budgetKey);
        String category = clean(transactionCategory);
        if (key.isEmpty() || category.isEmpty()) return false;
        if (key.indexOf('/') >= 0) return key.equals(category);
        return key.equals(category) || category.startsWith(key + "/");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
