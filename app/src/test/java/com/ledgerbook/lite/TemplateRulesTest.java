package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class TemplateRulesTest {
    @Test
    public void templateCreatesCurrentTimeSnapshotWithStoredFields() {
        TemplateRepository.Template template = new TemplateRepository.Template(
                8L, 1L, "工作日午餐", LedgerDb.TYPE_EXPENSE, "餐饮/午餐",
                3200L, 200L, 4L, null, "本人", "工作日", false, true,
                "公司附近", 0);
        List<LedgerDb.Account> accounts = Arrays.asList(
                account(4L, "微信"), account(5L, "现金"));

        TemplateRules.Snapshot result = TemplateRules.instantiate(
                template, accounts, 99_000L);

        assertEquals(LedgerDb.TYPE_EXPENSE, result.type);
        assertEquals("餐饮/午餐", result.category);
        assertEquals(3200L, result.amountCents);
        assertEquals(200L, result.discountCents);
        assertEquals(4L, result.accountId);
        assertNull(result.toAccountId);
        assertEquals("本人", result.bookkeeper);
        assertEquals("工作日", result.tags);
        assertEquals("公司附近", result.note);
        assertEquals(99_000L, result.occurredAt);
    }

    @Test
    public void missingSourceAccountFallsBackAndTransferTargetStaysDifferent() {
        TemplateRepository.Template template = new TemplateRepository.Template(
                9L, 1L, "转入储蓄", LedgerDb.TYPE_TRANSFER, "账户转账",
                50_000L, 0L, 99L, 3L, "本人", "", false, true, "", 0);
        List<LedgerDb.Account> accounts = Arrays.asList(
                account(3L, "储蓄卡"), account(4L, "现金"));

        TemplateRules.Snapshot result = TemplateRules.instantiate(
                template, accounts, 123L);

        assertEquals(4L, result.accountId);
        assertEquals(Long.valueOf(3L), result.toAccountId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transferNeedsTwoAccounts() {
        TemplateRepository.Template template = new TemplateRepository.Template(
                1L, 1L, "转账", LedgerDb.TYPE_TRANSFER, "账户转账",
                100L, 0L, 1L, 2L, "本人", "", false, true, "", 0);
        TemplateRules.instantiate(template,
                java.util.Collections.singletonList(account(1L, "现金")), 1L);
    }

    private static LedgerDb.Account account(long id, String name) {
        return new LedgerDb.Account(id, 1L, name, "现金账户", "常用", 0L, "");
    }
}
