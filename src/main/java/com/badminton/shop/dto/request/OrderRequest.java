package com.badminton.shop.dto.request;

import com.badminton.shop.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequest {

    @NotEmpty(message = "Giỏ hàng không được trống")
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    private String receiverPhone;

    @NotBlank(message = "Tên người nhận không được để trống")
    private String receiverName;

    private String note;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;
}
