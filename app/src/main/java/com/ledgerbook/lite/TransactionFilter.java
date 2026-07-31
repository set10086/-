package com.ledgerbook.lite;

import java.util.Objects;

public final class TransactionFilter {
    public static final long ALL_LEDGERS = -1L;

    public final long ledgerId;
    public final long fromInclusive;
    public final long toExclusive;
    public final String type;
    public final String category;
    public final long accountId;
    public final String bookkeeper;

    public TransactionFilter(long ledgerId, long fromInclusive, long toExclusive,
                             String type, String category, long accountId, String bookkeeper) {
        if (ledgerId != ALL_LEDGERS && ledgerId <= 0L) {
            throw new IllegalArgumentException("账本范围无效");
        }
        if (fromInclusive >= toExclusive) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        if (accountId < 0L) {
            throw new IllegalArgumentException("账户范围无效");
        }
        this.ledgerId = ledgerId;
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
        this.type = normalize(type);
        this.category = normalize(category);
        this.accountId = accountId;
        this.bookkeeper = normalize(bookkeeper);
    }

    public static TransactionFilter forCurrentLedger(long ledgerId, long fromInclusive, long toExclusive) {
        return new TransactionFilter(ledgerId, fromInclusive, toExclusive,
                null, null, 0L, null);
    }

    public static TransactionFilter forAllLedgers(long fromInclusive, long toExclusive) {
        return new TransactionFilter(ALL_LEDGERS, fromInclusive, toExclusive,
                null, null, 0L, null);
    }

    public TransactionFilter withLedger(long value) {
        return new TransactionFilter(value, fromInclusive, toExclusive,
                type, category, accountId, bookkeeper);
    }

    public TransactionFilter withRange(long from, long to) {
        return new TransactionFilter(ledgerId, from, to,
                type, category, accountId, bookkeeper);
    }

    public TransactionFilter withDetails(String valueType, String valueCategory,
                                         long valueAccountId, String valueBookkeeper) {
        return new TransactionFilter(ledgerId, fromInclusive, toExclusive,
                valueType, valueCategory, valueAccountId, valueBookkeeper);
    }

    public boolean hasDetailFilters() {
        return type != null || category != null || accountId > 0L || bookkeeper != null;
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String clean = value.trim();
        return clean.isEmpty() ? null : clean;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TransactionFilter)) return false;
        TransactionFilter that = (TransactionFilter) other;
        return ledgerId == that.ledgerId
                && fromInclusive == that.fromInclusive
                && toExclusive == that.toExclusive
                && accountId == that.accountId
                && Objects.equals(type, that.type)
                && Objects.equals(category, that.category)
                && Objects.equals(bookkeeper, that.bookkeeper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ledgerId, fromInclusive, toExclusive,
                type, category, accountId, bookkeeper);
    }
}
