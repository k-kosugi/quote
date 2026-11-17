package com.microsoft.japan.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** すべてのコンテキストに共通する Web の設定を行う. */
@Configuration(proxyBeanMethods = false)
public class WebConfiguration implements WebMvcConfigurer {

    private static final String API_PATH_PATTERN = "/api/**";

    private final String[] allowedOrigins;

    /**
     * CORS で許可するオリジンを受け取る.
     *
     * @param allowedOrigins quote.cors.allowed-origins に指定したオリジン. 未指定の場合は空
     */
    public WebConfiguration(@Value("${quote.cors.allowed-origins:}") final String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins.clone();
    }

    /** オリジンが指定されている場合だけ, API に対する別オリジンからのアクセスを許可する. */
    @Override
    public void addCorsMappings(final CorsRegistry registry) {
        if (allowedOrigins.length == 0) {
            return;
        }
        registry.addMapping(API_PATH_PATTERN)
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
