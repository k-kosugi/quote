package com.microsoft.japan.quote.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.microsoft.japan.quote.domain.InvalidQuoteException;

/** 依頼明細の入力検証を検証する. */
final class RequestedLineTests {

    /** 数量 0 だけが不正で, 前後の追加と解約は許可する. */
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    @DisplayName("数量の境界値に-1と0と1が渡された場合0だけ拒否する")
    void 数量の境界値前後が渡された場合0だけInvalidQuoteExceptionが発生する(final int amount) {
        if (amount == 0) {
            assertThrows(InvalidQuoteException.class, () -> new RequestedLine("SKU-A", amount));
        } else {
            assertEquals(amount, new RequestedLine("SKU-A", amount).amount());
        }
    }

    /** 空の SKU を拒否する. */
    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("SKUに空文字または空白が渡された場合InvalidQuoteExceptionが発生する")
    void SKUに空文字が渡された場合InvalidQuoteExceptionが発生する(final String sku) {
        assertThrows(InvalidQuoteException.class, () -> new RequestedLine(sku, 1));
    }

    /** null の SKU を拒否する. */
    @Test
    @DisplayName("SKUにnullが渡された場合InvalidQuoteExceptionが発生する")
    void SKUにnullが渡された場合InvalidQuoteExceptionが発生する() {
        assertThrows(InvalidQuoteException.class, () -> new RequestedLine(null, 1));
    }

    /** 前後の空白を取り除く. */
    @Test
    @DisplayName("SKUの前後に空白が渡された場合取り除いて保持する")
    void SKUの前後に空白が渡された場合正常終了する() {
        assertEquals("SKU-A", new RequestedLine(" SKU-A ", 1).sku());
    }
}
