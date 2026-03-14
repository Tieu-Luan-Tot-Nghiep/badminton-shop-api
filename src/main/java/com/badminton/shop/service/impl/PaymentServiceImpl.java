package com.badminton.shop.service.impl;

import com.badminton.shop.dto.response.PaymentResponse;
import com.badminton.shop.entity.Payment;
import com.badminton.shop.enums.PaymentMethod;
import com.badminton.shop.enums.PaymentStatus;
import com.badminton.shop.exception.BadRequestException;
import com.badminton.shop.exception.BusinessException;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.repository.OrderRepository;
import com.badminton.shop.repository.PaymentRepository;
import com.badminton.shop.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Thanh toán", "orderId", orderId));
        return toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(Long orderId, String transactionCode) {
        log.info("Xác nhận thanh toán cho đơn hàng ID: {}, transactionCode: {}", orderId, transactionCode);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Thanh toán", "orderId", orderId));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new BusinessException("PAYMENT_ALREADY_COMPLETED",
                    "Đơn hàng ID " + orderId + " đã được thanh toán");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("INVALID_PAYMENT_STATUS",
                    "Trạng thái thanh toán hiện tại không cho phép xác nhận: " + payment.getStatus());
        }

        if (paymentRepository.existsByTransactionCode(transactionCode)) {
            throw new BadRequestException("Mã giao dịch '" + transactionCode + "' đã được sử dụng");
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionCode(transactionCode);
        payment.setPaidAt(LocalDateTime.now());

        Payment updated = paymentRepository.save(payment);
        log.info("Xác nhận thanh toán thành công cho đơn hàng ID: {}, transactionCode: {}",
                orderId, transactionCode);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(Long orderId, PaymentMethod method) {
        log.info("Xử lý thanh toán cho đơn hàng ID: {}, phương thức: {}", orderId, method);

        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Đơn hàng", "id", orderId);
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Thanh toán", "orderId", orderId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("INVALID_PAYMENT_STATUS",
                    "Đơn hàng không thể xử lý thanh toán ở trạng thái: " + payment.getStatus());
        }

        payment.setMethod(method);
        Payment updated = paymentRepository.save(payment);
        log.info("Cập nhật phương thức thanh toán thành công cho đơn hàng ID: {}", orderId);
        return toResponse(updated);
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .transactionCode(payment.getTransactionCode())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
