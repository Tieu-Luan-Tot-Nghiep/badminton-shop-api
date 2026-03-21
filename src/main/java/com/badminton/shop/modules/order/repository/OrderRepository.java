package com.badminton.shop.modules.order.repository;

import com.badminton.shop.modules.order.entity.Order;
import com.badminton.shop.modules.order.entity.OrderStatus;
import com.badminton.shop.modules.order.entity.PaymentMethod;
import com.badminton.shop.modules.order.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	@EntityGraph(attributePaths = {"items", "items.variant", "items.variant.product"})
	Optional<Order> findByOrderCode(String orderCode);

	@EntityGraph(attributePaths = {"items", "items.variant", "items.variant.product"})
	Page<Order> findAllByUserId(Long userId, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "items", "items.variant", "items.variant.product"})
	List<Order> findAllByPaymentMethodAndPaymentStatusAndStatusInAndCreatedAtBefore(
			PaymentMethod paymentMethod,
			PaymentStatus paymentStatus,
			List<OrderStatus> statuses,
			LocalDateTime createdBefore
	);
}
