package com.badminton.shop.modules.shipping.service;

import com.badminton.shop.modules.shipping.dto.request.ShippingWebhookRequest;

public interface ShippingWebhookService {

    void handleWebhook(ShippingWebhookRequest request);
}
