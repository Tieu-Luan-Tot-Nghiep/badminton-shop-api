package com.badminton.shop.modules.shipping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.shipping.ghn")
public class GHNProperties {

    private String baseUrl = "https://dev-online-gateway.ghn.vn/shiip/public-api";
    private String token;
    private Long shopId;
    private Integer fromDistrictId;
    private Integer serviceTypeId = 2;

    private Integer defaultWeightGrams = 1000;
    private Integer defaultLengthCm = 35;
    private Integer defaultWidthCm = 10;
    private Integer defaultHeightCm = 5;

    private String callbackAuthToken;
}
