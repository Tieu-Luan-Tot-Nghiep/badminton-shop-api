package com.badminton.shop.config;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestClientBuilderCustomizer elasticsearchApiKeyCustomizer(
            @Value("${app.search.elasticsearch.api-key:}") String apiKey
    ) {
        return builder -> {
            if (apiKey == null || apiKey.isBlank()) {
                return;
            }
            Header authorizationHeader = new BasicHeader("Authorization", "ApiKey " + apiKey);
            builder.setDefaultHeaders(new Header[]{authorizationHeader});
        };
    }
}
