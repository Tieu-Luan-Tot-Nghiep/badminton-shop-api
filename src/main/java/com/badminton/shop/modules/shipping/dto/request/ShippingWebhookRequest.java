package com.badminton.shop.modules.shipping.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingWebhookRequest {

    @JsonProperty("OrderCode")
    @JsonAlias({"order_code", "orderCode"})
    private String orderCode;

    @JsonProperty("ClientOrderCode")
    @JsonAlias({"client_order_code", "clientOrderCode"})
    private String clientOrderCode;

    @JsonProperty("Status")
    @JsonAlias({"status", "StatusCode"})
    private String status;

    @JsonProperty("ShopID")
    @JsonAlias({"shop_id", "shopId"})
    private Long shopId;
}
