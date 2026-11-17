package com.microsoft.japan.quote;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.microsoft.japan.quote.application.CatalogUnavailableException;
import com.microsoft.japan.quote.application.SkuPriceProvider;

/** 結合テストで別サービスのカタログの代わりに使う, 固定の単価表を持つ構成. */
@TestConfiguration(proxyBeanMethods = false)
public class FakeCatalogConfiguration {

    /** 接続できない状況を再現する SKU ID. */
    public static final String UNAVAILABLE_SKU = "SKU-CATALOG-DOWN";

    private static final Map<String, BigDecimal> PRICES = Map.of(
            "SKU-A", new BigDecimal("1000"),
            "SKU-B", new BigDecimal("2000"),
            "SKU-C", new BigDecimal("500"),
            "SKU-D", new BigDecimal("1500"),
            "SKU-STANDARD", new BigDecimal("1000"),
            "SKU-PROFESSIONAL", new BigDecimal("3000"));

    /** 単価表から価格を返し, 特定の SKU では接続失敗を再現する. */
    @Bean
    @Primary
    public SkuPriceProvider fakeSkuPriceProvider() {
        return sku -> {
            if (UNAVAILABLE_SKU.equals(sku)) {
                throw new CatalogUnavailableException(new IllegalStateException("catalog down"));
            }
            return Optional.ofNullable(PRICES.get(sku));
        };
    }
}
