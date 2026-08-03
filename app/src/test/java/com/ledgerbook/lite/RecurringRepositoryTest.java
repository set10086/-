package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class RecurringRepositoryTest {
    @Test
    public void dueMaterializationIsIdempotentAndNeverPostsTransactions() {
        FakeBackend backend = new FakeBackend();
        RecurringRepository repository = new RecurringRepository(backend);
        LedgerDb.Txn source = transaction();
        long ruleId = repository.createFromTransaction(1L, "工作日早餐", source,
                RecurringRules.Frequency.DAILY, 1,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), 0);

        assertEquals(3, repository.materializeDue(1L, LocalDate.of(2026, 8, 3)));
        assertEquals(0, repository.materializeDue(1L, LocalDate.of(2026, 8, 3)));
        List<RecurringRepository.Pending> pending = repository.listPending(1L);
        assertEquals(3, pending.size());
        assertEquals(RecurringRepository.STATUS_PENDING, pending.get(0).status);
        assertEquals(0L, pending.get(0).transactionId);
        assertEquals(ruleId, pending.get(0).ruleId);
    }

    @Test
    public void finiteOccurrenceCountStopsGeneration() {
        FakeBackend backend = new FakeBackend();
        RecurringRepository repository = new RecurringRepository(backend);
        repository.createFromTransaction(1L, "每周零花", transaction(),
                RecurringRules.Frequency.WEEKLY, 1,
                LocalDate.of(2026, 8, 1), null, 2);

        assertEquals(2, repository.materializeDue(1L, LocalDate.of(2026, 9, 30)));
        assertEquals(2, repository.listPending(1L).size());
    }

    @Test
    public void disabledRuleDoesNotGenerateAndCanResume() {
        FakeBackend backend = new FakeBackend();
        RecurringRepository repository = new RecurringRepository(backend);
        long id = repository.createFromTransaction(1L, "房租", transaction(),
                RecurringRules.Frequency.MONTHLY, 1,
                LocalDate.of(2026, 8, 1), null, 0);
        repository.setEnabled(id, false);
        assertEquals(0, repository.materializeDue(1L, LocalDate.of(2026, 8, 1)));
        repository.setEnabled(id, true);
        assertEquals(1, repository.materializeDue(1L, LocalDate.of(2026, 8, 1)));
    }

    @Test
    public void pendingItemsRequireExplicitResolution() {
        FakeBackend backend = new FakeBackend();
        RecurringRepository repository = new RecurringRepository(backend);
        repository.createFromTransaction(1L, "房租", transaction(),
                RecurringRules.Frequency.MONTHLY, 1,
                LocalDate.of(2026, 8, 1), null, 0);
        repository.materializeDue(1L, LocalDate.of(2026, 8, 1));
        RecurringRepository.Pending first = repository.listPending(1L).get(0);

        repository.markPosted(first.id, 88L);
        assertEquals(0, repository.listPending(1L).size());
        assertEquals(RecurringRepository.STATUS_POSTED,
                backend.pending.get(0).status);
        assertEquals(88L, backend.pending.get(0).transactionId);

        repository.createFromTransaction(1L, "保险", transaction(),
                RecurringRules.Frequency.YEARLY, 1,
                LocalDate.of(2026, 8, 2), null, 0);
        repository.materializeDue(1L, LocalDate.of(2026, 8, 2));
        RecurringRepository.Pending second = repository.listPending(1L).get(0);
        repository.skip(second.id);
        assertEquals(RecurringRepository.STATUS_SKIPPED,
                backend.findPending(second.id).status);
    }

    @Test
    public void deletingRuleRemovesItsPendingRowsOnly() {
        FakeBackend backend = new FakeBackend();
        RecurringRepository repository = new RecurringRepository(backend);
        long first = repository.createFromTransaction(1L, "A", transaction(),
                RecurringRules.Frequency.DAILY, 1,
                LocalDate.of(2026, 8, 1), null, 1);
        repository.createFromTransaction(1L, "B", transaction(),
                RecurringRules.Frequency.DAILY, 1,
                LocalDate.of(2026, 8, 1), null, 1);
        repository.materializeDue(1L, LocalDate.of(2026, 8, 1));

        repository.deleteRule(first);
        assertEquals(1, repository.listRules(1L).size());
        assertEquals(1, repository.listPending(1L).size());
    }

    private static LedgerDb.Txn transaction() {
        return new LedgerDb.Txn(7L, LedgerDb.TYPE_EXPENSE,
                "餐饮/早餐", 1800L, 0L, 3L, "微信", null, null,
                "本人", "工作日", false, true, "", 100L);
    }

    private static final class FakeBackend implements RecurringRepository.Backend {
        private final List<RecurringRepository.Rule> rules = new ArrayList<>();
        private final List<RecurringRepository.Pending> pending = new ArrayList<>();
        private long nextRuleId = 1L;
        private long nextPendingId = 1L;

        @Override public List<RecurringRepository.Rule> loadRules(long ledgerId) {
            List<RecurringRepository.Rule> result = new ArrayList<>();
            for (RecurringRepository.Rule rule : rules) {
                if (rule.ledgerId == ledgerId) result.add(rule);
            }
            return result;
        }

        @Override public long insertRule(RecurringRepository.Rule value, long now) {
            RecurringRepository.Rule stored = value.withId(nextRuleId++);
            rules.add(stored);
            return stored.id;
        }

        @Override public void setEnabled(long id, boolean enabled, long now) {
            for (int index = 0; index < rules.size(); index++) {
                RecurringRepository.Rule rule = rules.get(index);
                if (rule.id == id) {
                    rules.set(index, rule.withEnabled(enabled));
                    return;
                }
            }
            throw new IllegalArgumentException("missing rule");
        }

        @Override public void deleteRule(long id) {
            rules.removeIf(value -> value.id == id);
            pending.removeIf(value -> value.ruleId == id);
        }

        @Override public int nextOccurrenceIndex(long ruleId) {
            int max = -1;
            for (RecurringRepository.Pending value : pending) {
                if (value.ruleId == ruleId) max = Math.max(max, value.occurrenceIndex);
            }
            return max + 1;
        }

        @Override public boolean insertPending(long ruleId, int occurrenceIndex,
                                               LocalDate dueDate, long now) {
            for (RecurringRepository.Pending value : pending) {
                if (value.ruleId == ruleId && value.occurrenceIndex == occurrenceIndex) {
                    return false;
                }
            }
            pending.add(new RecurringRepository.Pending(nextPendingId++, ruleId,
                    occurrenceIndex, dueDate, RecurringRepository.STATUS_PENDING, 0L));
            return true;
        }

        @Override public List<RecurringRepository.Pending> loadPending(long ledgerId) {
            List<Long> allowed = new ArrayList<>();
            for (RecurringRepository.Rule rule : rules) {
                if (rule.ledgerId == ledgerId) allowed.add(rule.id);
            }
            List<RecurringRepository.Pending> result = new ArrayList<>();
            for (RecurringRepository.Pending value : pending) {
                if (allowed.contains(value.ruleId)
                        && RecurringRepository.STATUS_PENDING.equals(value.status)) {
                    result.add(value);
                }
            }
            result.sort(java.util.Comparator.comparing(value -> value.dueDate));
            return result;
        }

        @Override public void resolvePending(long id, String status,
                                             long transactionId, long now) {
            for (int index = 0; index < pending.size(); index++) {
                RecurringRepository.Pending value = pending.get(index);
                if (value.id == id) {
                    pending.set(index, new RecurringRepository.Pending(value.id,
                            value.ruleId, value.occurrenceIndex, value.dueDate,
                            status, transactionId));
                    return;
                }
            }
            throw new IllegalArgumentException("missing pending");
        }

        RecurringRepository.Pending findPending(long id) {
            for (RecurringRepository.Pending value : pending) if (value.id == id) return value;
            throw new AssertionError("missing pending");
        }
    }
}
