package com.badminton.shop.modules.order.consumer;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.messaging.dto.FcmNotificationMessage;
import com.badminton.shop.modules.messaging.dto.ShippingStatusChangedMessage;
import com.badminton.shop.modules.messaging.service.FcmService;
import com.badminton.shop.modules.order.entity.Order;
import com.badminton.shop.modules.order.entity.OrderHistory;
import com.badminton.shop.modules.order.entity.PaymentMethod;
import com.badminton.shop.modules.order.entity.PaymentStatus;
import com.badminton.shop.modules.order.entity.OrderStatus;
import com.badminton.shop.modules.membership.service.MembershipService;
import com.badminton.shop.modules.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingStatusConsumer {

    private final OrderRepository orderRepository;
    private final MembershipService membershipService;
    private final FcmService fcmService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_SHIPPING_STATUS_QUEUE)
    @Transactional
    public void consumeShippingStatus(ShippingStatusChangedMessage message) {
        if (message == null || !hasText(message.getStatus())) {
            return;
        }

        Optional<Order> orderOptional = findOrder(message);
        if (orderOptional.isEmpty()) {
            log.warn("[shipping-status] Không tìm thấy order cho message: {}", message);
            return;
        }

        Order order = orderOptional.get();
        OrderStatus mappedStatus = mapGhnStatus(message.getStatus());
        if (mappedStatus == null || order.getStatus() == mappedStatus) {
            return;
        }

        order.setStatus(mappedStatus);

        if (mappedStatus == OrderStatus.DELIVERED) {
            if (order.getPaymentMethod() == PaymentMethod.COD
                    && order.getPaymentStatus() != PaymentStatus.COMPLETED) {
                order.setPaymentStatus(PaymentStatus.COMPLETED);
            }

            if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
                try {
                    membershipService.addPointsFromOrder(
                            order.getUser().getId(),
                            java.math.BigDecimal.valueOf(order.getTotalAmount()),
                            order.getId()
                    );
                } catch (RuntimeException ignored) {
                    // Shipping status processing should remain resilient.
                }
            }

            // Push notification: Giao hàng thành công
            sendShippingNotification(order,
                    "📦 Giao hàng thành công",
                    "Đơn hàng #" + order.getOrderCode() + " đã được giao thành công. Cảm ơn bạn đã mua sắm!",
                    "ORDER_DELIVERED");

        } else if (mappedStatus == OrderStatus.SHIPPING) {
            // Push notification: Đang giao hàng
            sendShippingNotification(order,
                    "🚚 Đơn hàng đang được giao",
                    "Đơn hàng #" + order.getOrderCode() + " đang trên đường giao đến bạn.",
                    "ORDER_SHIPPING");

        } else if (mappedStatus == OrderStatus.CANCELLED || mappedStatus == OrderStatus.RETURNED) {
            // Push notification: GHN báo hủy/hoàn hàng
            sendShippingNotification(order,
                    "⚠️ Giao hàng không thành công",
                    "Đơn hàng #" + order.getOrderCode() + " không thể giao. Vui lòng liên hệ hỗ trợ.",
                    "ORDER_SHIPPING_FAILED");
        }

        order.getHistories().add(OrderHistory.builder()
                .order(order)
                .status(mappedStatus)
                .note("GHN cập nhật trạng thái: " + message.getStatus())
                .build());

        orderRepository.save(order);
    }

    private Optional<Order> findOrder(ShippingStatusChangedMessage message) {
        if (hasText(message.getClientOrderCode())) {
            Optional<Order> order = orderRepository.findByOrderCode(message.getClientOrderCode());
            if (order.isPresent()) {
                return order;
            }
        }

        if (hasText(message.getShippingCode())) {
            return orderRepository.findByShippingCode(message.getShippingCode());
        }

        return Optional.empty();
    }

    private OrderStatus mapGhnStatus(String ghnStatus) {
        String normalized = ghnStatus.toLowerCase(Locale.ROOT);
        if (normalized.contains("delivered")) {
            return OrderStatus.DELIVERED;
        }
        if (normalized.contains("return") || normalized.contains("damage") || normalized.contains("lost")) {
            return OrderStatus.RETURNED;
        }
        if (normalized.contains("cancel") || normalized.contains("fail")) {
            return OrderStatus.CANCELLED;
        }
        if (normalized.contains("pick") || normalized.contains("transport") || normalized.contains("deliver")) {
            return OrderStatus.SHIPPING;
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void sendShippingNotification(Order order, String title, String body, String screen) {
        try {
            var user = order.getUser();
            if (user == null || user.getFcmToken() == null || user.getFcmToken().isBlank()) {
                return;
            }
            fcmService.sendAsync(FcmNotificationMessage.builder()
                    .fcmToken(user.getFcmToken())
                    .title(title)
                    .body(body)
                    .data(Map.of(
                            "screen", screen,
                            "orderCode", order.getOrderCode() != null ? order.getOrderCode() : ""
                    ))
                    .build());
        } catch (Exception e) {
            log.warn("[FCM] Không thể gửi shipping notification. orderCode={}: {}",
                    order.getOrderCode(), e.getMessage());
        }
    }
}
