package com.badminton.shop.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDataIntegrityViolationException_ReturnsConflictForPhoneNumber() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Key (phone_number)=(0377314202) already exists.");

        var response = handler.handleDataIntegrityViolationException(exception);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Phone number is already in use", response.getBody().getMessage());
        assertEquals("error", response.getBody().getStatus());
    }
}