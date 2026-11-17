package com.microsoft.japan.quote.domain;

import java.math.BigDecimal;

/** 割引と円単位の丸めを適用した金額. 年額合計は数量を含む1年分, 合計金額は契約期間全体の総額. */
public record QuoteTotals(BigDecimal totalAnnual, BigDecimal totalAmount) {

    /** 合計金額の必須条件と非負条件を検証する. */
    public QuoteTotals {
        if (totalAnnual == null || totalAmount == null
                || totalAnnual.signum() < 0 || totalAmount.signum() < 0) {
            throw new InvalidQuoteException("Totals must be zero or greater.");
        }
    }
}
