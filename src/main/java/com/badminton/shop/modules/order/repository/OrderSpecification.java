package com.badminton.shop.modules.order.repository;

import com.badminton.shop.modules.order.entity.Order;
import com.badminton.shop.modules.order.entity.OrderStatus;
import com.badminton.shop.modules.order.entity.PaymentMethod;
import com.badminton.shop.modules.order.entity.PaymentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> filterOrders(
            String keyword,
            String status,
            String paymentStatus,
            String paymentMethod,
            LocalDateTime from,
            LocalDateTime to) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String searchKeyword = "%" + keyword.toLowerCase() + "%";
                Predicate orderCodePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("orderCode")), searchKeyword);
                Predicate receiverNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("receiverName")), searchKeyword);
                Predicate receiverPhonePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("receiverPhone")), searchKeyword);
                predicates.add(criteriaBuilder.or(orderCodePredicate, receiverNamePredicate, receiverPhonePredicate));
            }

            if (StringUtils.hasText(status)) {
                try {
                    OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), orderStatus));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid status
                }
            }

            if (StringUtils.hasText(paymentStatus)) {
                try {
                    PaymentStatus payStatus = PaymentStatus.valueOf(paymentStatus.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("paymentStatus"), payStatus));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid
                }
            }

            if (StringUtils.hasText(paymentMethod)) {
                try {
                    PaymentMethod payMethod = PaymentMethod.valueOf(paymentMethod.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("paymentMethod"), payMethod));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid
                }
            }

            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
