package com.ledgerbook.lite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import org.junit.Test;

public class AmountExpressionTest {
    @Test
    public void respectsMultiplicationPrecedence() {
        assertEquals(new BigDecimal("28.50"), AmountExpression.evaluate("12.5+8*2"));
    }

    @Test
    public void supportsSubtractionAndDecimalDivision() {
        assertEquals(new BigDecimal("7.50"), AmountExpression.evaluate("10-5/2"));
    }

    @Test
    public void acceptsDisplayOperators() {
        assertEquals(new BigDecimal("18.00"), AmountExpression.evaluate("20−8÷4"));
    }

    @Test
    public void rejectsDivisionByZero() {
        assertThrows(IllegalArgumentException.class, () -> AmountExpression.evaluate("5/0"));
    }

    @Test
    public void rejectsMalformedExpression() {
        assertThrows(IllegalArgumentException.class, () -> AmountExpression.evaluate("1++2"));
    }
}
