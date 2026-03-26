package com.badminton.shop.modules.shipping.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.shipping.config.GHNProperties;
import com.badminton.shop.modules.shipping.dto.request.ShippingWebhookRequest;
import com.badminton.shop.modules.shipping.service.ShippingWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipping/webhook")
@RequiredArgsConstructor
public class ShippingWebhookController {

    private final ShippingWebhookService shippingWebhookService;
    private final GHNProperties ghnProperties;

    @PostMapping("/ghn")
    public ResponseEntity<ApiResponse<Object>> receiveWebhook(
            @RequestBody ShippingWebhookRequest request,
            @RequestHeader(value = "X-Webhook-Token", required = false) String webhookToken) {

        String expectedToken = ghnProperties.getCallbackAuthToken();
        if (expectedToken != null && !expectedToken.isBlank()
                && (webhookToken == null || !expectedToken.equals(webhookToken))) {
            return ResponseEntity.status(401).body(ApiResponse.error(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Webhook token is invalid."));
        }

        shippingWebhookService.handleWebhook(request);
        return ResponseEntity.ok(ApiResponse.success("Webhook received.", null));
    }
}
