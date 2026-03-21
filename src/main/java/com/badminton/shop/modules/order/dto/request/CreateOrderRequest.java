package com.badminton.shop.modules.order.dto.request;

import com.badminton.shop.modules.order.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

	@NotEmpty(message = "Danh sách sản phẩm không được để trống")
	@Valid
	@Builder.Default
	private List<OrderLineRequest> items = new ArrayList<>();

	// Nếu có addressId, hệ thống sẽ lấy snapshot receiver từ sổ địa chỉ.
	private Long addressId;

	// Fallback cho trường hợp user muốn nhập địa chỉ thủ công.
	private String receiverName;

	private String receiverPhone;

	private String shippingAddress;

	// Placeholder cho module Promotion trong tương lai.
	private String voucherCode;

	private String orderNote;

	@NotNull(message = "Phương thức thanh toán không được để trống")
	private PaymentMethod paymentMethod;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OrderLineRequest {
		@NotNull(message = "variantId không được để trống")
		private Long variantId;

		@NotNull(message = "quantity không được để trống")
		@Min(value = 1, message = "quantity phải lớn hơn hoặc bằng 1")
		private Integer quantity;
	}
}
