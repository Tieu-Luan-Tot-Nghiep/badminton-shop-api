package com.badminton.shop.modules.order.repository;

import com.badminton.shop.modules.order.entity.OrderReturnRequest;
import com.badminton.shop.modules.order.entity.ReturnRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderReturnRequestRepository extends JpaRepository<OrderReturnRequest, Long>, JpaSpecificationExecutor<OrderReturnRequest> {

    @EntityGraph(attributePaths = {
            "order", "order.items", "order.items.variant", "order.items.variant.product", "order.user",
            "user", "items", "items.orderItem", "items.orderItem.variant", "items.orderItem.variant.product"
    })
    Optional<OrderReturnRequest> findById(Long id);

    boolean existsByOrderIdAndStatusIn(Long orderId, Collection<ReturnRequestStatus> statuses);
}
