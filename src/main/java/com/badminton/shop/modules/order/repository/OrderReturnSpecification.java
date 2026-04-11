package com.badminton.shop.modules.order.repository;

import com.badminton.shop.modules.order.entity.OrderReturnRequest;
import com.badminton.shop.modules.order.entity.ReturnRequestStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class OrderReturnSpecification {

    public static Specification<OrderReturnRequest> filterReturns(
            String keyword,
            String status) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String searchKeyword = "%" + keyword.toLowerCase() + "%";
                // Lọc theo reason hoặc bankName
                Predicate reasonPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("reason")), searchKeyword);
                Predicate bankNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("bankName")), searchKeyword);
                
                // Cũng có thể lọc theo orderCode (root.join("order").get("orderCode"))
                Predicate orderCodePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.join("order").get("orderCode")), searchKeyword);
                
                predicates.add(criteriaBuilder.or(reasonPredicate, bankNamePredicate, orderCodePredicate));
            }

            if (StringUtils.hasText(status)) {
                try {
                    ReturnRequestStatus returnStatus = ReturnRequestStatus.valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), returnStatus));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid status
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
