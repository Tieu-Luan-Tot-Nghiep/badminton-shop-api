package com.badminton.shop.modules.product.repository;

import com.badminton.shop.modules.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
	boolean existsBySku(String sku);
}
