package com.badminton.shop.repository;

import com.badminton.shop.entity.Payment;
import com.badminton.shop.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionCode(String transactionCode);

    boolean existsByTransactionCode(String transactionCode);

    boolean existsByOrderId(Long orderId);

    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
