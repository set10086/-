package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class SubscriptionRulesTest {
    @Test
    public void monthlyAndAnnualEquivalentCostsAreNormalized() {
        SubscriptionRules.Cost monthly = SubscriptionRules.cost(
                3_000L, SubscriptionRules.Frequency.MONTHLY, 1);
        assertEquals(3_000L, monthly.monthlyCents);
        assertEquals(36_000L, monthly.annualCents);

        SubscriptionRules.Cost annual = SubscriptionRules.cost(
                120_000L, SubscriptionRules.Frequency.YEARLY, 1);
        assertEquals(10_000L, annual.monthlyCents);
        assertEquals(120_000L, annual.annualCents);

        SubscriptionRules.Cost weekly = SubscriptionRules.cost(
                1_000L, SubscriptionRules.Frequency.WEEKLY, 1);
        assertEquals(52_000L, weekly.annualCents);
        assertEquals(4_333L, weekly.monthlyCents);
    }

    @Test
    public void renewalUsesStoredAnchorAndDoesNotDriftAfterShortMonth() {
        SubscriptionRepository.Subscription value = subscription(
                LocalDate.of(2026, 1, 31), 1, 31,
                SubscriptionRules.Frequency.MONTHLY, 1);
        assertEquals(LocalDate.of(2026, 2, 28),
                SubscriptionRules.nextCharge(value));

        SubscriptionRepository.Subscription february = value.withNextCharge(
                LocalDate.of(2026, 2, 28));
        assertEquals(LocalDate.of(2026, 3, 31),
                SubscriptionRules.nextCharge(february));
    }

    @Test
    public void reminderStateSeparatesDueUpcomingLaterAndPaused() {
        LocalDate today = LocalDate.of(2026, 8, 3);
        assertEquals(SubscriptionRules.Reminder.DUE,
                SubscriptionRules.reminder(today.minusDays(1), true, today, 7));
        assertEquals(SubscriptionRules.Reminder.UPCOMING,
                SubscriptionRules.reminder(today.plusDays(7), true, today, 7));
        assertEquals(SubscriptionRules.Reminder.LATER,
                SubscriptionRules.reminder(today.plusDays(8), true, today, 7));
        assertEquals(SubscriptionRules.Reminder.PAUSED,
                SubscriptionRules.reminder(today, false, today, 7));
    }

    @Test
    public void postingSnapshotUsesValidAccountAndChargeDateNoon() {
        SubscriptionRepository.Subscription value = new SubscriptionRepository.Subscription(
                1L, 2L, "视频会员", 2_500L, "娱乐/影视会员", 99L,
                SubscriptionRules.Frequency.MONTHLY, 1,
                LocalDate.of(2026, 8, 5), 8, 5, true, "自动续费", 0);
        List<LedgerDb.Account> accounts = Arrays.asList(
                account(3L, "微信"), account(4L, "现金"));

        SubscriptionRules.Snapshot snapshot = SubscriptionRules.instantiate(
                value, accounts, ZoneId.of("Asia/Shanghai"));

        assertEquals(3L, snapshot.accountId);
        assertEquals("娱乐/影视会员", snapshot.category);
        assertEquals(2_500L, snapshot.amountCents);
        assertEquals(LocalDate.of(2026, 8, 5),
                java.time.Instant.ofEpochMilli(snapshot.occurredAt)
                        .atZone(ZoneId.of("Asia/Shanghai")).toLocalDate());
        assertEquals(12, java.time.Instant.ofEpochMilli(snapshot.occurredAt)
                .atZone(ZoneId.of("Asia/Shanghai")).getHour());
    }

    private static SubscriptionRepository.Subscription subscription(
            LocalDate next, int anchorMonth, int anchorDay,
            SubscriptionRules.Frequency frequency, int interval) {
        return new SubscriptionRepository.Subscription(1L, 1L, "服务", 1_000L,
                "娱乐/会员", 1L, frequency, interval, next,
                anchorMonth, anchorDay, true, "", 0);
    }

    private static LedgerDb.Account account(long id, String name) {
        return new LedgerDb.Account(id, 2L, name, "支付账户", "常用", 0L, "");
    }
}
