package com.badminton.shop.service.impl;

import com.badminton.shop.dto.request.CartItemRequest;
import com.badminton.shop.dto.response.CartItemResponse;
import com.badminton.shop.dto.response.CartResponse;
import com.badminton.shop.entity.Cart;
import com.badminton.shop.entity.CartItem;
import com.badminton.shop.entity.Product;
import com.badminton.shop.entity.User;
import com.badminton.shop.exception.BadRequestException;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.repository.CartItemRepository;
import com.badminton.shop.repository.CartRepository;
import com.badminton.shop.repository.ProductRepository;
import com.badminton.shop.repository.UserRepository;
import com.badminton.shop.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId).orElse(null);
        if (cart == null) {
            return CartResponse.builder()
                    .userId(userId)
                    .items(List.of())
                    .totalItems(0)
                    .totalAmount(BigDecimal.ZERO)
                    .build();
        }
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(Long userId, CartItemRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Sản phẩm '" + product.getName() + "' không còn bán");
        }

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Sản phẩm '" + product.getName() + "' chỉ còn " +
                    product.getStockQuantity() + " sản phẩm");
        }

        Cart cart = getOrCreateCart(userId);

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            if (newQuantity > product.getStockQuantity()) {
                throw new BadRequestException("Tổng số lượng vượt quá tồn kho của sản phẩm");
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        log.debug("Thêm sản phẩm ID: {} vào giỏ hàng user ID: {}", product.getId(), userId);
        return toResponse(cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    @Override
    @Transactional
    public CartResponse updateCartItemQuantity(Long userId, Long productId, Integer quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng", "userId", userId));

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm trong giỏ hàng", "productId", productId));

        Product product = cartItem.getProduct();
        if (quantity > product.getStockQuantity()) {
            throw new BadRequestException("Số lượng vượt quá tồn kho của sản phẩm: " + product.getStockQuantity());
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        return toResponse(cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItemFromCart(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng", "userId", userId));

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm trong giỏ hàng", "productId", productId));

        cartItemRepository.delete(cartItem);
        log.debug("Xóa sản phẩm ID: {} khỏi giỏ hàng user ID: {}", productId, userId);
        return toResponse(cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartItemRepository.deleteByCartId(cart.getId());
            log.debug("Xóa toàn bộ giỏ hàng của user ID: {}", userId);
        });
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", userId));
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getCartItems().stream()
                .map(item -> {
                    BigDecimal unitPrice = item.getProduct().getEffectivePrice();
                    return CartItemResponse.builder()
                            .id(item.getId())
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .productImageUrl(item.getProduct().getImageUrl())
                            .unitPrice(unitPrice)
                            .quantity(item.getQuantity())
                            .totalPrice(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                            .build();
                })
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(items)
                .totalItems(items.size())
                .totalAmount(totalAmount)
                .build();
    }
}
