package com.badminton.shop.service.impl;

import com.badminton.shop.dto.request.OrderItemRequest;
import com.badminton.shop.dto.request.OrderRequest;
import com.badminton.shop.dto.response.OrderItemResponse;
import com.badminton.shop.dto.response.OrderResponse;
import com.badminton.shop.dto.response.PageResponse;
import com.badminton.shop.dto.response.PaymentResponse;
import com.badminton.shop.entity.*;
import com.badminton.shop.enums.OrderStatus;
import com.badminton.shop.enums.PaymentStatus;
import com.badminton.shop.exception.BadRequestException;
import com.badminton.shop.exception.BusinessException;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.repository.OrderRepository;
import com.badminton.shop.repository.PaymentRepository;
import com.badminton.shop.repository.ProductRepository;
import com.badminton.shop.repository.UserRepository;
import com.badminton.shop.service.OrderService;
import com.badminton.shop.util.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        log.info("Bắt đầu tạo đơn hàng cho user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", userId));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", itemRequest.getProductId()));

            if (!product.isActive()) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' không còn bán");
            }

            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new BusinessException("INSUFFICIENT_STOCK",
                        "Sản phẩm '" + product.getName() + "' chỉ còn " + product.getStockQuantity() + " sản phẩm");
            }

            BigDecimal unitPrice = product.getEffectivePrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            subtotal = subtotal.add(totalPrice);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .productImageUrl(product.getImageUrl())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .build();
            orderItems.add(orderItem);

            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            productRepository.save(product);
        }

        BigDecimal shippingFee = calculateShippingFee(subtotal);
        BigDecimal totalAmount = subtotal.add(shippingFee);
        String orderCode = AppUtils.generateOrderCode();

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .shippingAddress(request.getShippingAddress())
                .receiverPhone(request.getReceiverPhone())
                .receiverName(request.getReceiverName())
                .note(request.getNote())
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(savedOrder)
                .method(request.getPaymentMethod())
                .amount(totalAmount)
                .build();
        paymentRepository.save(payment);

        log.info("Tạo đơn hàng thành công: orderCode={}, totalAmount={}", orderCode, totalAmount);
        return toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "orderCode", orderCode));
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findAllByUserId(userId, pageable);
        return AppUtils.toPageResponse(orderPage, orderPage.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(int page, int size, OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = (status != null)
                ? orderRepository.findAllByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        return AppUtils.toPageResponse(orderPage, orderPage.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = findById(id);
        OrderStatus currentStatus = order.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        log.info("Cập nhật trạng thái đơn hàng ID: {} từ {} sang {}", id, currentStatus, newStatus);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id, Long userId) {
        Order order = findById(id);

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền hủy đơn hàng này");
        }

        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            throw new BusinessException("CANNOT_CANCEL",
                    "Chỉ có thể hủy đơn hàng ở trạng thái PENDING. Trạng thái hiện tại: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        restoreStock(order.getOrderItems());
        orderRepository.save(order);
        log.info("Hủy đơn hàng thành công ID: {} bởi user ID: {}", id, userId);
    }

    private void restoreStock(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }
    }

    private BigDecimal calculateShippingFee(BigDecimal subtotal) {
        BigDecimal freeShippingThreshold = BigDecimal.valueOf(500000);
        return subtotal.compareTo(freeShippingThreshold) >= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(30000);
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        boolean valid = switch (from) {
            case PENDING -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED;
            case CONFIRMED -> to == OrderStatus.PROCESSING || to == OrderStatus.CANCELLED;
            case PROCESSING -> to == OrderStatus.SHIPPED;
            case SHIPPED -> to == OrderStatus.DELIVERED;
            case DELIVERED -> to == OrderStatus.REFUNDED;
            default -> false;
        };

        if (!valid) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                    "Không thể chuyển trạng thái từ " + from + " sang " + to);
        }
    }

    private Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", id));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProductName())
                        .productImageUrl(item.getProductImageUrl())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .toList();

        PaymentResponse paymentResponse = null;
        if (order.getPayment() != null) {
            Payment payment = order.getPayment();
            paymentResponse = PaymentResponse.builder()
                    .id(payment.getId())
                    .orderId(order.getId())
                    .method(payment.getMethod())
                    .status(payment.getStatus())
                    .amount(payment.getAmount())
                    .transactionCode(payment.getTransactionCode())
                    .paidAt(payment.getPaidAt())
                    .createdAt(payment.getCreatedAt())
                    .build();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discount(order.getDiscount())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .receiverPhone(order.getReceiverPhone())
                .receiverName(order.getReceiverName())
                .note(order.getNote())
                .orderItems(itemResponses)
                .payment(paymentResponse)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
