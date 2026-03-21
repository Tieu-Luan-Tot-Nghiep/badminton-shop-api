package com.badminton.shop.modules.inventory.repository;

import com.badminton.shop.modules.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByVariantId(Long variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockByVariantId(Long variantId);

    List<Inventory> findAllByAvailableQuantityLessThan(Integer threshold);
}
