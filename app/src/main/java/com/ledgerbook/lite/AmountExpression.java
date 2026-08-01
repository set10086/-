package com.ledgerbook.lite;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AmountExpression {
    private AmountExpression() {
    }

    public static BigDecimal evaluate(String raw) {
        String expression = raw == null ? "" : raw.replace(" ", "")
                .replace('×', '*').replace('÷', '/').replace('−', '-');
        if (expression.isEmpty()) {
            throw new IllegalArgumentException("请输入金额");
        }
        Parser parser = new Parser(expression);
        BigDecimal value = parser.parseExpression();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("算式格式不正确");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text;
        }

        private BigDecimal parseExpression() {
            BigDecimal value = parseTerm();
            while (!atEnd()) {
                char operator = text.charAt(index);
                if (operator != '+' && operator != '-') {
                    break;
                }
                index++;
                BigDecimal right = parseTerm();
                value = operator == '+' ? value.add(right) : value.subtract(right);
            }
            return value;
        }

        private BigDecimal parseTerm() {
            BigDecimal value = parseNumber();
            while (!atEnd()) {
                char operator = text.charAt(index);
                if (operator != '*' && operator != '/') {
                    break;
                }
                index++;
                BigDecimal right = parseNumber();
                if (operator == '*') {
                    value = value.multiply(right);
                } else {
                    if (right.compareTo(BigDecimal.ZERO) == 0) {
                        throw new IllegalArgumentException("除数不能为零");
                    }
                    value = value.divide(right, 10, RoundingMode.HALF_UP);
                }
            }
            return value;
        }

        private BigDecimal parseNumber() {
            int start = index;
            boolean decimal = false;
            boolean digit = false;
            while (!atEnd()) {
                char character = text.charAt(index);
                if (character >= '0' && character <= '9') {
                    digit = true;
                    index++;
                } else if (character == '.' && !decimal) {
                    decimal = true;
                    index++;
                } else {
                    break;
                }
            }
            if (!digit) {
                throw new IllegalArgumentException("算式格式不正确");
            }
            try {
                return new BigDecimal(text.substring(start, index));
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("算式格式不正确");
            }
        }

        private boolean atEnd() {
            return index >= text.length();
        }
    }
}
