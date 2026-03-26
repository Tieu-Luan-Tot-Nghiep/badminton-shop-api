package com.badminton.shop.modules.shipping.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(GHNProperties.class)
@Slf4j
public class ShippingWebClientConfig {

    @Bean("ghnWebClient")
    public WebClient ghnWebClient(WebClient.Builder builder, GHNProperties properties) {
        String baseUrl = normalizeBaseUrl(properties.getBaseUrl());
        String token = normalizeToken(properties.getToken());
        return builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader("Token", token)
                .defaultHeaders(headers -> {
                    Long shopId = properties.getShopId();
                    if (shopId != null && shopId > 0) {
                        headers.set("ShopId", String.valueOf(shopId));
                    }
                })
                .filter(logRequest())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            URI url = request.url();
            log.info("GHN request => {} {}", request.method(), url);
            return Mono.just(request);
        });
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        if (normalized.isEmpty()) {
            normalized = "https://dev-online-gateway.ghn.vn/shiip/public-api";
        }

        // Accept GHN_BASE_URL without path and auto-fix to GHN public API base.
        if (!normalized.contains("/shiip/public-api")) {
            if (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            normalized = normalized + "/shiip/public-api";
        }

        // Keep trailing slash so relative paths like "master-data/province"
        // are appended under /shiip/public-api/ instead of replacing "public-api".
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }

        return normalized;
    }

    private String normalizeToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("Missing GHN token. Please configure app.shipping.ghn.token or GHN_TOKEN.");
        }
        String normalized = token.trim()
                .replace("\r", "")
                .replace("\n", "");

        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }

        if (normalized.regionMatches(true, 0, "Bearer ", 0, 7)) {
            normalized = normalized.substring(7).trim();
        }

        if (normalized.isEmpty()) {
            throw new IllegalStateException("GHN token is empty after normalization. Please set GHN_TOKEN correctly.");
        }

        return normalized;
    }
}
