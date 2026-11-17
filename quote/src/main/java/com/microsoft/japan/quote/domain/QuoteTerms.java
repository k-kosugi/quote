package com.microsoft.japan.quote.domain;

import java.time.LocalDate;
import java.time.Period;

/** 契約期間を表す契約条件. 見積の対象地域は日本に限られるため, 地域は保持しない. */
public record QuoteTerms(
        LocalDate contractStart,
        LocalDate contractEnd) {

    /** 必須項目と契約日の前後関係を検証する. */
    public QuoteTerms {
        if (contractStart == null || contractEnd == null) {
            throw new InvalidQuoteException("Contract start and contract end are required.");
        }
        if (contractStart.isAfter(contractEnd)) {
            throw new InvalidQuoteException("Contract start must not be after contract end.");
        }
    }

    /**
     * 契約期間の月数を返す. 終了日を含む暦の月数で数え, 端数の日がある場合は 1 か月に切り上げる.
     * 同日契約は 1 か月になる.
     */
    public int contractMonths() {
        final Period period = Period.between(contractStart, contractEnd.plusDays(1));
        final int months = Math.toIntExact(period.toTotalMonths());
        return period.getDays() > 0 ? months + 1 : months;
    }
}
