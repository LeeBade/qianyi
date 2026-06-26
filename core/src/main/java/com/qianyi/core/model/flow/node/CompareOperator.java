package com.qianyi.core.model.flow.node;

/**
 *
 *
 * @author TianJunQi
 * @since 2026-06-26
 */
public enum CompareOperator {
    EQ("=="),
    NEQ("!="),
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<=");

    private final String symbol;

    CompareOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
