package com.microsoft.japan.quote.configuration;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.microsoft.japan.quote.adapter.out.catalog.CatalogSkuPriceProvider;
import com.microsoft.japan.quote.adapter.out.persistence.InMemoryQuoteRepository;
import com.microsoft.japan.quote.application.QuoteService;
import com.microsoft.japan.quote.application.SkuPriceProvider;
import com.microsoft.japan.quote.domain.QuoteRepository;

/** ドメインを Spring に依存させずに構成要素を接続する. */
@Configuration(proxyBeanMethods = false)
public class QuoteConfiguration {

    private static final Duration CATALOG_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration CATALOG_READ_TIMEOUT = Duration.ofSeconds(5);

    /** メモリ保存先をシングルトンとして登録する. */
    @Bean
    public QuoteRepository quoteRepository() {
        return new InMemoryQuoteRepository();
    }

    /** サーバーのタイムゾーンで作成日を取得する時計を登録する. */
    @Bean
    public Clock quoteClock() {
        return Clock.systemDefaultZone();
    }

    /** 別サービスの SKU カタログ API から月額単価を取得するアダプターを登録する. */
    @Bean
    public SkuPriceProvider skuPriceProvider(@Value("${quote.catalog.base-url}") final String baseUrl) {
        final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CATALOG_CONNECT_TIMEOUT).build();
        final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(CATALOG_READ_TIMEOUT);
        return new CatalogSkuPriceProvider(RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build());
    }

    /** 見積もりのユースケースを登録する. */
    @Bean
    public QuoteService quoteService(final QuoteRepository repository,
            final SkuPriceProvider priceProvider, final Clock quoteClock) {
        return new QuoteService(repository, priceProvider, quoteClock);
    }
}
