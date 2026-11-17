package com.microsoft.japan.quote.application;

import com.microsoft.japan.quote.domain.QuoteId;

/** 操作対象の見積もりが存在しないことを表す. */
public final class QuoteNotFoundException extends RuntimeException {

    /** 見つからなかった識別子をメッセージに含める. */
    public QuoteNotFoundException(final QuoteId id) {
        super("Quote not found: " + id.value());
    }
}
