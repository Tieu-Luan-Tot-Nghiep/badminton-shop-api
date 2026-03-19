package com.badminton.shop.modules.order.service.impl;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.messaging.dto.CartSyncMessage;
import com.badminton.shop.modules.order.dto.request.AddCartItemRequest;
import com.badminton.shop.modules.order.dto.request.UpdateCartItemRequest;
import com.badminton.shop.modules.order.dto.response.CartItemResponse;
import com.badminton.shop.modules.order.dto.response.CartResponse;
import com.badminton.shop.modules.order.entity.Cart;
import com.badminton.shop.modules.order.entity.CartItem;
import com.badminton.shop.modules.order.repository.CartRepository;
import com.badminton.shop.modules.order.service.CartService;
import com.badminton.shop.modules.product.entity.Product;
import com.badminton.shop.modules.product.entity.ProductVariant;
import com.badminton.shop.modules.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private static final String CART_KEY_PREFIX = "cart:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;

    @Value("${app.cart.redis.ttl-days:7}")
    private long cartTtlDays = 7L;

    @Override
    public CartResponse getMyCart(String principalName) {
        User user = getUserByPrincipal(principalName);
        return loadCartByUserId(user.getId());
    }

    @Override
    public CartResponse addItem(String principalName, AddCartItemRequest request) {
        User user = getUserByPrincipal(principalName);
        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy biến thể sản phẩm với id: " + request.getVariantId()));

        CartResponse cart = loadCartByUserId(user.getId());
        Optional<CartItemResponse> existingItem = cart.getItems().stream()
                .filter(item -> Objects.equals(item.getVariantId(), request.getVariantId()))
                .findFirst();

        int desiredQuantity = request.getQuantity();
        if (existingItem.isPresent()) {
            desiredQuantity += existingItem.get().getQuantity();
        }

        validateStock(variant, desiredQuantity);

        if (existingItem.isPresent()) {
            CartItemResponse item = existingItem.get();
            item.setQuantity(desiredQuantity);
            item.setUnitPrice(safePrice(variant));
            item.setLineTotal(safePrice(variant).multiply(BigDecimal.valueOf(desiredQuantity)));
            item.setProductName(resolveProductName(variant.getProduct()));
            item.setProductId(resolveProductId(variant.getProduct()));
            item.setSku(variant.getSku());
            item.setSize(variant.getSize());
            item.setColor(variant.getColor());
        } else {
            cart.getItems().add(toCartItemResponse(variant, request.getQuantity()));
        }

        finalizeAndSync(user.getId(), cart);
        return cart;
    }

    @Override
    public CartResponse updateItemQuantity(String principalName, Long variantId, UpdateCartItemRequest request) {
        User user = getUserByPrincipal(principalName);

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy biến thể sản phẩm với id: " + variantId));

        CartResponse cart = loadCartByUserId(user.getId());
        CartItemResponse item = cart.getItems().stream()
                .filter(it -> Objects.equals(it.getVariantId(), variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sản phẩm này không tồn tại trong giỏ hàng"));

        validateStock(variant, request.getQuantity());

        item.setQuantity(request.getQuantity());
        item.setUnitPrice(safePrice(variant));
    item.setLineTotal(safePrice(variant).multiply(BigDecimal.valueOf(request.getQuantity())));
        item.setProductName(resolveProductName(variant.getProduct()));
        item.setProductId(resolveProductId(variant.getProduct()));
        item.setSku(variant.getSku());
        item.setSize(variant.getSize());
        item.setColor(variant.getColor());

        finalizeAndSync(user.getId(), cart);
        return cart;
    }

    @Override
    public CartResponse removeItem(String principalName, Long variantId) {
        User user = getUserByPrincipal(principalName);
        CartResponse cart = loadCartByUserId(user.getId());

        boolean removed = cart.getItems().removeIf(item -> Objects.equals(item.getVariantId(), variantId));
        if (!removed) {
            throw new ResourceNotFoundException("Sản phẩm này không tồn tại trong giỏ hàng");
        }

        finalizeAndSync(user.getId(), cart);
        return cart;
    }

    @Override
    public CartResponse clearCart(String principalName) {
        User user = getUserByPrincipal(principalName);
        CartResponse cart = loadCartByUserId(user.getId());
        cart.setItems(new ArrayList<>());

        finalizeAndSync(user.getId(), cart);
        return cart;
    }

    private User getUserByPrincipal(String principalName) {
        return userRepository.findByEmail(principalName)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với email: " + principalName));
    }

    private CartResponse loadCartByUserId(Long userId) {
        String redisKey = buildCartKey(userId);
        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached instanceof CartResponse cartResponse) {
                return cartResponse;
            }
        } catch (RuntimeException ex) {
            // Tương thích ngược khi schema cache thay đổi (vd: Double -> BigDecimal).
            log.warn("[cart-cache] Không thể deserialize cart cache cho key {}. Evict key và fallback Postgres.",
                    redisKey, ex);
            redisTemplate.delete(redisKey);
        }

        CartResponse recovered = recoverCartFromPostgres(userId);
        cacheCart(redisKey, recovered);
        return recovered;
    }

    private CartResponse recoverCartFromPostgres(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .map(this::toCartResponse)
                .orElseGet(() -> emptyCart(userId));
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toCartItemResponse)
                .toList();

        CartResponse response = CartResponse.builder()
                .userId(cart.getUser().getId())
                .items(new ArrayList<>(items))
                .updatedAt(cart.getUpdatedAt() != null ? cart.getUpdatedAt() : LocalDateTime.now())
                .build();
        recalculateTotals(response);
        return response;
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        ProductVariant variant = item.getVariant();
        Product product = variant != null ? variant.getProduct() : null;
        BigDecimal unitPrice = variant != null ? safePrice(variant) : BigDecimal.ZERO;

        return CartItemResponse.builder()
                .variantId(variant != null ? variant.getId() : null)
                .productId(resolveProductId(product))
                .productName(resolveProductName(product))
                .sku(variant != null ? variant.getSku() : null)
                .size(variant != null ? variant.getSize() : null)
                .color(variant != null ? variant.getColor() : null)
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .lineTotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }

    private CartItemResponse toCartItemResponse(ProductVariant variant, int quantity) {
        Product product = variant.getProduct();
            BigDecimal unitPrice = safePrice(variant);

        return CartItemResponse.builder()
                .variantId(variant.getId())
                .productId(resolveProductId(product))
                .productName(resolveProductName(product))
                .sku(variant.getSku())
                .size(variant.getSize())
                .color(variant.getColor())
                .unitPrice(unitPrice)
                .quantity(quantity)
                .lineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                .build();
    }

    private void finalizeAndSync(Long userId, CartResponse cart) {
        recalculateTotals(cart);
        cart.setUpdatedAt(LocalDateTime.now());

        String redisKey = buildCartKey(userId);
        cacheCart(redisKey, cart);
        touchTtl(redisKey);
        publishCartSync(userId, cart);
    }

    private void cacheCart(String redisKey, CartResponse cart) {
        redisTemplate.opsForValue().set(redisKey, cart, cartTtlDays, TimeUnit.DAYS);
    }

    private void touchTtl(String redisKey) {
        redisTemplate.expire(redisKey, cartTtlDays, TimeUnit.DAYS);
    }

    private void publishCartSync(Long userId, CartResponse cart) {
        List<CartSyncMessage.CartSyncItem> syncItems = cart.getItems().stream()
                .map(item -> CartSyncMessage.CartSyncItem.builder()
                        .variantId(item.getVariantId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        CartSyncMessage message = CartSyncMessage.builder()
                .userId(userId)
                .items(syncItems)
                .updatedAt(cart.getUpdatedAt())
                .build();

        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CART_EXCHANGE,
                RabbitMQConfig.CART_SYNC_ROUTING_KEY,
                message
            );
        } catch (RuntimeException ex) {
            // Redis vẫn là nguồn phục vụ chính; persistence async sẽ retry qua thao tác tiếp theo.
            log.error("[cart-sync] Không thể publish snapshot cart cho user {}", userId, ex);
        }
    }

    private void recalculateTotals(CartResponse cart) {
        int totalItems = cart.getItems().stream()
                .map(CartItemResponse::getQuantity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        BigDecimal totalAmount = cart.getItems().stream()
                .map(CartItemResponse::getLineTotal)
                .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalItems(totalItems);
        cart.setTotalAmount(totalAmount);
    }

    private void validateStock(ProductVariant variant, int quantity) {
        Integer stock = variant.getStock();
        if (stock != null && quantity > stock) {
            throw new IllegalArgumentException("Số lượng yêu cầu vượt quá tồn kho hiện tại");
        }
    }

    private CartResponse emptyCart(Long userId) {
        return CartResponse.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .totalItems(0)
                .totalAmount(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String buildCartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private BigDecimal safePrice(ProductVariant variant) {
        return variant.getPrice() != null ? BigDecimal.valueOf(variant.getPrice()) : BigDecimal.ZERO;
    }

    private Long resolveProductId(Product product) {
        return product != null ? product.getId() : null;
    }

    private String resolveProductName(Product product) {
        return product != null ? product.getName() : null;
    }
}
