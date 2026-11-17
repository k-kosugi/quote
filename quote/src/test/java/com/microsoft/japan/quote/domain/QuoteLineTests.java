package com.microsoft.japan.quote.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 明細同士が同じ見積もりに同居できるかの判定を検証する. */
final class QuoteLineTests {

    /** 組み合わせの向きに依存せず, SKU-STANDARD と SKU-PROFESSIONAL だけが同居できない. */
    @ParameterizedTest
    @CsvSource({
            "SKU-STANDARD,SKU-PROFESSIONAL,true",
            "SKU-PROFESSIONAL,SKU-STANDARD,true",
            "SKU-STANDARD,SKU-STANDARD,false",
            "SKU-PROFESSIONAL,SKU-PROFESSIONAL,false",
            "SKU-STANDARD,SKU-A,false",
            "SKU-A,SKU-PROFESSIONAL,false",
            "sku-standard,SKU-PROFESSIONAL,false"})
    @DisplayName("2つの明細のSKUが渡された場合SKU-STANDARDとSKU-PROFESSIONALの組み合わせだけ同居不可と判定する")
    void 明細の組み合わせが渡された場合同居可否を判定する(
            final String first, final String second, final boolean expected) {
        assertEquals(expected, line(first).isIncompatibleWith(line(second)));
    }

    /** テスト用の明細を生成する. */
    private static QuoteLine line(final String sku) {
        return new QuoteLine(sku, BigDecimal.ONE, 1);
    }
}
