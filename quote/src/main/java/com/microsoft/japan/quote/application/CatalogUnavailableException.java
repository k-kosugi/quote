package com.microsoft.japan.quote.application;

/** SKU カタログに接続できず, 単価を確認できないことを表す. */
public final class CatalogUnavailableException extends RuntimeException {

    /** 原因を保持して, 単価を確認できなかったことを通知する. */
    public CatalogUnavailableException(final Throwable cause) {
        super("SKU catalog is unavailable.", cause);
    }
}
