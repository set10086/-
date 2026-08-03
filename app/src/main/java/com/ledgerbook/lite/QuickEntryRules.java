package com.ledgerbook.lite;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure fast-entry rules shared by the Android dialog and JVM tests. */
public final class QuickEntryRules {
    public static final class CategoryChoice {
        public final String icon;
        public final String label;

        public CategoryChoice(String icon, String label) {
            this.icon = icon == null ? "" : icon;
            this.label = label == null ? "" : label;
        }
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
                 boolean reimbursable, boolean includeBudget, String note, long occurredAt) {
            this.type = type;
            this.category = category;
            this.amountCents = amountCents;
            this.discountCents = discountCents;
            this.accountId = accountId;
            this.toAccountId = toAccountId;
            this.bookkeeper = safe(bookkeeper, "本人");
            this.tags = safe(tags, "");
            this.reimbursable = reimbursable;
            this.includeBudget = includeBudget;
            this.note = safe(note, "");
            this.occurredAt = occurredAt;
        }
    }

    private QuickEntryRules() {
    }

    public static List<CategoryChoice> recentCategories(List<LedgerDb.Txn> rows,
                                                         String type, int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        String requestedType = safe(type, LedgerDb.TYPE_EXPENSE);
        List<CategoryChoice> result = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();
        if (rows == null) return result;
        for (LedgerDb.Txn row : rows) {
            if (row == null || !requestedType.equals(row.type)) continue;
            String category = safe(row.category, "").trim();
            if (category.isEmpty() || !used.add(category)) continue;
            result.add(new CategoryChoice(InputCatalog.iconFor(requestedType, category), category));
            if (result.size() >= limit) break;
        }
        return result;
    }

    public static Snapshot copyOf(LedgerDb.Txn source, long occurredAt) {
        if (source == null) throw new IllegalArgumentException("source cannot be null");
        return new Snapshot(source.type, source.category, source.amountCents,
                source.discountCents, source.accountId, source.toAccountId,
                source.bookkeeper, source.tags, source.reimbursable,
                source.includeBudget, source.note, occurredAt);
    }

    public static LedgerDb.Account preferredAccount(List<LedgerDb.Account> accounts,
                                                     long preferredId) {
        if (accounts == null || accounts.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个账户");
        }
        for (LedgerDb.Account account : accounts) {
            if (account.id == preferredId) return account;
        }
        return accounts.get(0);
    }

    private static String safe(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
