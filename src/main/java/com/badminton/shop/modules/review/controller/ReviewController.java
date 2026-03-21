package com.badminton.shop.modules.review.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.review.dto.request.CreateReviewRequest;
import com.badminton.shop.modules.review.dto.request.UpdateReviewRequest;
import com.badminton.shop.modules.review.dto.response.ReviewResponse;
import com.badminton.shop.modules.review.dto.response.ReviewSummaryResponse;
import com.badminton.shop.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            Principal principal,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        ReviewResponse response = reviewService.createReview(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Review created successfully.", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        ReviewResponse response = reviewService.updateReview(principal.getName(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully.", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteReview(Principal principal, @PathVariable Long id) {
        reviewService.deleteReview(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully.", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewById(@PathVariable Long id) {
        ReviewResponse response = reviewService.getReviewById(id);
        return ResponseEntity.ok(ApiResponse.success("Review fetched successfully.", response));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviewsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ReviewResponse> response = reviewService.getReviewsByProduct(productId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Product reviews fetched successfully.", response));
    }

    @GetMapping("/products/{productId}/summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getReviewSummaryByProduct(@PathVariable Long productId) {
        ReviewSummaryResponse response = reviewService.getReviewSummaryByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Review summary fetched successfully.", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getMyReviews(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ReviewResponse> response = reviewService.getMyReviews(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success("My reviews fetched successfully.", response));
    }
}
