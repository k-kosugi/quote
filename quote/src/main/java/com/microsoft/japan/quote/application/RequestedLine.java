package com.microsoft.japan.quote.application;

import com.microsoft.japan.quote.domain.InvalidQuoteException;

/** 利用者が指定する SKU と増減数量. 単価は含まず, カタログから解決する. */
public record RequestedLine(String sku, int amount) {

    /** SKU が空でないことと数量が 0 でないことを検証する. */
    public RequestedLine {
        if (sku == null || sku.isBlank()) {
            throw new InvalidQuoteException("SKU is required.");
        }
        if (amount == 0) {
            throw new InvalidQuoteException("Amount must not be zero.");
        }
        sku = sku.strip();
    }
}
