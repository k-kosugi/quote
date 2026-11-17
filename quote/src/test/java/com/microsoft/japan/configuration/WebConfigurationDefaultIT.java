package com.microsoft.japan.configuration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.microsoft.japan.quote.QuoteApplication;

/** プロファイル未指定の場合に CORS が許可されないことを HTTP 結合動作として検証する. */
@SpringBootTest(classes = QuoteApplication.class)
@AutoConfigureMockMvc
final class WebConfigurationDefaultIT {

    private static final String DEV_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mvc;

    /** 許可オリジンが未設定の場合に、別オリジンへの許可ヘッダーが付かないことを検証する. */
    @Test
    @DisplayName("プロファイル未指定でAPIが呼び出された場合許可ヘッダーが付かない")
    void プロファイル未指定でAPIが呼び出された場合許可ヘッダーが付かない() throws Exception {
        mvc.perform(get("/api/quotes").header(HttpHeaders.ORIGIN, DEV_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
