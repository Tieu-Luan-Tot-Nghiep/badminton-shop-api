package com.badminton.shop.repository;

import com.badminton.shop.entity.Order;
import com.badminton.shop.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    Page<Order> findAllByUserId(Long userId, Pageable pageable);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findAllByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    boolean existsByOrderCode(String orderCode);
}
