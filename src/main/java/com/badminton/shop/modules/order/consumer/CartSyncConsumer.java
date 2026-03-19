package com.badminton.shop.modules.order.consumer;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.messaging.dto.CartSyncMessage;
import com.badminton.shop.modules.order.entity.Cart;
import com.badminton.shop.modules.order.entity.CartItem;
import com.badminton.shop.modules.order.repository.CartRepository;
import com.badminton.shop.modules.product.entity.ProductVariant;
import com.badminton.shop.modules.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartSyncConsumer {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;

    @RabbitListener(queues = RabbitMQConfig.CART_SYNC_QUEUE)
    @Transactional
    public void consumeCartSync(CartSyncMessage message) {
        if (message == null || message.getUserId() == null) {
            log.warn("[cart-sync] Bỏ qua message không hợp lệ: {}", message);
            return;
        }

        User user = userRepository.findById(message.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với id: " + message.getUserId()));

        Cart cart = cartRepository.findByUserIdWithItems(message.getUserId())
                .orElseGet(() -> Cart.builder().user(user).build());

        cart.getItems().clear();

        List<CartItem> syncedItems = new ArrayList<>();
        List<CartSyncMessage.CartSyncItem> incomingItems =
                message.getItems() != null ? message.getItems() : List.of();

        for (CartSyncMessage.CartSyncItem syncItem : incomingItems) {
            ProductVariant variant = productVariantRepository.findById(syncItem.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy biến thể sản phẩm với id: " + syncItem.getVariantId()));

            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(syncItem.getQuantity())
                    .build();
            syncedItems.add(cartItem);
        }

        cart.getItems().addAll(syncedItems);
        cart.setUpdatedAt(message.getUpdatedAt() != null ? message.getUpdatedAt() : LocalDateTime.now());
        cartRepository.save(cart);

        log.info("[cart-sync] Đồng bộ giỏ hàng user {} xuống Postgres thành công ({} items)",
                message.getUserId(), syncedItems.size());
    }
}
