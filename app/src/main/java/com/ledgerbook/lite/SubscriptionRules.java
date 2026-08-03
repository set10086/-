package com.ledgerbook.lite;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/** Pure subscription normalization, reminder, renewal, and posting rules. */
public final class SubscriptionRules {
    public enum Frequency { WEEKLY, MONTHLY, YEARLY }
    public enum Reminder { DUE, UPCOMING, LATER, PAUSED }

    public static final class Cost {
        public final long monthlyCents;
        public final long annualCents;
        Cost(long monthlyCents, long annualCents) {
            this.monthlyCents = monthlyCents; this.annualCents = annualCents;
        }
    }

    public static final class Snapshot {
        public final String category;
        public final long amountCents;
        public final long accountId;
        public final String note;
        public final long occurredAt;
        Snapshot(String category, long amountCents, long accountId, String note, long occurredAt) {
            this.category = category; this.amountCents = amountCents;
            this.accountId = accountId; this.note = note; this.occurredAt = occurredAt;
        }
    }

    private SubscriptionRules() {}

    public static Cost cost(long amountCents, Frequency frequency, int interval) {
        if (amountCents <= 0L) throw new IllegalArgumentException("订阅金额必须大于 0");
        if (frequency == null) throw new IllegalArgumentException("订阅周期不能为空");
        if (interval <= 0) throw new IllegalArgumentException("订阅间隔必须大于 0");
        long annual;
        switch (frequency) {
            case WEEKLY: annual = safeMultiply(amountCents, 52L) / interval; break;
            case MONTHLY: annual = safeMultiply(amountCents, 12L) / interval; break;
            default: annual = amountCents / interval; break;
        }
        return new Cost(annual / 12L, annual);
    }

    public static LocalDate nextCharge(SubscriptionRepository.Subscription value) {
        if (value == null) throw new IllegalArgumentException("订阅不能为空");
        LocalDate current = value.nextChargeDate;
        if (value.frequency == Frequency.WEEKLY) return current.plusWeeks(value.interval);
        if (value.frequency == Frequency.MONTHLY) {
            YearMonth target = YearMonth.from(current).plusMonths(value.interval);
            return target.atDay(Math.min(value.anchorDay, target.lengthOfMonth()));
        }
        int year = Math.addExact(current.getYear(), value.interval);
        YearMonth target = YearMonth.of(year, value.anchorMonth);
        return target.atDay(Math.min(value.anchorDay, target.lengthOfMonth()));
    }

    public static Reminder reminder(LocalDate nextCharge, boolean active,
                                    LocalDate today, int upcomingDays) {
        if (!active) return Reminder.PAUSED;
        if (nextCharge == null || today == null) throw new IllegalArgumentException("日期不能为空");
        if (upcomingDays < 0) throw new IllegalArgumentException("提醒天数不能为负数");
        if (!nextCharge.isAfter(today)) return Reminder.DUE;
        return !nextCharge.isAfter(today.plusDays(upcomingDays)) ? Reminder.UPCOMING : Reminder.LATER;
    }

    public static Snapshot instantiate(SubscriptionRepository.Subscription value,
                                       List<LedgerDb.Account> accounts, ZoneId zone) {
        if (value == null) throw new IllegalArgumentException("订阅不能为空");
        if (accounts == null || accounts.isEmpty()) throw new IllegalArgumentException("当前账本没有可用账户");
        if (zone == null) throw new IllegalArgumentException("时区不能为空");
        LedgerDb.Account account = null;
        for (LedgerDb.Account candidate : accounts) if (candidate.id == value.accountId) account = candidate;
        if (account == null) account = accounts.get(0);
        long occurredAt = value.nextChargeDate.atTime(12, 0).atZone(zone).toInstant().toEpochMilli();
        return new Snapshot(value.category, value.amountCents, account.id, value.note, occurredAt);
    }

    private static long safeMultiply(long value, long multiplier) {
        try { return Math.multiplyExact(value, multiplier); }
        catch (ArithmeticException error) { return Long.MAX_VALUE; }
    }
}
