package com.badminton.shop.modules.order.entity;

public enum OrderStatus {
    PENDING,      // Chờ xác nhận
    CONFIRMED,    // Đã xác nhận
    PROCESSING,   // Đang xử lý (Đóng gói / Căng cước vợt)
    SHIPPING,     // Đang giao hàng
    DELIVERED,    // Đã giao hàng thành công
    CANCELLED,    // Đã hủy
    RETURNED      // Trả hàng / Hoàn tiền
}
