package com.badminton.shop.modules.order.repository;

import com.badminton.shop.modules.order.entity.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @EntityGraph(attributePaths = {"order", "order.user", "variant", "variant.product", "review"})
    Optional<OrderItem> findWithDetailsById(Long id);
}
