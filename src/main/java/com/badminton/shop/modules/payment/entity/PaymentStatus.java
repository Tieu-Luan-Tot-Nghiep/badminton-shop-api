package com.badminton.shop.modules.payment.entity;

public enum PaymentStatus {
    PENDING,     // Chưa thanh toán
    PAID,        // Đã thanh toán
    FAILED,      // Thanh toán thất bại
    REFUNDED     // Đã hoàn tiền
}
