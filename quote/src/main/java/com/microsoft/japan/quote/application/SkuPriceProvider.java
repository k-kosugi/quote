package com.microsoft.japan.quote.application;

import java.math.BigDecimal;
import java.util.Optional;

/** 見積もりが SKU の月額単価を取得するためのポート. 価格の決定権は SKU カタログにある. */
public interface SkuPriceProvider {

    /** SKU の月額単価 (JPY) を返す. SKU が存在しない場合は空を返し, カタログに接続できない場合は CatalogUnavailableException とする. */
    Optional<BigDecimal> findMonthlyListPrice(String sku);
}
