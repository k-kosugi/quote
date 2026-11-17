package com.microsoft.japan.quote.domain;

import java.math.BigDecimal;

/** 見積もり全体に適用する百分率の割引率. */
public record DiscountRate(BigDecimal value) {

    private static final BigDecimal MAX_RATE = new BigDecimal("100");

    /** 0 から 100 までの割引率であることを検証する. */
    public DiscountRate {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(MAX_RATE) > 0) {
            throw new InvalidQuoteException("Discount rate must be between 0 and 100.");
        }
    }

    /** 割引後の比率を返す. */
    public BigDecimal calculateMultiplier() {
        return BigDecimal.ONE.subtract(this.value.movePointLeft(2));
    }
}
