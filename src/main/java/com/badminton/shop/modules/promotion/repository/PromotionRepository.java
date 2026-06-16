package com.badminton.shop.modules.promotion.repository;

import com.badminton.shop.modules.promotion.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    Page<Promotion> findAllByIsActiveTrue(Pageable pageable);

    long countByIsActiveTrue();

    /**
     * Tìm promotion đang active và sắp hết hạn trong khoảng [now, deadline].
     * Dùng cho scheduler cảnh báo voucher sắp hết hạn.
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND p.expiryDate >= :now AND p.expiryDate <= :deadline")
    List<Promotion> findActiveExpiringSoon(@Param("now") LocalDateTime now,
                                           @Param("deadline") LocalDateTime deadline);

    /**
     * Tìm promotion sắp bắt đầu trong khoảng [from, to].
     * Dùng cho scheduler thông báo flash sale sắp mở.
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND p.startDate >= :from AND p.startDate <= :to")
    List<Promotion> findStartingSoon(@Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);
}
