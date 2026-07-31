package com.ledgerbook.lite;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public final class CartoonStyleTest {
    @Test
    public void transactionTypesHaveStableStickerIcons() {
        assertEquals("🍜", CartoonStyle.transactionIcon(LedgerDb.TYPE_EXPENSE, "餐饮"));
        assertEquals("💰", CartoonStyle.transactionIcon(LedgerDb.TYPE_INCOME, "工资"));
        assertEquals("↔", CartoonStyle.transactionIcon(LedgerDb.TYPE_TRANSFER, "账户转账"));
    }

    @Test
    public void semanticColorsAreDistinctAndOpaque() {
        assertNotEquals(CartoonStyle.INCOME, CartoonStyle.EXPENSE);
        assertNotEquals(CartoonStyle.EXPENSE, CartoonStyle.TRANSFER);
        assertEquals(0xFF000000, CartoonStyle.INCOME & 0xFF000000);
        assertEquals(0xFF000000, CartoonStyle.EXPENSE & 0xFF000000);
    }

    @Test
    public void accountTypesHaveFriendlyIcons() {
        assertEquals("👛", CartoonStyle.accountIcon("现金账户"));
        assertEquals("🏦", CartoonStyle.accountIcon("储蓄账户"));
        assertEquals("💳", CartoonStyle.accountIcon("信用卡账户"));
        assertEquals("🌱", CartoonStyle.accountIcon("投资账户"));
        assertEquals("🧺", CartoonStyle.accountIcon("其他账户"));
    }

    @Test
    public void cornerRadiusIsLargeEnoughForCartoonCards() {
        assertTrue(CartoonStyle.CARD_RADIUS_DP >= 20);
        assertTrue(CartoonStyle.BUTTON_RADIUS_DP >= 16);
    }
}
