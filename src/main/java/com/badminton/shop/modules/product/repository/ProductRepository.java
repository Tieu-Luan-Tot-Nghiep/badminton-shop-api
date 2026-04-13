package com.badminton.shop.modules.product.repository;

import com.badminton.shop.modules.product.entity.Product;
import com.badminton.shop.modules.product.repository.projection.AdminProductListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category c
            LEFT JOIN FETCH p.brand b
            WHERE p.id = :id
            """)
    Optional<Product> findByIdForSearchSync(@Param("id") Long id);

    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category c
            LEFT JOIN FETCH p.brand b
            """)
    java.util.List<Product> findAllForSearchSync();

    /**
     * Lấy danh sách sản phẩm public:
     * - is_deleted = false (đã filter bởi @SQLRestriction)
     * - is_active = true
     * - Category & Brand chưa bị xóa mềm
     * - Hỗ trợ lọc theo category slug, brand slug, khoảng giá
     */
    @Query("""
            SELECT p FROM Product p
            JOIN p.category c
            JOIN p.brand b
            WHERE p.isActive = true
              AND c.isDeleted = false
              AND b.isDeleted = false
              AND (:categorySlug IS NULL OR c.slug = :categorySlug)
              AND (:brandSlug IS NULL OR b.slug = :brandSlug)
              AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
              AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
                                                        AND (:keyword IS NULL OR LOWER(CAST(p.name AS text)) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')))
            """)
    Page<Product> findAllPublicProducts(
            @Param("categorySlug") String categorySlug,
            @Param("brandSlug") String brandSlug,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * Lấy chi tiết sản phẩm public theo slug (kiểm tra active + brand/category chưa bị xóa)
     */
                @Query("""
                                                SELECT DISTINCT p FROM Product p
                                                JOIN FETCH p.category c
                                                JOIN FETCH p.brand b
                                                LEFT JOIN FETCH p.productImages pi
                                                LEFT JOIN FETCH p.variants v
                                                WHERE p.slug = :slug
                                                        AND p.isActive = true
                                                        AND c.isDeleted = false
                                                        AND b.isDeleted = false
                                                """)
                Optional<Product> findPublicProductBySlug(@Param("slug") String slug);

                @Query("""
                                                SELECT p FROM Product p
                                                JOIN p.category c
                                                JOIN p.brand b
                                                WHERE p.isActive = true
                                                        AND c.isDeleted = false
                                                        AND b.isDeleted = false
                                                ORDER BY p.updatedAt DESC, p.id DESC
                                                """)
                List<Product> findFeaturedProducts(Pageable pageable);

                @Query("""
                                                SELECT p FROM Product p
                                                JOIN p.category c
                                                JOIN p.brand b
                                                WHERE p.isActive = true
                                                        AND c.isDeleted = false
                                                        AND b.isDeleted = false
                                                ORDER BY p.createdAt DESC, p.id DESC
                                                """)
                List<Product> findNewestProducts(Pageable pageable);

                    @Query(
                            value = """
                                    SELECT
                                        p.id AS id,
                                        p.name AS name,
                                        p.slug AS slug,
                                        p.short_description AS shortDescription,
                                        p.thumbnail_url AS thumbnailUrl,
                                        p.base_price AS basePrice,
                                        b.name AS brandName,
                                        c.name AS categoryName,
                                        p.is_active AS isActive,
                                        p.is_deleted AS isDeleted
                                    FROM products p
                                    LEFT JOIN brands b ON p.brand_id = b.id
                                    LEFT JOIN categories c ON p.category_id = c.id
                                    WHERE (:categorySlug IS NULL OR c.slug = :categorySlug)
                                      AND (:brandSlug IS NULL OR b.slug = :brandSlug)
                                      AND (:minPrice IS NULL OR p.base_price >= :minPrice)
                                      AND (:maxPrice IS NULL OR p.base_price <= :maxPrice)
                                      AND (:keyword IS NULL OR LOWER(CAST(p.name AS text)) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')))
                                      AND (:isActive IS NULL OR p.is_active = :isActive)
                                      AND (:isDeleted IS NULL OR p.is_deleted = :isDeleted)
                                    ORDER BY p.created_at DESC, p.id DESC
                                    """,
                            countQuery = """
                                    SELECT COUNT(1)
                                    FROM products p
                                    LEFT JOIN brands b ON p.brand_id = b.id
                                    LEFT JOIN categories c ON p.category_id = c.id
                                    WHERE (:categorySlug IS NULL OR c.slug = :categorySlug)
                                      AND (:brandSlug IS NULL OR b.slug = :brandSlug)
                                      AND (:minPrice IS NULL OR p.base_price >= :minPrice)
                                      AND (:maxPrice IS NULL OR p.base_price <= :maxPrice)
                                      AND (:keyword IS NULL OR LOWER(CAST(p.name AS text)) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')))
                                      AND (:isActive IS NULL OR p.is_active = :isActive)
                                      AND (:isDeleted IS NULL OR p.is_deleted = :isDeleted)
                                    """,
                            nativeQuery = true
                    )
                    Page<AdminProductListProjection> findAllAdminProducts(
                            @Param("categorySlug") String categorySlug,
                            @Param("brandSlug") String brandSlug,
                            @Param("minPrice") BigDecimal minPrice,
                            @Param("maxPrice") BigDecimal maxPrice,
                            @Param("keyword") String keyword,
                            @Param("isActive") Boolean isActive,
                            @Param("isDeleted") Boolean isDeleted,
                            Pageable pageable
                    );
}
