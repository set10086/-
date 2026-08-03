package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class TemplateRepositoryTest {
    @Test
    public void saveFromTransactionListsAndDeletesTemplates() {
        FakeBackend backend = new FakeBackend();
        TemplateRepository repository = new TemplateRepository(backend);
        LedgerDb.Txn transaction = new LedgerDb.Txn(7L, LedgerDb.TYPE_EXPENSE,
                "餐饮/早餐", 1800L, 0L, 3L, "微信", null, null,
                "本人", "上班", false, true, "", 100L);

        long id = repository.saveFromTransaction(1L, "早餐", transaction);
        List<TemplateRepository.Template> templates = repository.list(1L);

        assertEquals(1L, id);
        assertEquals(1, templates.size());
        assertEquals("早餐", templates.get(0).name);
        assertEquals("餐饮/早餐", templates.get(0).category);
        assertEquals(1800L, templates.get(0).amountCents);

        repository.delete(id);
        assertEquals(0, repository.list(1L).size());
    }

    @Test
    public void listUsesExplicitSortOrderThenName() {
        FakeBackend backend = new FakeBackend();
        backend.values.add(template(1L, "晚餐", 20));
        backend.values.add(template(2L, "午餐", 10));
        backend.values.add(template(3L, "早餐", 10));

        List<TemplateRepository.Template> templates =
                new TemplateRepository(backend).list(1L);

        assertEquals("午餐", templates.get(0).name);
        assertEquals("早餐", templates.get(1).name);
        assertEquals("晚餐", templates.get(2).name);
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankTemplateNameIsRejected() {
        LedgerDb.Txn transaction = new LedgerDb.Txn(1L, LedgerDb.TYPE_EXPENSE,
                "餐饮", 100L, 0L, 1L, "现金", null, null,
                "本人", "", false, true, "", 1L);
        new TemplateRepository(new FakeBackend())
                .saveFromTransaction(1L, "  ", transaction);
    }

    private static TemplateRepository.Template template(long id, String name, int order) {
        return new TemplateRepository.Template(id, 1L, name,
                LedgerDb.TYPE_EXPENSE, "餐饮", 100L, 0L, 1L, null,
                "本人", "", false, true, "", order);
    }

    private static final class FakeBackend implements TemplateRepository.Backend {
        private final List<TemplateRepository.Template> values = new ArrayList<>();
        private long nextId = 1L;

        @Override public List<TemplateRepository.Template> load(long ledgerId) {
            List<TemplateRepository.Template> result = new ArrayList<>();
            for (TemplateRepository.Template value : values) {
                if (value.ledgerId == ledgerId) result.add(value);
            }
            return result;
        }

        @Override public long insert(TemplateRepository.Template value, long now) {
            TemplateRepository.Template stored = new TemplateRepository.Template(
                    nextId++, value.ledgerId, value.name, value.type, value.category,
                    value.amountCents, value.discountCents, value.accountId,
                    value.toAccountId, value.bookkeeper, value.tags,
                    value.reimbursable, value.includeBudget, value.note, value.sortOrder);
            values.add(stored);
            return stored.id;
        }

        @Override public void delete(long id) {
            values.removeIf(value -> value.id == id);
        }
    }
}
