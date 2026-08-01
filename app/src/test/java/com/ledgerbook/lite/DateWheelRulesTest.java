package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DateWheelRulesTest {
    @Test
    public void handlesLeapYears() {
        assertEquals(29, DateWheelRules.daysInMonth(2024, 2));
        assertEquals(28, DateWheelRules.daysInMonth(2100, 2));
        assertEquals(29, DateWheelRules.daysInMonth(2000, 2));
    }

    @Test
    public void handlesThirtyAndThirtyOneDayMonths() {
        assertEquals(30, DateWheelRules.daysInMonth(2026, 4));
        assertEquals(31, DateWheelRules.daysInMonth(2026, 7));
    }

    @Test
    public void rejectsInvalidMonth() {
        org.junit.Assert.assertThrows(IllegalArgumentException.class,
                () -> DateWheelRules.daysInMonth(2026, 13));
    }
}
