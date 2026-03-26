package com.badminton.shop.modules.product.repository;

import com.badminton.shop.modules.product.entity.ProductWishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductWishlistRepository extends JpaRepository<ProductWishlist, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<ProductWishlist> findByUserIdAndProductId(Long userId, Long productId);

    @Query("""
            SELECT w FROM ProductWishlist w
            JOIN FETCH w.product p
            LEFT JOIN FETCH p.brand
            LEFT JOIN FETCH p.category
            WHERE w.user.id = :userId
            ORDER BY w.createdAt DESC
            """)
    List<ProductWishlist> findAllByUserIdWithProduct(@Param("userId") Long userId);
}
