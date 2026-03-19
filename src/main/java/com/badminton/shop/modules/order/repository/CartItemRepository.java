package com.badminton.shop.modules.order.repository;

import com.badminton.shop.modules.order.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Xóa tất cả cart items có variant thuộc product đã bị xóa
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.variant.id IN :variantIds")
    void deleteAllByVariantIdIn(@Param("variantIds") List<Long> variantIds);
}
