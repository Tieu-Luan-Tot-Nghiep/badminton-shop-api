package com.badminton.shop.modules.auth.repository;

import com.badminton.shop.modules.auth.entity.Role;
import com.badminton.shop.modules.auth.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> filterUsers(String keyword, String role, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String searchKeyword = "%" + keyword.toLowerCase() + "%";
                Predicate usernamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), searchKeyword);
                Predicate emailPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchKeyword);
                Predicate fullNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), searchKeyword);
                Predicate phonePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), searchKeyword);
                
                predicates.add(criteriaBuilder.or(usernamePredicate, emailPredicate, fullNamePredicate, phonePredicate));
            }

            if (StringUtils.hasText(role)) {
                try {
                    Role userRole = Role.valueOf(role.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("role"), userRole));
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
