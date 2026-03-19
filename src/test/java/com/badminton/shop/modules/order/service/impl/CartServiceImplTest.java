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
import com.badminton.shop.modules.product.entity.Product;
import com.badminton.shop.modules.product.entity.ProductVariant;
import com.badminton.shop.modules.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        user = User.builder()
                .id(1L)
                .email("customer@test.com")
                .username("customer")
                .isActive(true)
                .build();

        product = Product.builder()
                .id(100L)
                .name("Yonex Astrox")
                .slug("yonex-astrox")
                .build();

        variant = ProductVariant.builder()
                .id(10L)
                .sku("SKU-10")
                .size("4U")
                .color("Black")
                .price(100.0)
                .stock(10)
                .product(product)
                .build();
    }

    @Test
    void getMyCart_ShouldReturnCachedCart_WhenRedisHit() {
        CartResponse cached = CartResponse.builder()
                .userId(1L)
                .items(new ArrayList<>())
                .totalItems(0)
                .totalAmount(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(user));
        when(valueOperations.get("cart:1")).thenReturn(cached);

        CartResponse result = cartService.getMyCart("customer@test.com");

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        verify(cartRepository, never()).findByUserIdWithItems(any());
    }

    @Test
    void getMyCart_ShouldRecoverFromPostgresAndCache_WhenRedisMiss() {
        CartItem cartItem = CartItem.builder()
                .cart(null)
                .variant(variant)
                .quantity(2)
                .build();

        Cart cart = Cart.builder()
                .id(5L)
                .user(user)
                .updatedAt(LocalDateTime.now())
                .items(new ArrayList<>(List.of(cartItem)))
                .build();

        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(user));
        when(valueOperations.get("cart:1")).thenReturn(null);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));

        CartResponse result = cartService.getMyCart("customer@test.com");

        assertEquals(1, result.getItems().size());
        assertEquals(2, result.getTotalItems());
        assertEquals(0, BigDecimal.valueOf(200.0).compareTo(result.getTotalAmount()));
        verify(valueOperations).set(eq("cart:1"), any(CartResponse.class), eq(7L), eq(TimeUnit.DAYS));
    }

        @Test
        void getMyCart_ShouldEvictAndRecover_WhenRedisDeserializeFails() {
                CartItem cartItem = CartItem.builder()
                                .cart(null)
                                .variant(variant)
                                .quantity(2)
                                .build();

                Cart cart = Cart.builder()
                                .id(6L)
                                .user(user)
                                .updatedAt(LocalDateTime.now())
                                .items(new ArrayList<>(List.of(cartItem)))
                                .build();

                when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(user));
                when(valueOperations.get("cart:1")).thenThrow(new RuntimeException("deserialize error"));
                when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(cart));

                CartResponse result = cartService.getMyCart("customer@test.com");

                assertEquals(1, result.getItems().size());
                assertEquals(2, result.getTotalItems());
                assertEquals(0, BigDecimal.valueOf(200.0).compareTo(result.getTotalAmount()));

                verify(redisTemplate).delete("cart:1");
                verify(valueOperations).set(eq("cart:1"), any(CartResponse.class), eq(7L), eq(TimeUnit.DAYS));
        }

    @Test
    void addItem_ShouldCreateNewItemAndPublishSync_WhenItemNotExists() {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .variantId(10L)
                .quantity(2)
                .build();

        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(user));
        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));
        when(valueOperations.get("cart:1")).thenReturn(null);
        when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.empty());

        CartResponse result = cartService.addItem("customer@test.com", request);

        assertEquals(1, result.getItems().size());
        assertEquals(2, result.getTotalItems());
        assertEquals(0, BigDecimal.valueOf(200.0).compareTo(result.getTotalAmount()));

        verify(valueOperations, times(2)).set(eq("cart:1"), any(CartResponse.class), eq(7L), eq(TimeUnit.DAYS));
        verify(redisTemplate).expire("cart:1", 7L, TimeUnit.DAYS);

        ArgumentCaptor<CartSyncMessage> captor = ArgumentCaptor.forClass(CartSyncMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.CART_EXCHANGE),
                eq(RabbitMQConfig.CART_SYNC_ROUTING_KEY),
                captor.capture()
        );
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals(1, captor.getValue().getItems().size());
        assertEquals(10L, captor.getValue().getItems().get(0).getVariantId());
        assertEquals(2, captor.getValue().getItems().get(0).getQuantity());
    }

    @Test
    void addItem_ShouldMergeQuantity_WhenItemExistsInCart() {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .variantId(10L)
                .quantity(2)
                .build();

        CartItemResponse existingItem = CartItemResponse.builder()
                .variantId(10L)
                .productId(100L)
                .productName("Yonex Astrox")
                .sku("SKU-10")
                .size("4U")
                .color("Black")
                .unitPrice(BigDecimal.valueOf(100.0))
                .quantity(1)
                .lineTotal(BigDecimal.valueOf(100.0))
                .build();

        CartResponse cached = CartResponse.builder()
                .userId(1L)
                .items(new ArrayList<>(List.of(existingItem)))
                .totalItems(1)
                .totalAmount(BigDecimal.valueOf(100.0))
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(user));
        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));
        when(valueOperations.get("cart:1")).thenReturn(cached);

        CartResponse result = cartService.addItem("customer@test.com", request);

        assertEquals(1, result.getItems().size());
        assertEquals(3, result.getItems().get(0).getQuantity());
        assertEquals(3, result.getTotalItems());
        assertEquals(0, BigDecimal.valueOf(300.0).compareTo(result.getTotalAmount()));
    }

    @Test
    void updateItemQuantity_ShouldThrow_WhenExceedStock() {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(11)
                .build();

        CartItemResponse existingItem = CartItemResponse.builder()
                .variantId(10L)
                .quantity(1)
                .lineTotal(BigDecimal.valueOf(100.0))
                .build();

        CartResponse cached = CartResponse.builder()
                .userId(1L)
                .items(new ArrayList<>(List.of(existingItem)))
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(user));
        when(productVariantRepository.findById(10L)).thenReturn(Optional.of(variant));
        when(valueOperations.get("cart:1")).thenReturn(cached);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateItemQuantity("customer@test.com", 10L, request)
        );

        assertEquals("Số lượng yêu cầu vượt quá tồn kho hiện tại", ex.getMessage());
        verify(rabbitTemplate, never()).convertAndSend(
                eq(RabbitMQConfig.CART_EXCHANGE),
                eq(RabbitMQConfig.CART_SYNC_ROUTING_KEY),
                any(CartSyncMessage.class)
        );
    }

    @Test
    void removeItem_ShouldThrow_WhenItemNotInCart() {
        CartResponse cached = CartResponse.builder()
                .userId(1L)
                .items(new ArrayList<>())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(user));
        when(valueOperations.get("cart:1")).thenReturn(cached);

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.removeItem("customer@test.com", 999L));
    }
}
