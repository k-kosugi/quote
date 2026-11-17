package com.microsoft.japan.quote.domain;

import java.util.UUID;

/** null を許可しない見積もり識別子. */
public record QuoteId(UUID value) {

    /** 識別子の必須条件を検証する. */
    public QuoteId {
        if (value == null) {
            throw new InvalidQuoteException("Quote ID is required.");
        }
    }
}
