package com.badminton.shop.modules.order.service;

import com.badminton.shop.modules.order.dto.request.CreateReturnRequest;
import com.badminton.shop.modules.order.dto.request.ReceiveReturnRequest;
import com.badminton.shop.modules.order.dto.request.CreateOrderRequest;
import com.badminton.shop.modules.order.dto.response.CheckoutContextResponse;
import com.badminton.shop.modules.order.dto.response.OrderPreviewResponse;
import com.badminton.shop.modules.order.dto.response.OrderResponse;
import com.badminton.shop.modules.order.dto.response.ReturnRequestResponse;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface OrderService {

	CheckoutContextResponse getCheckoutContext(String principalName);

	OrderPreviewResponse previewOrder(String principalName, CreateOrderRequest request);

	OrderResponse purchase(String principalName, CreateOrderRequest request);

	Map<String, String> handleVnpayReturn(Map<String, String> params);

	Map<String, String> handleVnpayIpn(Map<String, String> params);

	Page<OrderResponse> getMyOrders(String principalName, int page, int size);

	OrderResponse cancelOrderByUser(String principalName, String orderCode, String reason);

	OrderResponse confirmCodOrder(String orderCode, String adminName, String note);

	int autoCancelExpiredPendingVnpayOrders();

	ReturnRequestResponse createReturnRequest(String principalName, String orderCode, CreateReturnRequest request);

	ReturnRequestResponse approveReturnRequest(Long returnRequestId, String adminName, String note);

	ReturnRequestResponse rejectReturnRequest(Long returnRequestId, String adminName, String note);

	ReturnRequestResponse receiveReturnedItems(Long returnRequestId, String adminName, ReceiveReturnRequest request);

	ReturnRequestResponse markReturnRefunded(Long returnRequestId, String adminName, String note);

    Page<OrderResponse> adminGetOrders(
            String keyword,
            String status,
            String paymentStatus,
            String paymentMethod,
            java.time.LocalDateTime from,
            java.time.LocalDateTime to,
            int page,
            int size
    );

    OrderResponse adminGetOrder(String orderCode);

    OrderResponse adminUpdateOrderStatus(String orderCode, String status, String note, String adminName);

    OrderResponse adminAssignShipping(String orderCode, String shippingCode, String shippingProvider, java.time.LocalDateTime expectedDeliveryAt, String adminName);

    Page<ReturnRequestResponse> adminGetReturns(String keyword, String status, int page, int size);

    java.util.Map<String, Long> adminGetReturnStats();
}
