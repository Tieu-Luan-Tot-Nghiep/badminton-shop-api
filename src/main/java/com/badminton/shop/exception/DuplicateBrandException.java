package com.badminton.shop.exception;

public class DuplicateBrandException extends RuntimeException {
    public DuplicateBrandException(String message) {
        super(message);
    }
}
