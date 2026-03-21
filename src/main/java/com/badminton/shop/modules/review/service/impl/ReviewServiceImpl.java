package com.badminton.shop.modules.review.service.impl;

import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.order.entity.OrderItem;
import com.badminton.shop.modules.order.entity.OrderStatus;
import com.badminton.shop.modules.order.repository.OrderItemRepository;
import com.badminton.shop.modules.product.entity.Product;
import com.badminton.shop.modules.product.repository.ProductRepository;
import com.badminton.shop.modules.review.dto.request.CreateReviewRequest;
import com.badminton.shop.modules.review.dto.request.UpdateReviewRequest;
import com.badminton.shop.modules.review.dto.response.ReviewResponse;
import com.badminton.shop.modules.review.dto.response.ReviewSummaryResponse;
import com.badminton.shop.modules.review.entity.Review;
import com.badminton.shop.modules.review.repository.ReviewRepository;
import com.badminton.shop.modules.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public ReviewResponse createReview(String principalName, CreateReviewRequest request) {
        User user = findUserByPrincipal(principalName);

        OrderItem orderItem = orderItemRepository.findWithDetailsById(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + request.getOrderItemId()));

        if (!orderItem.getOrder().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to review this order item.");
        }

        if (orderItem.getOrder().getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Only delivered orders can be reviewed.");
        }

        if (reviewRepository.findByOrderItemId(orderItem.getId()).isPresent()) {
            throw new IllegalArgumentException("This order item has already been reviewed.");
        }

        Product product = orderItem.getVariant().getProduct();
        Review review = Review.builder()
                .rating(request.getRating())
                .comment(normalizeComment(request.getComment()))
                .user(user)
                .product(product)
                .orderItem(orderItem)
                .build();

        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    @Override
    public ReviewResponse updateReview(String principalName, Long reviewId, UpdateReviewRequest request) {
        User user = findUserByPrincipal(principalName);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to update this review.");
        }

        review.setRating(request.getRating());
        review.setComment(normalizeComment(request.getComment()));
        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    @Override
    public void deleteReview(String principalName, Long reviewId) {
        User user = findUserByPrincipal(principalName);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to delete this review.");
        }

        OrderItem orderItem = review.getOrderItem();
        if (orderItem != null) {
            orderItem.setReview(null);
            review.setOrderItem(null);
        }

        reviewRepository.delete(review);
        reviewRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        return toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByProduct(Long productId, int page, int size) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return reviewRepository.findAllByProductId(product.getId(), pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(String principalName, int page, int size) {
        User user = findUserByPrincipal(principalName);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return reviewRepository.findAllByUserId(user.getId(), pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummaryByProduct(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        long totalReviews = reviewRepository.countByProductId(productId);
        Double avgRating = reviewRepository.averageRatingByProductId(productId);

        return ReviewSummaryResponse.builder()
                .productId(productId)
                .averageRating(avgRating == null ? 0.0d : Math.round(avgRating * 100.0d) / 100.0d)
                .totalReviews(totalReviews)
                .build();
    }

    private User findUserByPrincipal(String principalName) {
        return userRepository.findByEmail(principalName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + principalName));
    }

    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String normalized = comment.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private ReviewResponse toResponse(Review review) {
        Product product = review.getProduct();
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .userId(review.getUser().getId())
                .username(review.getUser().getUsername())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .orderItemId(review.getOrderItem() != null ? review.getOrderItem().getId() : null)
                .build();
    }
}
