package com.ledgerbook.lite;

import java.util.List;

/** Converts a stored template into a valid transaction snapshot for current accounts. */
public final class TemplateRules {
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

    private TemplateRules() {
    }

    public static Snapshot instantiate(TemplateRepository.Template template,
                                       List<LedgerDb.Account> accounts,
                                       long occurredAt) {
        if (template == null) throw new IllegalArgumentException("模板不能为空");
        if (accounts == null || accounts.isEmpty()) {
            throw new IllegalArgumentException("当前账本没有可用账户");
        }
        boolean transfer = LedgerDb.TYPE_TRANSFER.equals(template.type);
        if (transfer && accounts.size() < 2) {
            throw new IllegalArgumentException("转账模板至少需要两个账户");
        }

        LedgerDb.Account desiredTarget = find(accounts, template.toAccountId);
        LedgerDb.Account source = find(accounts, template.accountId);
        if (source == null) {
            source = transfer && desiredTarget != null
                    ? firstDifferent(accounts, desiredTarget.id) : accounts.get(0);
        }
        if (source == null) throw new IllegalArgumentException("没有可用转出账户");

        Long targetId = null;
        if (transfer) {
            LedgerDb.Account target = desiredTarget;
            if (target == null || target.id == source.id) {
                target = firstDifferent(accounts, source.id);
            }
            if (target == null) throw new IllegalArgumentException("没有可用转入账户");
            targetId = target.id;
        }

        return new Snapshot(template.type, template.category,
                template.amountCents, template.discountCents,
                source.id, targetId, template.bookkeeper, template.tags,
                template.reimbursable, template.includeBudget,
                template.note, occurredAt);
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
