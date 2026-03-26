package com.badminton.shop.modules.shipping.exception;

public class ShippingIntegrationException extends RuntimeException {

    public ShippingIntegrationException(String message) {
        super(message);
    }

    public ShippingIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
