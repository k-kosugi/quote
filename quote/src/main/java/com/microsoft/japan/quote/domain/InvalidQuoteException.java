package com.microsoft.japan.quote.domain;

/** 見積もりの契約条件に違反したことを表す例外. */
public final class InvalidQuoteException extends IllegalArgumentException {

    /** 入力の問題を説明するメッセージを保持する. */
    public InvalidQuoteException(final String message) {
        super(message);
    }
}
