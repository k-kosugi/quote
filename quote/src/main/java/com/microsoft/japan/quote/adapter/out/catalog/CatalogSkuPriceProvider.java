package com.microsoft.japan.quote.adapter.out.catalog;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.japan.quote.application.CatalogUnavailableException;
import com.microsoft.japan.quote.application.SkuPriceProvider;

/** 別サービスの SKU カタログ API から月額単価を取得する. カタログのモデルを見積に持ち込まない境界となる. */
public final class CatalogSkuPriceProvider implements SkuPriceProvider {

    private static final String SKU_PATH = "/api/skus/{sku}";

    private final RestClient client;

    /** カタログ API の基底 URL を設定した HTTP クライアントを受け取る. */
    public CatalogSkuPriceProvider(final RestClient client) {
        this.client = client;
    }

    /** カタログに登録された SKU の月額単価を返す. 未登録の場合は空を返し, 接続できない場合は例外とする. */
    @Override
    public Optional<BigDecimal> findMonthlyListPrice(final String sku) {
        try {
            return Optional.ofNullable(this.client.get().uri(SKU_PATH, sku).retrieve().body(SkuPayload.class))
                    .map(SkuPayload::listPrice);
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientException exception) {
            throw new CatalogUnavailableException(exception);
        }
    }

    /** カタログ API の応答のうち, 単価だけを読み取る. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SkuPayload(BigDecimal listPrice) {
    }
}
