package com.badminton.shop.exception;

public class CategoryHasChildrenException extends RuntimeException {
    public CategoryHasChildrenException(String message) {
        super(message);
    }
}
