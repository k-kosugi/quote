package com.microsoft.japan.quote.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.japan.quote.FakeCatalogConfiguration;
import com.microsoft.japan.quote.domain.QuoteRepository;

/** HTTP アダプターからメモリ保存先までの結合動作を検証する. カタログは固定の単価表に差し替える. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FakeCatalogConfiguration.class)
final class QuoteApiIT {

    private static final String BASE = "/api/quotes";
    private static final String REQUEST = """
            {
              "contractStart": "2026-07-01",
              "contractEnd": "2029-06-30",
              "skuList": [
                {"sku": "SKU-A", "amount": 2},
                {"sku": "SKU-B", "amount": 3},
                {"sku": "SKU-C", "amount": -1}
              ],
              "discountRate": 10
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private QuoteRepository repository;

    @MockitoBean
    private Clock clock;

    /** テスト間でデータを共有せず、作成日を固定する. */
    @BeforeEach
    void 保存先を初期化する() {
        repository.findAll().forEach(quote -> repository.delete(quote.getId()));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Tokyo"));
        when(clock.instant()).thenReturn(Instant.parse("2026-11-11T14:59:59Z"));
    }

    /** 画像に対応する正負の明細の計算結果を検証する. */
    @Test
    @DisplayName("作成APIに正負の明細が渡された場合201とサーバー計算値が返る")
    void 作成APIに有効な明細が渡された場合正常終了する() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.validUntil").value("2026-11-25"))
                .andExpect(jsonPath("$.contractStart").value("2026-07-01"))
                .andExpect(jsonPath("$.contractEnd").value("2029-06-30"))
                .andExpect(jsonPath("$.region").doesNotExist())
                .andExpect(jsonPath("$.discountRate").value(10))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.contractType").value("EARLY_RENEWAL"))
                .andExpect(jsonPath("$.skuList[0].annual").value(24000))
                .andExpect(jsonPath("$.skuList[1].annual").value(72000))
                .andExpect(jsonPath("$.skuList[2].annual").value(0))
                .andExpect(jsonPath("$.totalAnnual").value(86400))
                .andExpect(jsonPath("$.totalAmount").value(259200));
    }

    /** 同じ ID の集約に対するライフサイクルを検証する. */
    @Test
    @DisplayName("作成したIDが各APIに渡された場合取得と保存と修正と削除ができる")
    void 各APIに作成済みIDが渡された場合正常終了する() throws Exception {
        final String location = createQuote();
        final String id = location.substring(location.lastIndexOf('/') + 1);
        when(clock.instant()).thenReturn(Instant.parse("2026-11-12T15:00:00Z"));

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validUntil").value("2026-11-25"))
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(post(location + "/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validUntil").value("2026-11-25"))
                .andExpect(jsonPath("$.status").value("SAVED"));
        mvc.perform(post(location + "/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validUntil").value("2026-11-25"))
                .andExpect(jsonPath("$.status").value("SAVED"));
        mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST.replace("\"discountRate\": 10", "\"discountRate\": 0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.validUntil").value("2026-11-25"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(288000));
        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.validUntil").value("2026-11-25"))
                .andExpect(jsonPath("$.totalAmount").value(288000));
        mvc.perform(delete(location)).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get(location)).andExpect(status().isNotFound());
        mvc.perform(get(BASE)).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    /** 不正な修正によって保存済みの内容や状態が壊れないことを検証する. */
    @Test
    @DisplayName("修正APIに禁止SKUが渡された場合400となり保存済みデータは変化しない")
    void 修正APIに禁止SKUが渡された場合保存済みデータを維持する() throws Exception {
        final String location = createQuote();
        mvc.perform(post(location + "/save")).andExpect(status().isOk());
        final String invalid = REQUEST.replace("\"SKU-A\"", "\"SKU-STANDARD\"")
                .replace("\"SKU-B\"", "\"SKU-PROFESSIONAL\"");

        mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("SKU-PROFESSIONAL and SKU-STANDARD cannot be combined."));
        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAVED"))
                .andExpect(jsonPath("$.skuList[0].sku").value("SKU-A"))
                .andExpect(jsonPath("$.totalAmount").value(259200));
    }

    /** 構文や型の誤りと業務制約違反はどちらも登録しない. */
    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "null",
            "{",
            """
            {"contractStart":"2026-07-01","contractEnd":"2029-06-30",
             "skuList":[{"sku":"SKU-A","amount":0}],"discountRate":0}
            """,
            """
            {"contractStart":"2026-07-01","contractEnd":"2029-06-30",
             "skuList":[{"sku":"SKU-A","amount":1.5}],"discountRate":0}
            """,
            """
            {"contractStart":"2026-07-01","contractEnd":"2029-06-30",
             "skuList":[{"sku":"SKU-A"}],"discountRate":0}
            """,
            """
            {"contractStart":"2026-07-01","contractEnd":"2029-06-30",
             "skuList":[null],"discountRate":0}
            """,
            """
            {"contractStart":"2026-07-01","contractEnd":"2029-06-30",
             "skuList":[{"sku":"SKU-A","amount":2147483648}],"discountRate":0}
            """,
            """
            {"contractStart":"2026-07-01","contractEnd":"2029-06-30",
             "skuList":[{"sku":"SKU-UNKNOWN","amount":1}],"discountRate":0}
            """,
            """
            {"contractStart":"2026-07-01","contractEnd":"2029-06-30",
             "skuList":[{"sku":"sku-a","amount":1}],"discountRate":0}
            """,
            """
            {"contractStart":"2026-07-01","contractEnd":"2029-06-30",
             "skuList":[{"sku":"SKU-A","listPrice":1,"amount":1}],"discountRate":0}
            """
    })
    @DisplayName("作成APIに不正な入力が渡された場合400となりデータは登録されない")
    void 作成APIに不正な入力が渡された場合登録されない(final String request) throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").isString());
        assertEquals(0, repository.findAll().size());
    }

    /** カタログに接続できない場合は、未登録の SKU と区別して 503 を返す. */
    @Test
    @DisplayName("カタログに接続できない場合503となり登録されない")
    void カタログに接続できない場合登録されない() throws Exception {
        final String request = REQUEST.replace("\"SKU-A\"", "\"" + FakeCatalogConfiguration.UNAVAILABLE_SKU + "\"");
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("SKU catalog is unavailable."));
        assertEquals(0, repository.findAll().size());
    }

    /** クライアントが計算値を送信しても受け入れない. */
    @Test
    @DisplayName("入力にtotalAmountが渡された場合400となり登録されない")
    void 入力に計算結果が渡された場合登録されない() throws Exception {
        final String request = REQUEST.replace("\"discountRate\": 10", "\"discountRate\": 10, \"totalAmount\": 1");
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest());
        assertEquals(0, repository.findAll().size());
    }

    /** 地域は日本固定のため, クライアントから指定させない. */
    @Test
    @DisplayName("入力にregionが渡された場合400となり登録されない")
    void 入力に地域が渡された場合登録されない() throws Exception {
        final String request = REQUEST.replace("\"discountRate\": 10", "\"discountRate\": 10, \"region\": \"Japan\"");
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest());
        assertEquals(0, repository.findAll().size());
    }

    /** 有効期限をクライアントから上書きさせない. */
    @Test
    @DisplayName("入力にvalidUntilが渡された場合400となり登録されない")
    void 入力に有効期限が渡された場合登録されない() throws Exception {
        final String request = REQUEST.replace("\"discountRate\": 10",
                "\"discountRate\": 10, \"validUntil\": \"2030-01-01\"");
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest());
        assertEquals(0, repository.findAll().size());
    }

    /** 全ての単件操作で未存在を明示する. */
    @Test
    @DisplayName("各APIに未存在のIDが渡された場合404が返る")
    void 各APIに未存在IDが渡された場合404が返る() throws Exception {
        final String location = BASE + "/" + UUID.randomUUID();
        mvc.perform(get(location))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isNotFound());
        mvc.perform(post(location + "/save")).andExpect(status().isNotFound());
        mvc.perform(delete(location)).andExpect(status().isNotFound());
    }

    /** UUID として解釈できない ID を拒否する. */
    @Test
    @DisplayName("取得APIに不正なUUIDが渡された場合400が返る")
    void 取得APIに不正なUUIDが渡された場合400が返る() throws Exception {
        mvc.perform(get(BASE + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Quote ID must be a UUID."));
    }

    /** 生成された OpenAPI が HTTP で公開されることを検証する. */
    @Test
    @DisplayName("OpenAPI仕様取得APIが呼び出された場合実装に基づく仕様が返る")
    void OpenAPI仕様取得APIが呼び出された場合正常終了する() throws Exception {
        final String specification = mvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(specification.contains("openapi:"));
        assertTrue(specification.contains("title: 見積 API"));
        assertTrue(specification.contains("/api/quotes:"));
        assertTrue(specification.contains("operationId: createQuote"));
        assertTrue(specification.contains("SKU-STANDARD と SKU-PROFESSIONAL"));
        assertTrue(specification.contains("      - skuList"));
    }

    /** ファイルとして置いた仕様 (openapi.json) が, 実装から生成した仕様と一致することを検証する. */
    @Test
    @DisplayName("openapi.jsonが渡された場合実装から生成した仕様と一致する")
    void openapi_jsonが実装と一致する() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode generated = (ObjectNode) mapper.readTree(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        final ObjectNode exported = (ObjectNode) mapper.readTree(Files.readString(Path.of("openapi.json")));

        // 呼び出し先の URL は環境ごとに異なるため比較しない
        generated.remove("servers");
        exported.remove("servers");
        assertEquals(exported, generated);
    }

    /** Swagger UI が公開されることを検証する. */
    @Test
    @DisplayName("Swagger UIが呼び出された場合画面が返る")
    void SwaggerUIが呼び出された場合正常終了する() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    /** ドラフトを作成して Location を返す. */
    private String createQuote() throws Exception {
        return mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }
}
