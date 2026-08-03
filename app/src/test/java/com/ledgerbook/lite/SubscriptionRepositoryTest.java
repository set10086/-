package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class SubscriptionRepositoryTest {
    @Test
    public void createFromExpenseCalculatesTotalsAndReminders() {
        FakeBackend backend = new FakeBackend();
        SubscriptionRepository repository = new SubscriptionRepository(backend);
        LedgerDb.Txn source = expense(3_000L, 4L);

        long id = repository.createFromTransaction(1L, "音乐会员", source,
                SubscriptionRules.Frequency.MONTHLY, 1,
                LocalDate.of(2026, 8, 8), "家庭套餐");
        SubscriptionRepository.Snapshot snapshot = repository.snapshot(
                1L, LocalDate.of(2026, 8, 3), 7);

        assertEquals(1L, id);
        assertEquals(1, snapshot.values.size());
        assertEquals(3_000L, snapshot.monthlyCents);
        assertEquals(36_000L, snapshot.annualCents);
        assertEquals(1, snapshot.upcomingCount);
        assertEquals(0, snapshot.dueCount);
    }

    @Test
    public void postingAdvancePreservesAnchorAndPauseStopsReminderTotals() {
        FakeBackend backend = new FakeBackend();
        SubscriptionRepository repository = new SubscriptionRepository(backend);
        long id = repository.createFromTransaction(1L, "云盘", expense(1_000L, 3L),
                SubscriptionRules.Frequency.MONTHLY, 1,
                LocalDate.of(2026, 1, 31), "");

        repository.advanceAfterPosting(id);
        assertEquals(LocalDate.of(2026, 2, 28), backend.find(id).nextChargeDate);
        repository.advanceAfterPosting(id);
        assertEquals(LocalDate.of(2026, 3, 31), backend.find(id).nextChargeDate);

        repository.setActive(id, false);
        SubscriptionRepository.Snapshot paused = repository.snapshot(
                1L, LocalDate.of(2026, 3, 1), 30);
        assertEquals(0L, paused.monthlyCents);
        assertEquals(0, paused.upcomingCount);
        assertFalse(backend.find(id).active);
    }

    @Test
    public void deleteRemovesOnlyRequestedSubscription() {
        FakeBackend backend = new FakeBackend();
        SubscriptionRepository repository = new SubscriptionRepository(backend);
        long first = repository.createFromTransaction(1L, "A", expense(100L, 1L),
                SubscriptionRules.Frequency.MONTHLY, 1,
                LocalDate.of(2026, 8, 5), "");
        repository.createFromTransaction(1L, "B", expense(200L, 1L),
                SubscriptionRules.Frequency.YEARLY, 1,
                LocalDate.of(2026, 9, 1), "");

        repository.delete(first);
        assertEquals(1, repository.list(1L).size());
        assertEquals("B", repository.list(1L).get(0).name);
    }

    @Test(expected = IllegalArgumentException.class)
    public void incomeCannotBecomeSubscription() {
        LedgerDb.Txn income = new LedgerDb.Txn(1L, LedgerDb.TYPE_INCOME,
                "职业收入/工资", 100L, 0L, 1L, "卡", null, null,
                "本人", "", false, true, "", 1L);
        new SubscriptionRepository(new FakeBackend()).createFromTransaction(
                1L, "工资", income, SubscriptionRules.Frequency.MONTHLY,
                1, LocalDate.now(), "");
    }

    private static LedgerDb.Txn expense(long amount, long accountId) {
        return new LedgerDb.Txn(1L, LedgerDb.TYPE_EXPENSE,
                "娱乐/会员", amount, 0L, accountId, "微信", null, null,
                "本人", "订阅", false, true, "", 1L);
    }

    private static final class FakeBackend implements SubscriptionRepository.Backend {
        private final List<SubscriptionRepository.Subscription> values = new ArrayList<>();
        private long nextId = 1L;

        @Override public List<SubscriptionRepository.Subscription> load(long ledgerId) {
            List<SubscriptionRepository.Subscription> result = new ArrayList<>();
            for (SubscriptionRepository.Subscription value : values) {
                if (value.ledgerId == ledgerId) result.add(value);
            }
            return result;
        }

        @Override public long insert(SubscriptionRepository.Subscription value, long now) {
            SubscriptionRepository.Subscription stored = value.withId(nextId++);
            values.add(stored);
            return stored.id;
        }

        @Override public void setActive(long id, boolean active, long now) {
            for (int index = 0; index < values.size(); index++) {
                if (values.get(index).id == id) {
                    values.set(index, values.get(index).withActive(active));
                    return;
                }
            }
            throw new IllegalArgumentException("missing subscription");
        }

        @Override public void setNextCharge(long id, LocalDate nextCharge, long now) {
            for (int index = 0; index < values.size(); index++) {
                if (values.get(index).id == id) {
                    values.set(index, values.get(index).withNextCharge(nextCharge));
                    return;
                }
            }
            throw new IllegalArgumentException("missing subscription");
        }

        @Override public void delete(long id) {
            values.removeIf(value -> value.id == id);
        }

        SubscriptionRepository.Subscription find(long id) {
            for (SubscriptionRepository.Subscription value : values) {
                if (value.id == id) return value;
            }
            throw new AssertionError("missing subscription");
        }
    }
}
