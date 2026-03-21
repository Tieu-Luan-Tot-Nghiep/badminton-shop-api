package com.badminton.shop.modules.review.repository;

import com.badminton.shop.modules.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"user", "product", "orderItem"})
    Optional<Review> findById(Long id);

    Optional<Review> findByOrderItemId(Long orderItemId);

    @EntityGraph(attributePaths = {"user", "product", "orderItem"})
    Page<Review> findAllByProductId(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "product", "orderItem"})
    Page<Review> findAllByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "product", "orderItem"})
    List<Review> findTop3ByProductIdOrderByCreatedAtDesc(Long productId);

    long countByProductId(Long productId);

    @Query("select avg(r.rating) from Review r where r.product.id = :productId")
    Double averageRatingByProductId(@Param("productId") Long productId);
}
