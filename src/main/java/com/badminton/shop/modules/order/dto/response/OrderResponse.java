package com.badminton.shop.modules.order.dto.response;

import com.badminton.shop.modules.order.entity.OrderStatus;
import com.badminton.shop.modules.order.entity.PaymentMethod;
import com.badminton.shop.modules.order.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
	private Long id;
	private String orderCode;
	private String receiverName;
	private String receiverPhone;
	private String shippingAddress;
	private String orderNote;
	private OrderStatus status;
	private PaymentMethod paymentMethod;
	private PaymentStatus paymentStatus;
	private String voucherCode;
	private BigDecimal discountAmount;
	private BigDecimal itemsAmount;
	private BigDecimal shippingFee;
	private BigDecimal totalAmount;
	private String paymentUrl;
	private LocalDateTime createdAt;

	@Builder.Default
	private List<OrderLineResponse> items = new ArrayList<>();

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OrderLineResponse {
		private Long variantId;
		private String productName;
		private String sku;
		private Integer quantity;
		private BigDecimal unitPrice;
		private BigDecimal lineAmount;
	}
}
