package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class RecurringRulesTest {
    @Test
    public void monthlyScheduleKeepsOriginalEndOfMonthAnchor() {
        LocalDate start = LocalDate.of(2026, 1, 31);
        assertEquals(LocalDate.of(2026, 1, 31),
                RecurringRules.dueDate(start, RecurringRules.Frequency.MONTHLY, 1, 0));
        assertEquals(LocalDate.of(2026, 2, 28),
                RecurringRules.dueDate(start, RecurringRules.Frequency.MONTHLY, 1, 1));
        assertEquals(LocalDate.of(2026, 3, 31),
                RecurringRules.dueDate(start, RecurringRules.Frequency.MONTHLY, 1, 2));
        assertEquals(LocalDate.of(2026, 4, 30),
                RecurringRules.dueDate(start, RecurringRules.Frequency.MONTHLY, 1, 3));
    }

    @Test
    public void yearlyLeapDayUsesLastValidDayWithoutDrifting() {
        LocalDate start = LocalDate.of(2024, 2, 29);
        assertEquals(LocalDate.of(2025, 2, 28),
                RecurringRules.dueDate(start, RecurringRules.Frequency.YEARLY, 1, 1));
        assertEquals(LocalDate.of(2028, 2, 29),
                RecurringRules.dueDate(start, RecurringRules.Frequency.YEARLY, 1, 4));
    }

    @Test
    public void dailyAndWeeklyIntervalsAreCalculatedFromStart() {
        LocalDate start = LocalDate.of(2026, 8, 3);
        assertEquals(LocalDate.of(2026, 8, 9),
                RecurringRules.dueDate(start, RecurringRules.Frequency.DAILY, 2, 3));
        assertEquals(LocalDate.of(2026, 8, 31),
                RecurringRules.dueDate(start, RecurringRules.Frequency.WEEKLY, 2, 2));
    }

    @Test
    public void postingSnapshotUsesDueDateNoonAndValidAccountFallback() {
        RecurringRepository.Rule rule = new RecurringRepository.Rule(
                1L, 7L, "月度储蓄", LedgerDb.TYPE_TRANSFER, "账户转账",
                50_000L, 0L, 99L, 4L, "本人", "储蓄", false, true, "",
                RecurringRules.Frequency.MONTHLY, 1,
                LocalDate.of(2026, 8, 5), null, 0, true, 0);
        List<LedgerDb.Account> accounts = Arrays.asList(
                account(4L, "储蓄卡"), account(5L, "现金"));

        RecurringRules.Snapshot snapshot = RecurringRules.instantiate(
                rule, accounts, LocalDate.of(2026, 8, 5), ZoneId.of("Asia/Shanghai"));

        assertEquals(5L, snapshot.accountId);
        assertEquals(Long.valueOf(4L), snapshot.toAccountId);
        assertEquals(LocalDate.of(2026, 8, 5),
                java.time.Instant.ofEpochMilli(snapshot.occurredAt)
                        .atZone(ZoneId.of("Asia/Shanghai")).toLocalDate());
        assertEquals(12, java.time.Instant.ofEpochMilli(snapshot.occurredAt)
                .atZone(ZoneId.of("Asia/Shanghai")).getHour());
    }

    @Test(expected = IllegalArgumentException.class)
    public void intervalMustBePositive() {
        RecurringRules.dueDate(LocalDate.now(), RecurringRules.Frequency.DAILY, 0, 1);
    }

    private static LedgerDb.Account account(long id, String name) {
        return new LedgerDb.Account(id, 7L, name, "现金账户", "常用", 0L, "");
    }
}
