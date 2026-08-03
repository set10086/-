package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class BudgetRepositoryTest {
    @Test
    public void repositoryUpsertsAndCalculatesMonthSnapshot() {
        FakeBackend backend = new FakeBackend();
        BudgetRepository repository = new BudgetRepository(backend);

        repository.setTotalBudget(1L, "2026-08", 300_000L);
        repository.setCategoryBudget(1L, "2026-08", "餐饮", 100_000L);
        repository.setCategoryBudget(1L, "2026-08", "交通/地铁", 30_000L);
        backend.spentTotal = 180_000L;
        backend.spentByCategory.put("餐饮", 82_000L);
        backend.spentByCategory.put("交通/地铁", 12_000L);

        BudgetRepository.MonthSnapshot snapshot = repository.snapshot(
                1L, "2026-08", 20, 31);

        assertEquals(300_000L, snapshot.total.budget.amountCents);
        assertEquals(180_000L, snapshot.total.status.spentCents);
        assertEquals(BudgetRules.Level.SAFE, snapshot.total.status.level);
        assertEquals(2, snapshot.categories.size());
        assertEquals("餐饮", snapshot.categories.get(0).budget.categoryKey);
        assertEquals(BudgetRules.Level.WARNING, snapshot.categories.get(0).status.level);
        assertEquals("交通/地铁", snapshot.categories.get(1).budget.categoryKey);
    }

    @Test
    public void deletingBudgetRemovesOnlyRequestedScope() {
        FakeBackend backend = new FakeBackend();
        BudgetRepository repository = new BudgetRepository(backend);
        repository.setTotalBudget(2L, "2026-09", 200_000L);
        repository.setCategoryBudget(2L, "2026-09", "住房", 80_000L);

        repository.removeCategoryBudget(2L, "2026-09", "住房");
        BudgetRepository.MonthSnapshot snapshot = repository.snapshot(
                2L, "2026-09", 1, 30);
        assertEquals(200_000L, snapshot.total.budget.amountCents);
        assertEquals(0, snapshot.categories.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidMonthKeyIsRejected() {
        new BudgetRepository(new FakeBackend()).setTotalBudget(1L, "2026-13", 1L);
    }

    private static final class FakeBackend implements BudgetRepository.Backend {
        private final List<BudgetRepository.Budget> budgets = new ArrayList<>();
        private final java.util.Map<String, Long> spentByCategory = new java.util.HashMap<>();
        private long spentTotal;
        private long nextId = 1L;

        @Override public List<BudgetRepository.Budget> load(long ledgerId, String monthKey) {
            List<BudgetRepository.Budget> result = new ArrayList<>();
            for (BudgetRepository.Budget budget : budgets) {
                if (budget.ledgerId == ledgerId && budget.monthKey.equals(monthKey)) {
                    result.add(budget);
                }
            }
            result.sort(java.util.Comparator.comparing(value -> value.categoryKey));
            return result;
        }

        @Override public void upsert(long ledgerId, String monthKey, String categoryKey,
                                     long amountCents, long now) {
            for (int index = 0; index < budgets.size(); index++) {
                BudgetRepository.Budget budget = budgets.get(index);
                if (budget.ledgerId == ledgerId && budget.monthKey.equals(monthKey)
                        && budget.categoryKey.equals(categoryKey)) {
                    budgets.set(index, new BudgetRepository.Budget(budget.id, ledgerId,
                            monthKey, categoryKey, amountCents));
                    return;
                }
            }
            budgets.add(new BudgetRepository.Budget(nextId++, ledgerId,
                    monthKey, categoryKey, amountCents));
        }

        @Override public void remove(long ledgerId, String monthKey, String categoryKey) {
            budgets.removeIf(value -> value.ledgerId == ledgerId
                    && value.monthKey.equals(monthKey)
                    && value.categoryKey.equals(categoryKey));
        }

        @Override public long spent(long ledgerId, String monthKey, String categoryKey) {
            return categoryKey.isEmpty() ? spentTotal
                    : spentByCategory.getOrDefault(categoryKey, 0L);
        }
    }
}
