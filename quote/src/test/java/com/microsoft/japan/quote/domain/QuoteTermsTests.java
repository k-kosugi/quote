package com.microsoft.japan.quote.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 契約月数の数え方を検証する. */
final class QuoteTermsTests {

    /** 終了日を含めて暦の月数で数える. */
    @ParameterizedTest
    @CsvSource({
            "2026-07-01,2029-06-30,36",
            "2026-07-01,2027-06-30,12",
            "2026-07-01,2026-07-31,1",
            "2028-02-01,2028-02-29,1"})
    @DisplayName("契約期間が月の初日から末日までちょうどの月数になる場合端数なしで数える")
    void 契約期間がちょうどの月数の場合その月数になる(
            final String start, final String end, final int expected) {
        assertEquals(expected, new QuoteTerms(LocalDate.parse(start), LocalDate.parse(end)).contractMonths());
    }

    /** 端数の日が 1 日でもあれば 1 か月に切り上げる境界を検証する. */
    @Test
    @DisplayName("契約月数の境界値に1か月ちょうどと1か月と1日が渡された場合1か月と2か月になる")
    void 契約月数の境界値前後が渡された場合端数を切り上げる() {
        final LocalDate start = LocalDate.of(2026, 7, 1);

        assertEquals(1, new QuoteTerms(start, LocalDate.of(2026, 7, 31)).contractMonths());
        assertEquals(2, new QuoteTerms(start, LocalDate.of(2026, 8, 1)).contractMonths());
    }

    /** 同日契約も最低 1 か月とする. */
    @Test
    @DisplayName("開始日と終了日が同日の場合1か月になる")
    void 開始日と終了日が同日の場合1か月になる() {
        final LocalDate day = LocalDate.of(2026, 7, 1);

        assertEquals(1, new QuoteTerms(day, day).contractMonths());
    }
}
