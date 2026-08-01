package com.ledgerbook.lite;

import java.util.GregorianCalendar;

public final class DateWheelRules {
    private DateWheelRules() {
    }

    public static int daysInMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("月份必须在 1 到 12 之间");
        }
        switch (month) {
            case 2:
                return new GregorianCalendar().isLeapYear(year) ? 29 : 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }
}
