package com.ledgerbook.lite;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/** Pure recurrence calculations and confirmed-posting snapshots. */
public final class RecurringRules {
    public enum Frequency {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }

    public static final class Snapshot {
        public final String type;
        public final String category;
        public final long amountCents;
        public final long discountCents;
        public final long accountId;
        public final Long toAccountId;
        public final String bookkeeper;
        public final String tags;
        public final boolean reimbursable;
        public final boolean includeBudget;
        public final String note;
        public final long occurredAt;

        Snapshot(String type, String category, long amountCents, long discountCents,
                 long accountId, Long toAccountId, String bookkeeper, String tags,
                 boolean reimbursable, boolean includeBudget, String note,
                 long occurredAt) {
            this.type = type;
            this.category = category;
            this.amountCents = amountCents;
            this.discountCents = discountCents;
            this.accountId = accountId;
            this.toAccountId = toAccountId;
            this.bookkeeper = bookkeeper;
            this.tags = tags;
            this.reimbursable = reimbursable;
            this.includeBudget = includeBudget;
            this.note = note;
            this.occurredAt = occurredAt;
        }
    }

    private RecurringRules() {
    }

    public static LocalDate dueDate(LocalDate start, Frequency frequency,
                                    int interval, int occurrenceIndex) {
        if (start == null) throw new IllegalArgumentException("开始日期不能为空");
        if (frequency == null) throw new IllegalArgumentException("周期类型不能为空");
        if (interval <= 0) throw new IllegalArgumentException("周期间隔必须大于 0");
        if (occurrenceIndex < 0) throw new IllegalArgumentException("周期序号不能为负数");
        long steps = (long) interval * occurrenceIndex;
        switch (frequency) {
            case DAILY:
                return start.plusDays(steps);
            case WEEKLY:
                return start.plusWeeks(steps);
            case MONTHLY: {
                YearMonth target = YearMonth.from(start).plusMonths(steps);
                return target.atDay(Math.min(start.getDayOfMonth(), target.lengthOfMonth()));
            }
            case YEARLY: {
                int targetYear = Math.toIntExact((long) start.getYear() + steps);
                YearMonth target = YearMonth.of(targetYear, start.getMonthValue());
                return target.atDay(Math.min(start.getDayOfMonth(), target.lengthOfMonth()));
            }
            default:
                throw new IllegalArgumentException("不支持的周期类型");
        }
    }

    public static boolean isOccurrenceAllowed(RecurringRepository.Rule rule,
                                              int occurrenceIndex,
                                              LocalDate dueDate) {
        if (rule == null || dueDate == null || occurrenceIndex < 0) return false;
        if (rule.maxOccurrences > 0 && occurrenceIndex >= rule.maxOccurrences) return false;
        return rule.endDate == null || !dueDate.isAfter(rule.endDate);
    }

    public static Snapshot instantiate(RecurringRepository.Rule rule,
                                       List<LedgerDb.Account> accounts,
                                       LocalDate dueDate, ZoneId zone) {
        if (rule == null) throw new IllegalArgumentException("周期规则不能为空");
        if (dueDate == null) throw new IllegalArgumentException("到期日期不能为空");
        if (zone == null) throw new IllegalArgumentException("时区不能为空");
        if (accounts == null || accounts.isEmpty()) {
            throw new IllegalArgumentException("当前账本没有可用账户");
        }
        boolean transfer = LedgerDb.TYPE_TRANSFER.equals(rule.type);
        if (transfer && accounts.size() < 2) {
            throw new IllegalArgumentException("转账周期至少需要两个账户");
        }

        LedgerDb.Account target = find(accounts, rule.toAccountId);
        LedgerDb.Account source = find(accounts, rule.accountId);
        if (source == null) {
            source = transfer && target != null
                    ? firstDifferent(accounts, target.id) : accounts.get(0);
        }
        if (source == null) throw new IllegalArgumentException("没有可用转出账户");

        Long targetId = null;
        if (transfer) {
            if (target == null || target.id == source.id) {
                target = firstDifferent(accounts, source.id);
            }
            if (target == null) throw new IllegalArgumentException("没有可用转入账户");
            targetId = target.id;
        }
        long occurredAt = dueDate.atTime(12, 0).atZone(zone).toInstant().toEpochMilli();
        return new Snapshot(rule.type, rule.category, rule.amountCents,
                rule.discountCents, source.id, targetId, rule.bookkeeper,
                rule.tags, rule.reimbursable, rule.includeBudget,
                rule.note, occurredAt);
    }

    private static LedgerDb.Account find(List<LedgerDb.Account> accounts, Long id) {
        if (id == null) return null;
        for (LedgerDb.Account account : accounts) {
            if (account.id == id) return account;
        }
        return null;
    }

    private static LedgerDb.Account firstDifferent(List<LedgerDb.Account> accounts, long id) {
        for (LedgerDb.Account account : accounts) {
            if (account.id != id) return account;
        }
        return null;
    }
}
