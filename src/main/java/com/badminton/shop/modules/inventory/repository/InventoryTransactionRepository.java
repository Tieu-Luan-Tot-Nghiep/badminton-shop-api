package com.badminton.shop.modules.inventory.repository;

import com.badminton.shop.modules.inventory.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    Page<InventoryTransaction> findAllByVariantIdOrderByCreatedAtDesc(Long variantId, Pageable pageable);
}
