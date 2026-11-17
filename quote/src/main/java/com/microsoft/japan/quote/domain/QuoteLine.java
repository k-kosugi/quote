package com.microsoft.japan.quote.domain;

import java.math.BigDecimal;

/** 月額単価と増減数量を持つ見積もり明細. */
public record QuoteLine(String sku, BigDecimal listPrice, int amount) {

    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");
    private static final String STANDARD_SKU = "SKU-STANDARD";
    private static final String PROFESSIONAL_SKU = "SKU-PROFESSIONAL";

    /** SKU と単価および 0 以外の数量を検証する. */
    public QuoteLine {
        if (sku == null || sku.isBlank()) {
            throw new InvalidQuoteException("SKU is required.");
        }
        if (listPrice == null || listPrice.signum() < 0) {
            throw new InvalidQuoteException("Monthly list price must be zero or greater.");
        }
        if (amount == 0) {
            throw new InvalidQuoteException("Amount must not be zero.");
        }
        sku = sku.strip();
    }

    /** 解約行かどうかを返す. */
    public boolean isCancellation() {
        return this.amount < 0;
    }

    /** 同じ見積もりに同居できない SKU の組み合わせかどうかを返す. 順序によらず判定する. */
    public boolean isIncompatibleWith(final QuoteLine other) {
        return this.sku.equals(STANDARD_SKU) && other.sku.equals(PROFESSIONAL_SKU)
                || this.sku.equals(PROFESSIONAL_SKU) && other.sku.equals(STANDARD_SKU);
    }

    /** 解約行は 0 とし、それ以外は数量を掛けた 1 年分の金額を返す. */
    public BigDecimal calculateAnnual() {
        return this.isCancellation() ? BigDecimal.ZERO
                : this.listPrice.multiply(MONTHS_PER_YEAR).multiply(BigDecimal.valueOf(this.amount));
    }
}
