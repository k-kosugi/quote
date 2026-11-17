package com.microsoft.japan.quote.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** 集計結果の非負条件と必須条件を検証する. */
final class QuoteTotalsTests {

    /** 単価合計の下限の前後を検証する. */
    @Test
    @DisplayName("totalAnnualの境界値に-1と0と1が渡された場合-1だけInvalidQuoteExceptionが発生する")
    void totalAnnualの境界値前後が渡された場合負数だけInvalidQuoteExceptionが発生する() {
        assertThrows(InvalidQuoteException.class, () -> new QuoteTotals(new BigDecimal("-1"), BigDecimal.ZERO));
        assertDoesNotThrow(() -> new QuoteTotals(BigDecimal.ZERO, BigDecimal.ZERO));
        assertDoesNotThrow(() -> new QuoteTotals(BigDecimal.ONE, BigDecimal.ZERO));
    }

    /** 数量を考慮した合計の下限の前後を検証する. */
    @Test
    @DisplayName("totalAmountの境界値に-1と0と1が渡された場合-1だけInvalidQuoteExceptionが発生する")
    void totalAmountの境界値前後が渡された場合負数だけInvalidQuoteExceptionが発生する() {
        assertThrows(InvalidQuoteException.class, () -> new QuoteTotals(BigDecimal.ZERO, new BigDecimal("-1")));
        assertDoesNotThrow(() -> new QuoteTotals(BigDecimal.ZERO, BigDecimal.ZERO));
        assertDoesNotThrow(() -> new QuoteTotals(BigDecimal.ZERO, BigDecimal.ONE));
    }

    /** 合計の片方が欠落した場合を検証する. */
    @ParameterizedTest
    @MethodSource("missingTotals")
    @DisplayName("合計金額のいずれかにnullが渡された場合InvalidQuoteExceptionが発生する")
    void 合計金額にnullが渡された場合InvalidQuoteExceptionが発生する(
            final BigDecimal annual, final BigDecimal amount) {
        assertThrows(InvalidQuoteException.class, () -> new QuoteTotals(annual, amount));
    }

    /** 欠落した合計金額のパターンを供給する. */
    private static Stream<Arguments> missingTotals() {
        return Stream.of(Arguments.of(null, BigDecimal.ZERO), Arguments.of(BigDecimal.ZERO, null));
    }
}
