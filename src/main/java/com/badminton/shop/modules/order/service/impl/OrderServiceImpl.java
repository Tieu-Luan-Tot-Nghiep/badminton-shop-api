package com.badminton.shop.modules.order.service.impl;

import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.auth.entity.UserAddress;
import com.badminton.shop.modules.auth.repository.UserAddressRepository;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.inventory.dto.StockInRequest;
import com.badminton.shop.modules.order.dto.request.CreateOrderRequest;
import com.badminton.shop.modules.order.dto.request.CreateReturnRequest;
import com.badminton.shop.modules.order.dto.request.ReceiveReturnRequest;
import com.badminton.shop.modules.order.dto.response.CheckoutAddressResponse;
import com.badminton.shop.modules.order.dto.response.CheckoutContextResponse;
import com.badminton.shop.modules.order.dto.response.OrderPreviewResponse;
import com.badminton.shop.modules.order.dto.response.OrderResponse;
import com.badminton.shop.modules.order.dto.response.ReturnRequestResponse;
import com.badminton.shop.modules.inventory.dto.InventoryLineRequest;
import com.badminton.shop.modules.inventory.dto.SystemInventoryRequest;
import com.badminton.shop.modules.inventory.service.InventoryService;
import com.badminton.shop.modules.messaging.dto.OrderCancelledEvent;
import com.badminton.shop.modules.messaging.dto.OrderCancellationEmailMessage;
import com.badminton.shop.modules.messaging.dto.OrderConfirmationEmailMessage;
import com.badminton.shop.modules.messaging.dto.RefundRequiredMessage;
import com.badminton.shop.modules.order.entity.Order;
import com.badminton.shop.modules.order.entity.OrderHistory;
import com.badminton.shop.modules.order.entity.OrderItem;
import com.badminton.shop.modules.order.entity.OrderReturnItem;
import com.badminton.shop.modules.order.entity.OrderReturnRequest;
import com.badminton.shop.modules.order.entity.OrderStatus;
import com.badminton.shop.modules.order.entity.PaymentMethod;
import com.badminton.shop.modules.order.entity.PaymentStatus;
import com.badminton.shop.modules.order.entity.ReturnItemAction;
import com.badminton.shop.modules.order.entity.ReturnRequestStatus;
import com.badminton.shop.modules.order.repository.OrderItemRepository;
import com.badminton.shop.modules.order.repository.OrderReturnRequestRepository;
import com.badminton.shop.modules.order.repository.OrderRepository;
import com.badminton.shop.modules.order.service.OrderService;
import com.badminton.shop.modules.promotion.dto.response.PromotionApplyResult;
import com.badminton.shop.modules.promotion.service.PromotionService;
import com.badminton.shop.modules.membership.service.MembershipService;
import com.badminton.shop.modules.product.entity.Product;
import com.badminton.shop.modules.product.entity.ProductVariant;
import com.badminton.shop.modules.product.repository.ProductVariantRepository;
import com.badminton.shop.modules.shipping.dto.request.ShippingFeeCalculationRequest;
import com.badminton.shop.modules.shipping.dto.request.ShippingOrderCreationRequest;
import com.badminton.shop.modules.shipping.dto.response.ShippingOrderResponse;
import com.badminton.shop.modules.shipping.service.ShippingService;
import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.utils.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderReturnRequestRepository orderReturnRequestRepository;
    private final PromotionService promotionService;
    private final MembershipService membershipService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    private final RabbitTemplate rabbitTemplate;
    private final EmailService emailService;

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.url}")
    private String vnpPayUrl;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    @Value("${app.order.vnpay.expire-minutes:15}")
    private long vnpayExpireMinutes;

    @Override
        @Transactional(readOnly = true)
        public CheckoutContextResponse getCheckoutContext(String principalName) {
        User user = getUserByPrincipal(principalName);
        List<UserAddress> addresses = userAddressRepository.findAllByUserId(user.getId());

        List<CheckoutAddressResponse> mapped = addresses.stream()
            .map(this::toCheckoutAddress)
            .toList();

        CheckoutAddressResponse defaultAddress = mapped.stream()
            .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
            .findFirst()
            .orElse(mapped.isEmpty() ? null : mapped.get(0));

        return CheckoutContextResponse.builder()
            .addresses(mapped)
            .defaultAddress(defaultAddress)
            .build();
        }

        @Override
    @Transactional(readOnly = true)
    public OrderPreviewResponse previewOrder(String principalName, CreateOrderRequest request) {
        User user = getUserByPrincipal(principalName);
        ShippingSnapshot shipping = resolveShippingSnapshot(user, request);
        BigDecimal shippingFee = calculateActualShippingFee(shipping.address(), request.getItems());
        PricingResult pricing = calculatePricing(request.getItems(), request.getVoucherCode(), shippingFee, user.getId());

        return OrderPreviewResponse.builder()
                .items(pricing.items())
            .voucherCode(pricing.voucherCode())
            .discountAmount(pricing.discountAmount())
                .itemsAmount(pricing.itemsAmount())
                .shippingFee(pricing.shippingFee())
                .totalAmount(pricing.totalAmount())
                .build();
    }

    @Override
    public OrderResponse purchase(String principalName, CreateOrderRequest request) {
        User user = getUserByPrincipal(principalName);
        ShippingSnapshot shipping = resolveShippingSnapshot(user, request);
        BigDecimal shippingFee = calculateActualShippingFee(shipping.address(), request.getItems());
        PricingResult pricing = calculatePricing(request.getItems(), request.getVoucherCode(), shippingFee, user.getId());

        String orderCode = generateOrderCode();
        PaymentMethod paymentMethod = request.getPaymentMethod();
        PaymentStatus paymentStatus = paymentMethod == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.PENDING;
        OrderStatus initialStatus = paymentMethod == PaymentMethod.VNPAY ? OrderStatus.AWAITING_PAYMENT : OrderStatus.PENDING;

        Order order = Order.builder()
                .orderCode(orderCode)
            .receiverName(shipping.receiverName())
            .receiverPhone(shipping.receiverPhone())
            .shippingAddress(shipping.shippingAddress())
                .orderNote(request.getOrderNote())
                .itemsAmount(pricing.itemsAmount().doubleValue())
                .shippingFee(pricing.shippingFee().doubleValue())
                .totalAmount(pricing.totalAmount().doubleValue())
                .discountAmount(pricing.discountAmount().doubleValue())
                .status(initialStatus)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)
                .shippingProvider("GHN")
                .user(user)
                .promotion(pricing.promotion())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        for (CreateOrderRequest.OrderLineRequest line : request.getItems()) {
            ProductVariant variant = productVariantRepository.findById(line.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy biến thể sản phẩm với id: " + line.getVariantId()));

            validateStock(variant, line.getQuantity());

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .variant(variant)
                    .quantity(line.getQuantity())
                    .priceAtPurchase(variant.getPrice())
                    .build();
            orderItems.add(item);
        }

        OrderHistory history = OrderHistory.builder()
                .order(order)
            .status(initialStatus)
                .note("Đơn hàng được tạo")
                .build();

        order.getItems().addAll(orderItems);
        order.getHistories().add(history);

        Order saved = orderRepository.save(order);

    inventoryService.reserveInventory(buildSystemInventoryRequest(
        saved,
        "Reserve inventory for pending order"
    ));

        if (saved.getPromotion() != null) {
            promotionService.publishPromotionUsage(saved.getPromotion().getId(), saved.getOrderCode());
        }

        String paymentUrl = null;
        if (paymentMethod == PaymentMethod.VNPAY) {
            paymentUrl = buildVnpayPaymentUrl(saved);
        }

        if (saved.getStatus() == OrderStatus.CONFIRMED) {
            createShippingOrderIfApplicable(saved);
            saved = orderRepository.save(saved);
        }

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EMAIL_EXCHANGE,
                    RabbitMQConfig.ORDER_CONFIRMATION_EMAIL_ROUTING_KEY,
                    OrderConfirmationEmailMessage.builder()
                            .email(user.getEmail())
                            .orderCode(saved.getOrderCode())
                            .customerName(saved.getReceiverName())
                            .totalAmount(saved.getTotalAmount())
                            .paymentMethod(saved.getPaymentMethod().name())
                            .build()
            );
        } catch (Exception e) {
            // Ignore email failure
        }
        
        return toOrderResponse(saved, paymentUrl);
    }

    @Override
    public Map<String, String> handleVnpayReturn(Map<String, String> params) {
        VnpayProcessResult result = processVnpayTransaction(params);

        Map<String, String> response = new HashMap<>();
        if (result.order() != null) {
            response.put("orderCode", result.order().getOrderCode());
            response.put("paymentStatus", result.order().getPaymentStatus().name());
            response.put("orderStatus", result.order().getStatus().name());
        }
        response.put("responseCode", result.responseCode());
        response.put("message", result.message());
        return response;
    }

    @Override
    public Map<String, String> handleVnpayIpn(Map<String, String> params) {
        try {
            VnpayProcessResult result = processVnpayTransaction(params);

            if (!"00".equals(result.responseCode())) {
                return Map.of("RspCode", result.responseCode(), "Message", result.message());
            }
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        } catch (IllegalArgumentException ex) {
            return Map.of("RspCode", "97", "Message", "Invalid Signature");
        } catch (ResourceNotFoundException ex) {
            return Map.of("RspCode", "01", "Message", "Order Not Found");
        } catch (Exception ex) {
            return Map.of("RspCode", "99", "Message", "Unknown Error");
        }
    }

    private VnpayProcessResult processVnpayTransaction(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            throw new IllegalArgumentException("Thiếu chữ ký vnp_SecureHash");
        }

        if (!isValidVnpSignature(params, receivedHash)) {
            throw new IllegalArgumentException("Chữ ký VNPAY không hợp lệ");
        }

        String orderCode = params.get("vnp_TxnRef");
        if (orderCode == null || orderCode.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã đơn hàng vnp_TxnRef");
        }

        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode));

        String responseCode = params.getOrDefault("vnp_ResponseCode", "99");
        if ("00".equals(responseCode)) {
            if (order.getPaymentStatus() != PaymentStatus.COMPLETED) {
            inventoryService.commitInventory(buildSystemInventoryRequest(
                order,
                "Commit inventory after payment completed"
            ));

                order.setPaymentStatus(PaymentStatus.COMPLETED);
                order.setStatus(OrderStatus.CONFIRMED);
                order.getHistories().add(OrderHistory.builder()
                        .order(order)
                        .status(OrderStatus.CONFIRMED)
                        .note("Thanh toán VNPAY thành công")
                        .build());

                try {
                    membershipService.addPointsFromOrder(
                            order.getUser().getId(),
                            BigDecimal.valueOf(order.getTotalAmount()),
                            order.getId()
                    );
                } catch (RuntimeException ignored) {
                    // Loyalty failure should not block payment success flow.
                }

                createShippingOrderIfApplicable(order);
            }
        } else {
            if (order.getPaymentStatus() == PaymentStatus.PENDING && order.getStatus() != OrderStatus.CANCELLED) {
                cancelOrderInternal(order, "VNPAY failed with code: " + responseCode, "system");
            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.getHistories().add(OrderHistory.builder()
                        .order(order)
                        .status(order.getStatus())
                        .note("Thanh toán VNPAY thất bại, mã lỗi: " + responseCode)
                        .build());
            }
        }

        Order saved = orderRepository.save(order);
        String message = "00".equals(responseCode) ? "Transaction Success" : "Transaction Failed";
        return new VnpayProcessResult(saved, responseCode, message);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(String principalName, int page, int size) {
        User user = getUserByPrincipal(principalName);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        return orderRepository.findAllByUserId(user.getId(), pageable)
                .map(order -> toOrderResponse(order, null));
    }

    @Override
    public OrderResponse cancelOrderByUser(String principalName, String orderCode, String reason) {
        User user = getUserByPrincipal(principalName);
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền hủy đơn hàng này.");
        }

        cancelOrderInternal(order, reason, user.getEmail());
        Order saved = orderRepository.save(order);
        return toOrderResponse(saved, null);
    }

    @Override
    public OrderResponse confirmCodOrder(String orderCode, String adminName, String note) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode));

        if (order.getPaymentMethod() != PaymentMethod.COD) {
            throw new IllegalArgumentException("Chỉ hỗ trợ xác nhận cho đơn COD.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ xác nhận được đơn COD ở trạng thái PENDING.");
        }

        inventoryService.commitInventory(buildSystemInventoryRequest(order, "Commit inventory after COD confirmation"));

        order.setStatus(OrderStatus.CONFIRMED);
        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .status(OrderStatus.CONFIRMED)
                .note("Admin xác nhận đơn COD: " + safe(note) + " (" + adminName + ")")
                .build());

        createShippingOrderIfApplicable(order);
        Order saved = orderRepository.save(order);
        return toOrderResponse(saved, null);
    }

    @Override
    public int autoCancelExpiredPendingVnpayOrders() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(vnpayExpireMinutes);

        List<Order> expiredOrders = orderRepository.findAllByPaymentMethodAndPaymentStatusAndStatusInAndCreatedAtBefore(
                PaymentMethod.VNPAY,
                PaymentStatus.PENDING,
                List.of(OrderStatus.AWAITING_PAYMENT, OrderStatus.PENDING, OrderStatus.CONFIRMED),
                expiredBefore
        );

        int affected = 0;
        for (Order order : expiredOrders) {
            cancelOrderInternal(order, "Auto-cancel: VNPAY timeout after " + vnpayExpireMinutes + " minutes", "system");
            orderRepository.save(order);
            affected++;
        }

        return affected;
    }

    @Override
    public ReturnRequestResponse createReturnRequest(String principalName, String orderCode, CreateReturnRequest request) {
        User user = getUserByPrincipal(principalName);
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền tạo yêu cầu trả hàng cho đơn này.");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Chỉ đơn hàng đã giao thành công mới được yêu cầu trả hàng.");
        }
        if (orderReturnRequestRepository.existsByOrderIdAndStatusIn(
                order.getId(),
                List.of(ReturnRequestStatus.REQUESTED, ReturnRequestStatus.AWAITING_RETURN, ReturnRequestStatus.RECEIVED)
        )) {
            throw new IllegalArgumentException("Đơn hàng đã có yêu cầu trả hàng đang xử lý.");
        }

        Map<Long, OrderItem> orderItemsById = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, i -> i));

        OrderReturnRequest returnRequest = OrderReturnRequest.builder()
                .order(order)
                .user(user)
                .status(ReturnRequestStatus.REQUESTED)
                .reason(request.getReason().trim())
                .refundMethod(request.getRefundMethod())
                .bankAccountName(request.getBankAccountName())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankName(request.getBankName())
                .evidenceUrls(joinEvidence(request.getEvidenceUrls()))
                .build();

        for (CreateReturnRequest.ReturnItemRequest returnItemRequest : request.getItems()) {
            OrderItem orderItem = orderItemsById.get(returnItemRequest.getOrderItemId());
            if (orderItem == null) {
                throw new IllegalArgumentException("orderItemId không thuộc đơn hàng: " + returnItemRequest.getOrderItemId());
            }
            if (returnItemRequest.getQuantity() > orderItem.getQuantity()) {
                throw new IllegalArgumentException("Số lượng trả vượt quá số lượng đã mua cho orderItemId=" + orderItem.getId());
            }

            returnRequest.getItems().add(OrderReturnItem.builder()
                    .returnRequest(returnRequest)
                    .orderItem(orderItem)
                    .requestedQuantity(returnItemRequest.getQuantity())
                    .receivedQuantity(0)
                    .build());
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .status(OrderStatus.RETURN_REQUESTED)
                .note("Khách hàng gửi yêu cầu trả hàng")
                .build());

        OrderReturnRequest saved = orderReturnRequestRepository.save(returnRequest);
        orderRepository.save(order);
        return toReturnRequestResponse(saved);
    }

    @Override
    public ReturnRequestResponse approveReturnRequest(Long returnRequestId, String adminName, String note) {
        OrderReturnRequest returnRequest = getReturnRequestOrThrow(returnRequestId);
        if (returnRequest.getStatus() != ReturnRequestStatus.REQUESTED) {
            throw new IllegalArgumentException("Yêu cầu trả hàng không ở trạng thái có thể duyệt.");
        }

        returnRequest.setStatus(ReturnRequestStatus.AWAITING_RETURN);
        returnRequest.setAdminNote(note);

        Order order = returnRequest.getOrder();
        order.setStatus(OrderStatus.AWAITING_RETURN);
        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .status(OrderStatus.AWAITING_RETURN)
                .note("Admin duyệt yêu cầu trả hàng: " + safe(note) + " (" + adminName + ")")
                .build());

        OrderReturnRequest saved = orderReturnRequestRepository.save(returnRequest);
        orderRepository.save(order);
        return toReturnRequestResponse(saved);
    }

    @Override
    public ReturnRequestResponse rejectReturnRequest(Long returnRequestId, String adminName, String note) {
        if (!hasText(note)) {
            throw new IllegalArgumentException("Vui lòng cung cấp lý do từ chối trả hàng.");
        }

        OrderReturnRequest returnRequest = getReturnRequestOrThrow(returnRequestId);
        if (returnRequest.getStatus() != ReturnRequestStatus.REQUESTED) {
            throw new IllegalArgumentException("Yêu cầu trả hàng không ở trạng thái có thể từ chối.");
        }

        returnRequest.setStatus(ReturnRequestStatus.REJECTED);
        returnRequest.setAdminNote(note);

        Order order = returnRequest.getOrder();
        order.setStatus(OrderStatus.DELIVERED);
        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .status(OrderStatus.DELIVERED)
                .note("Yêu cầu trả hàng bị từ chối: " + note + " (" + adminName + ")")
                .build());

        OrderReturnRequest saved = orderReturnRequestRepository.save(returnRequest);
        orderRepository.save(order);
        return toReturnRequestResponse(saved);
    }

    @Override
    public ReturnRequestResponse receiveReturnedItems(Long returnRequestId, String adminName, ReceiveReturnRequest request) {
        OrderReturnRequest returnRequest = getReturnRequestOrThrow(returnRequestId);
        if (returnRequest.getStatus() != ReturnRequestStatus.AWAITING_RETURN) {
            throw new IllegalArgumentException("Yêu cầu trả hàng chưa ở trạng thái chờ nhận hàng hoàn.");
        }

        Map<Long, OrderReturnItem> returnItemsByOrderItemId = returnRequest.getItems().stream()
                .collect(Collectors.toMap(item -> item.getOrderItem().getId(), i -> i));

        for (ReceiveReturnRequest.ReceiveItem receiveItem : request.getItems()) {
            OrderReturnItem returnItem = returnItemsByOrderItemId.get(receiveItem.getOrderItemId());
            if (returnItem == null) {
                throw new IllegalArgumentException("orderItemId không thuộc yêu cầu trả hàng: " + receiveItem.getOrderItemId());
            }
            if (receiveItem.getQuantity() > returnItem.getRequestedQuantity()) {
                throw new IllegalArgumentException("Số lượng nhận vượt quá số lượng yêu cầu trả cho orderItemId=" + receiveItem.getOrderItemId());
            }

            returnItem.setReceivedQuantity(receiveItem.getQuantity());
            returnItem.setAction(receiveItem.getAction());

            if (receiveItem.getAction() == ReturnItemAction.RESTOCK) {
                ProductVariant variant = returnItem.getOrderItem().getVariant();
                inventoryService.stockIn(adminName, StockInRequest.builder()
                        .variantId(variant.getId())
                        .quantity(receiveItem.getQuantity())
                        .note("Return restock for order " + returnRequest.getOrder().getOrderCode())
                        .build());
            }
        }

        returnRequest.setStatus(ReturnRequestStatus.RECEIVED);
        returnRequest.setAdminNote(request.getNote());

        Order order = returnRequest.getOrder();
        order.setStatus(OrderStatus.RETURN_RECEIVED);
        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .status(OrderStatus.RETURN_RECEIVED)
                .note("Kho đã nhận hàng hoàn: " + safe(request.getNote()) + " (" + adminName + ")")
                .build());

        OrderReturnRequest saved = orderReturnRequestRepository.save(returnRequest);
        orderRepository.save(order);
        return toReturnRequestResponse(saved);
    }

    @Override
    public ReturnRequestResponse markReturnRefunded(Long returnRequestId, String adminName, String note) {
        OrderReturnRequest returnRequest = getReturnRequestOrThrow(returnRequestId);
        if (returnRequest.getStatus() != ReturnRequestStatus.RECEIVED) {
            throw new IllegalArgumentException("Yêu cầu trả hàng chưa ở trạng thái có thể hoàn tiền.");
        }

        returnRequest.setStatus(ReturnRequestStatus.REFUNDED);
        returnRequest.setAdminNote(note);

        Order order = returnRequest.getOrder();
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setStatus(OrderStatus.REFUNDED);
        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .status(OrderStatus.REFUNDED)
                .note("Đã hoàn tiền cho đơn trả hàng: " + safe(note) + " (" + adminName + ")")
                .build());

        OrderReturnRequest saved = orderReturnRequestRepository.save(returnRequest);
        orderRepository.save(order);

        try {
            membershipService.rollbackPointsFromOrder(order.getUser().getId(), order.getId(), "refund");
        } catch (RuntimeException ignored) {
            // Refund flow should not fail due to loyalty rollback issue.
        }

        return toReturnRequestResponse(saved);
    }

        private PricingResult calculatePricing(
            List<CreateOrderRequest.OrderLineRequest> lines,
            String voucherCode,
            BigDecimal baseShippingFee,
            Long userId) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Danh sách sản phẩm không được để trống");
        }

        List<OrderResponse.OrderLineResponse> previewItems = new ArrayList<>();
        BigDecimal itemsAmount = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderLineRequest line : lines) {
            ProductVariant variant = productVariantRepository.findById(line.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy biến thể sản phẩm với id: " + line.getVariantId()));

            validateStock(variant, line.getQuantity());

            BigDecimal unitPrice = BigDecimal.valueOf(variant.getPrice());
            BigDecimal lineAmount = unitPrice.multiply(BigDecimal.valueOf(line.getQuantity()));
            itemsAmount = itemsAmount.add(lineAmount);

            Product product = variant.getProduct();
            previewItems.add(OrderResponse.OrderLineResponse.builder()
                    .variantId(variant.getId())
                    .productName(product != null ? product.getName() : null)
                    .sku(variant.getSku())
                    .quantity(line.getQuantity())
                    .unitPrice(unitPrice)
                    .lineAmount(lineAmount)
                    .build());
        }

        PromotionApplyResult promotionResult = promotionService.applyPromotionForOrder(voucherCode, itemsAmount, baseShippingFee);

        // Membership discount
        BigDecimal membershipDiscountPercent = membershipService.getDiscountPercentage(userId);
        BigDecimal tierDiscount = itemsAmount.multiply(membershipDiscountPercent).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        BigDecimal finalDiscount = promotionResult.getDiscountAmount().add(tierDiscount);
        BigDecimal newFinalTotal = promotionResult.getFinalTotalAmount().subtract(tierDiscount);
        if (newFinalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalDiscount = finalDiscount.add(newFinalTotal); // adjust finalDiscount
            newFinalTotal = BigDecimal.ZERO;
        }

        return new PricingResult(
            previewItems,
            promotionResult.getPromotion(),
            promotionResult.getVoucherCode(),
            itemsAmount,
            finalDiscount,
            promotionResult.getFinalShippingFee(),
            newFinalTotal
        );
    }

    private OrderResponse toOrderResponse(Order order, String paymentUrl) {
        List<OrderResponse.OrderLineResponse> lines = order.getItems().stream()
                .map(item -> {
                    ProductVariant variant = item.getVariant();
                    Product product = variant != null ? variant.getProduct() : null;
                    BigDecimal unitPrice = BigDecimal.valueOf(item.getPriceAtPurchase());
                    BigDecimal lineAmount = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                    return OrderResponse.OrderLineResponse.builder()
                            .variantId(variant != null ? variant.getId() : null)
                            .productName(product != null ? product.getName() : null)
                            .sku(variant != null ? variant.getSku() : null)
                            .quantity(item.getQuantity())
                            .unitPrice(unitPrice)
                            .lineAmount(lineAmount)
                            .build();
                })
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingAddress(order.getShippingAddress())
                .orderNote(order.getOrderNote())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .voucherCode(order.getPromotion() != null ? order.getPromotion().getCode() : null)
                .discountAmount(BigDecimal.valueOf(order.getDiscountAmount() == null ? 0.0d : order.getDiscountAmount()))
                .itemsAmount(BigDecimal.valueOf(order.getItemsAmount()))
                .shippingFee(BigDecimal.valueOf(order.getShippingFee()))
                .totalAmount(BigDecimal.valueOf(order.getTotalAmount()))
                .shippingCode(order.getShippingCode())
                .shippingProvider(order.getShippingProvider())
                .shippingExpectedDeliveryAt(order.getShippingExpectedDeliveryAt())
                .paymentUrl(paymentUrl)
                .createdAt(order.getCreatedAt())
                .items(lines)
                .build();
    }

            private BigDecimal calculateActualShippingFee(UserAddress shippingAddress, List<CreateOrderRequest.OrderLineRequest> lines) {
            List<ShippingFeeCalculationRequest.ShippingItemRequest> shippingItems = buildShippingItems(lines);
            BigDecimal insuranceValue = shippingItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            return shippingService.calculateShippingFee(ShippingFeeCalculationRequest.builder()
                .address(shippingAddress)
                .insuranceValue(insuranceValue)
                .items(shippingItems)
                .build()).setScale(0, RoundingMode.HALF_UP);
            }

            private List<ShippingFeeCalculationRequest.ShippingItemRequest> buildShippingItems(
                List<CreateOrderRequest.OrderLineRequest> lines) {
            List<ShippingFeeCalculationRequest.ShippingItemRequest> shippingItems = new ArrayList<>();

            for (CreateOrderRequest.OrderLineRequest line : lines) {
                ProductVariant variant = productVariantRepository.findById(line.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy biến thể sản phẩm với id: " + line.getVariantId()));

                Product product = variant.getProduct();
                shippingItems.add(ShippingFeeCalculationRequest.ShippingItemRequest.builder()
                    .name(product != null ? product.getName() : variant.getSku())
                    .sku(variant.getSku())
                    .quantity(line.getQuantity())
                    .unitPrice(BigDecimal.valueOf(variant.getPrice()))
                    .weightGrams(variant.getShippingWeightGrams())
                    .lengthCm(variant.getShippingLengthCm())
                    .widthCm(variant.getShippingWidthCm())
                    .heightCm(variant.getShippingHeightCm())
                    .build());
            }

            return shippingItems;
            }

    private String buildVnpayPaymentUrl(Order order) {
        String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(BigDecimal.valueOf(order.getTotalAmount()).multiply(BigDecimal.valueOf(100)).longValue()));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", order.getOrderCode());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
        vnpParams.put("vnp_IpAddr", "127.0.0.1");
        vnpParams.put("vnp_CreateDate", createDate);

        String hashData = buildQueryString(vnpParams, false);
        String secureHash = hmacSHA512(vnpHashSecret, hashData);

        return UriComponentsBuilder.fromHttpUrl(vnpPayUrl)
                .query(buildQueryString(vnpParams, true))
                .queryParam("vnp_SecureHash", secureHash)
                .build(true)
                .toUriString();
    }

    private boolean isValidVnpSignature(Map<String, String> params, String receivedHash) {
        Map<String, String> signParams = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }
            if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) {
                continue;
            }
            signParams.put(key, value);
        }

        String hashData = buildQueryString(signParams, false);
        String calculatedHash = hmacSHA512(vnpHashSecret, hashData);
        return calculatedHash.equalsIgnoreCase(receivedHash);
    }

    private String buildQueryString(Map<String, String> params, boolean urlEncode) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (index++ > 0) {
                sb.append('&');
            }

            String key = urlEncode
                    ? URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII)
                    : entry.getKey();
            String value = urlEncode
                    ? URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII)
                    : entry.getValue();

            sb.append(key).append('=').append(value);
        }
        return sb.toString();
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKeySpec);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            return hash.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký VNPAY", e);
        }
    }

    private void validateStock(ProductVariant variant, int quantity) {
        Integer stock = variant.getStock();
        if (stock == null || stock < quantity) {
            throw new IllegalArgumentException(
                    "Biến thể " + variant.getSku() + " không đủ tồn kho. Hiện còn: " + (stock == null ? 0 : stock));
        }
    }

    private User getUserByPrincipal(String principalName) {
        return userRepository.findByEmail(principalName)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với email: " + principalName));
    }

    private ShippingSnapshot resolveShippingSnapshot(User user, CreateOrderRequest request) {
        Long addressId = request.getAddressId();

        if (addressId != null) {
            UserAddress selected = userAddressRepository.findById(addressId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ với id: " + addressId));

            if (!selected.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Địa chỉ không thuộc về tài khoản hiện tại");
            }

            return new ShippingSnapshot(
                    selected.getReceiverName(),
                    selected.getPhoneNumber(),
                    fullAddress(selected),
                    selected
            );
        }

        if (hasText(request.getReceiverName()) && hasText(request.getReceiverPhone()) && hasText(request.getShippingAddress())) {
                UserAddress manualAddress = parseManualAddress(
                    request.getReceiverName(),
                    request.getReceiverPhone(),
                    request.getShippingAddress());

            return new ShippingSnapshot(
                    request.getReceiverName().trim(),
                    request.getReceiverPhone().trim(),
                    request.getShippingAddress().trim(),
                    manualAddress
            );
        }

        UserAddress defaultAddress = userAddressRepository.findAllByUserId(user.getId()).stream()
                .filter(address -> Boolean.TRUE.equals(address.getIsDefault()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy địa chỉ mặc định. Vui lòng chọn addressId hoặc cung cấp địa chỉ giao hàng"));

        return new ShippingSnapshot(
                defaultAddress.getReceiverName(),
                defaultAddress.getPhoneNumber(),
            fullAddress(defaultAddress),
            defaultAddress
        );
    }

        private UserAddress parseManualAddress(String receiverName, String receiverPhone, String shippingAddress) {
        String[] parts = Arrays.stream(shippingAddress.split(","))
            .map(String::trim)
            .filter(this::hasText)
            .toArray(String[]::new);

        if (parts.length < 4) {
            throw new IllegalArgumentException(
                "Địa chỉ giao hàng phải có đủ dạng: so nha, phuong/xa, quan/huyen, tinh/thanh pho.");
        }

        int n = parts.length;
        return UserAddress.builder()
            .receiverName(receiverName.trim())
            .phoneNumber(receiverPhone.trim())
            .specificAddress(String.join(", ", Arrays.copyOfRange(parts, 0, n - 3)))
            .ward(parts[n - 3])
            .district(parts[n - 2])
            .province(parts[n - 1])
            .isDefault(false)
            .build();
        }

    private CheckoutAddressResponse toCheckoutAddress(UserAddress address) {
        return CheckoutAddressResponse.builder()
                .id(address.getId())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getPhoneNumber())
                .shippingAddress(fullAddress(address))
                .isDefault(Boolean.TRUE.equals(address.getIsDefault()))
                .build();
    }

    private String fullAddress(UserAddress address) {
        return String.format("%s, %s, %s, %s",
                safe(address.getSpecificAddress()),
                safe(address.getWard()),
                safe(address.getDistrict()),
                safe(address.getProvince()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String generateOrderCode() {
        return "ORD-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) +
                "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

        private void createShippingOrderIfApplicable(Order order) {
        if (order.getStatus() != OrderStatus.CONFIRMED || hasText(order.getShippingCode())) {
            return;
        }

        UserAddress shipmentAddress = parseManualAddress(
            order.getReceiverName(),
            order.getReceiverPhone(),
            order.getShippingAddress());

        List<ShippingFeeCalculationRequest.ShippingItemRequest> shippingItems = order.getItems().stream()
            .map(item -> {
                ProductVariant variant = item.getVariant();
                Product product = variant != null ? variant.getProduct() : null;
                return ShippingFeeCalculationRequest.ShippingItemRequest.builder()
                    .name(product != null ? product.getName() : (variant != null ? variant.getSku() : "item"))
                    .sku(variant != null ? variant.getSku() : null)
                    .quantity(item.getQuantity())
                    .unitPrice(BigDecimal.valueOf(item.getPriceAtPurchase()))
                    .weightGrams(variant != null ? variant.getShippingWeightGrams() : null)
                    .lengthCm(variant != null ? variant.getShippingLengthCm() : null)
                    .widthCm(variant != null ? variant.getShippingWidthCm() : null)
                    .heightCm(variant != null ? variant.getShippingHeightCm() : null)
                    .build();
            })
            .toList();

        BigDecimal orderTotal = BigDecimal.valueOf(order.getTotalAmount() == null ? 0d : order.getTotalAmount());
        BigDecimal codAmount = order.getPaymentMethod() == PaymentMethod.COD
            ? orderTotal.setScale(0, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        ShippingFeeCalculationRequest feeRequest = ShippingFeeCalculationRequest.builder()
            .address(shipmentAddress)
            .insuranceValue(orderTotal)
            .items(shippingItems)
            .build();

        ShippingOrderResponse shippingOrder = shippingService.createShippingOrder(
            ShippingOrderCreationRequest.builder()
                .clientOrderCode(order.getOrderCode())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .address(shipmentAddress)
                .note(order.getOrderNote())
                .codAmount(codAmount)
                .insuranceValue(orderTotal)
                .feeCalculationRequest(feeRequest)
                .build());

        order.setShippingCode(shippingOrder.getShippingCode());
        order.setShippingProvider("GHN");
        order.setShippingExpectedDeliveryAt(shippingOrder.getExpectedDeliveryTime());
        order.getHistories().add(OrderHistory.builder()
            .order(order)
            .status(order.getStatus())
            .note("Tạo vận đơn GHN thành công: " + safe(shippingOrder.getShippingCode()))
            .build());
        }

        private SystemInventoryRequest buildSystemInventoryRequest(Order order, String note) {
        return SystemInventoryRequest.builder()
            .referenceCode(order.getOrderCode())
            .note(note)
            .items(order.getItems().stream()
                .map(item -> InventoryLineRequest.builder()
                    .variantId(item.getVariant().getId())
                    .quantity(item.getQuantity())
                    .build())
                .toList())
            .build();
        }

    private void cancelOrderInternal(Order order, String reason, String cancelledBy) {
        Set<OrderStatus> cancellable = Set.of(OrderStatus.PENDING, OrderStatus.AWAITING_PAYMENT, OrderStatus.CONFIRMED);
        if (!cancellable.contains(order.getStatus())) {
            throw new IllegalArgumentException("Đơn hàng không thể hủy ở trạng thái hiện tại: " + order.getStatus());
        }

        PaymentStatus oldPaymentStatus = order.getPaymentStatus();
        order.setStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.FAILED);
        }

        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .status(OrderStatus.CANCELLED)
                .note("Đơn hàng đã hủy. Lý do: " + reason)
                .build());

        if (oldPaymentStatus == PaymentStatus.COMPLETED) {
            for (OrderItem item : order.getItems()) {
                inventoryService.stockIn(cancelledBy, StockInRequest.builder()
                        .variantId(item.getVariant().getId())
                        .quantity(item.getQuantity())
                        .note("Restock from cancelled paid order " + order.getOrderCode())
                        .build());
            }
        } else {
            publishOrderCancelledEvent(order, reason, cancelledBy);
        }

        if (order.getPaymentMethod() == PaymentMethod.VNPAY && oldPaymentStatus == PaymentStatus.COMPLETED) {
            publishRefundRequired(order, reason);
        }

        try {
            membershipService.rollbackPointsFromOrder(order.getUser().getId(), order.getId(), "cancel");
        } catch (RuntimeException ignored) {
            // Cancellation flow should not fail due to loyalty rollback issue.
        }

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EMAIL_EXCHANGE,
                    RabbitMQConfig.ORDER_CANCELLATION_EMAIL_ROUTING_KEY,
                    OrderCancellationEmailMessage.builder()
                            .email(order.getUser().getEmail())
                            .orderCode(order.getOrderCode())
                            .reason(reason)
                            .build()
            );
        } catch (RuntimeException ignored) {
            // Cancellation should not fail because of a notification issue.
        }
    }

    private void publishOrderCancelledEvent(Order order, String reason, String cancelledBy) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderCode(order.getOrderCode())
                .reason(reason)
                .cancelledBy(cancelledBy)
                .items(order.getItems().stream()
                        .map(item -> OrderCancelledEvent.CancelledItem.builder()
                                .variantId(item.getVariant().getId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY,
                event
        );
    }

    private void publishRefundRequired(Order order, String reason) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.REFUND_REQUIRED_ROUTING_KEY,
                RefundRequiredMessage.builder()
                        .orderCode(order.getOrderCode())
                        .reason(reason)
                        .paymentMethod(order.getPaymentMethod().name())
                        .customerEmail(order.getUser().getEmail())
                        .build()
        );
    }

    private OrderReturnRequest getReturnRequestOrThrow(Long returnRequestId) {
        return orderReturnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu trả hàng với id: " + returnRequestId));
    }

    private ReturnRequestResponse toReturnRequestResponse(OrderReturnRequest request) {
        return ReturnRequestResponse.builder()
                .id(request.getId())
                .orderCode(request.getOrder().getOrderCode())
                .status(request.getStatus())
                .reason(request.getReason())
                .refundMethod(request.getRefundMethod())
                .bankAccountName(request.getBankAccountName())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankName(request.getBankName())
                .evidenceUrls(splitEvidence(request.getEvidenceUrls()))
                .adminNote(request.getAdminNote())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .items(request.getItems().stream()
                        .map(item -> ReturnRequestResponse.ReturnItemResponse.builder()
                                .orderItemId(item.getOrderItem().getId())
                                .variantId(item.getOrderItem().getVariant().getId())
                                .productName(item.getOrderItem().getVariant().getProduct().getName())
                                .sku(item.getOrderItem().getVariant().getSku())
                                .requestedQuantity(item.getRequestedQuantity())
                                .receivedQuantity(item.getReceivedQuantity())
                                .action(item.getAction())
                                .build())
                        .toList())
                .build();
    }

    private String joinEvidence(List<String> evidenceUrls) {
        if (evidenceUrls == null || evidenceUrls.isEmpty()) {
            return null;
        }
        return evidenceUrls.stream().filter(this::hasText).collect(Collectors.joining("\n"));
    }

    private List<String> splitEvidence(String evidenceUrls) {
        if (!hasText(evidenceUrls)) {
            return List.of();
        }
        return List.of(evidenceUrls.split("\\n")).stream().filter(this::hasText).toList();
    }

    private record PricingResult(
            List<OrderResponse.OrderLineResponse> items,
            com.badminton.shop.modules.promotion.entity.Promotion promotion,
            String voucherCode,
            BigDecimal itemsAmount,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            BigDecimal totalAmount
    ) {
    }

        private record ShippingSnapshot(
            String receiverName,
            String receiverPhone,
            String shippingAddress,
            UserAddress address
        ) {
        }

            private record VnpayProcessResult(
                Order order,
                String responseCode,
                String message
            ) {
            }
}
