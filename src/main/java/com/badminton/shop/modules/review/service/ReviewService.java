package com.badminton.shop.modules.review.service;

import com.badminton.shop.modules.review.dto.request.CreateReviewRequest;
import com.badminton.shop.modules.review.dto.request.UpdateReviewRequest;
import com.badminton.shop.modules.review.dto.response.ReviewResponse;
import com.badminton.shop.modules.review.dto.response.ReviewSummaryResponse;
import org.springframework.data.domain.Page;

public interface ReviewService {

    ReviewResponse createReview(String principalName, CreateReviewRequest request);

    ReviewResponse updateReview(String principalName, Long reviewId, UpdateReviewRequest request);

    void deleteReview(String principalName, Long reviewId);

    ReviewResponse getReviewById(Long reviewId);

    Page<ReviewResponse> getReviewsByProduct(Long productId, int page, int size);

    Page<ReviewResponse> getMyReviews(String principalName, int page, int size);

    ReviewSummaryResponse getReviewSummaryByProduct(Long productId);
}
