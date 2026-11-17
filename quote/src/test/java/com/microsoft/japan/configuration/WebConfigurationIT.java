package com.microsoft.japan.configuration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.microsoft.japan.quote.QuoteApplication;

/** 開発プロファイルにおける CORS の許可設定を HTTP 結合動作として検証する. */
@SpringBootTest(classes = QuoteApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
final class WebConfigurationIT {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String OTHER_ORIGIN = "http://example.com";

    @Autowired
    private MockMvc mvc;

    /** 許可したオリジンからの事前確認リクエストが通ることを検証する. */
    @Test
    @DisplayName("許可したオリジンから事前確認リクエストが送られた場合許可ヘッダーが返る")
    void 許可したオリジンから事前確認リクエストが送られた場合正常終了する() throws Exception {
        mvc.perform(options("/api/quotes")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    /** 許可していないオリジンの事前確認リクエストが拒否されることを検証する. */
    @Test
    @DisplayName("許可していないオリジンから事前確認リクエストが送られた場合403が返る")
    void 許可していないオリジンから事前確認リクエストが送られた場合403が返る() throws Exception {
        mvc.perform(options("/api/quotes")
                        .header(HttpHeaders.ORIGIN, OTHER_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    /** 許可したオリジンからの通常リクエストに許可ヘッダーが付くことを検証する. */
    @Test
    @DisplayName("許可したオリジンから見積一覧が呼び出された場合許可ヘッダーが返る")
    void 許可したオリジンから見積一覧が呼び出された場合正常終了する() throws Exception {
        mvc.perform(get("/api/quotes").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }
}
