package com.microsoft.japan.quote.adapter.out.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.microsoft.japan.quote.application.CatalogUnavailableException;

/** カタログ API の応答を月額単価として公開する境界を検証する. */
final class CatalogSkuPriceProviderTests {

    private static final String BASE_URL = "http://catalog.test";

    private MockRestServiceServer server;
    private CatalogSkuPriceProvider provider;

    /** HTTP 通信を行わず, 応答を差し替えられる構成にする. */
    @BeforeEach
    void 通信を差し替える() {
        final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.provider = new CatalogSkuPriceProvider(builder.build());
    }

    /** カタログの月額価格を, 未知の項目を無視してそのまま単価として返す. */
    @Test
    @DisplayName("登録済みのSKUが渡された場合カタログの月額価格を返す")
    void 登録済みSKUが渡された場合正常終了する() {
        server.expect(requestTo(BASE_URL + "/api/skus/SKU-A")).andRespond(withSuccess("""
                {"id":"SKU-A","listPrice":1000,"description":"Core plan.","extra":true}
                """, MediaType.APPLICATION_JSON));

        assertEquals(Optional.of(new BigDecimal("1000")), provider.findMonthlyListPrice("SKU-A"));
    }

    /** 存在しない SKU には価格を返さない. */
    @Test
    @DisplayName("未登録のSKUが渡された場合空を返す")
    void 未登録SKUが渡された場合空を返す() {
        server.expect(requestTo(BASE_URL + "/api/skus/SKU-X")).andRespond(withResourceNotFound());

        assertEquals(Optional.empty(), provider.findMonthlyListPrice("SKU-X"));
    }

    /** パスの区切りを含む SKU を, パスとして解釈させない. */
    @Test
    @DisplayName("スラッシュを含むSKUが渡された場合パスの区切りとして扱わずに問い合わせる")
    void スラッシュを含むSKUが渡された場合パスの区切りとして扱わない() {
        server.expect(requestTo(BASE_URL + "/api/skus/a%2Fb")).andRespond(withResourceNotFound());

        assertEquals(Optional.empty(), provider.findMonthlyListPrice("a/b"));
    }

    /** API Management のように, 基底 URL にパスが付いていても, パスを保って問い合わせる. */
    @Test
    @DisplayName("基底URLにパスが含まれる場合そのパスの下に問い合わせる")
    void 基底URLにパスが含まれる場合そのパスの下に問い合わせる() {
        final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL + "/catalog");
        final MockRestServiceServer pathServer = MockRestServiceServer.bindTo(builder).build();
        pathServer.expect(requestTo(BASE_URL + "/catalog/api/skus/SKU-A"))
                .andRespond(withSuccess("{\"listPrice\":1000}", MediaType.APPLICATION_JSON));

        assertEquals(Optional.of(new BigDecimal("1000")),
                new CatalogSkuPriceProvider(builder.build()).findMonthlyListPrice("SKU-A"));
    }

    /** カタログのサーバーエラーを, 未登録と区別して通知する. */
    @Test
    @DisplayName("カタログが5xxを返した場合CatalogUnavailableExceptionが発生する")
    void カタログが5xxを返した場合例外が発生する() {
        server.expect(requestTo(BASE_URL + "/api/skus/SKU-A")).andRespond(withServerError());

        assertThrows(CatalogUnavailableException.class, () -> provider.findMonthlyListPrice("SKU-A"));
    }
}
