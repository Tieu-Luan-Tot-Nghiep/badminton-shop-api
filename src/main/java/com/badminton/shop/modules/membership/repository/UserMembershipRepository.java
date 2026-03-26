package com.badminton.shop.modules.membership.repository;

import com.badminton.shop.modules.membership.entity.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserMembershipRepository extends JpaRepository<UserMembership, Long> {
    Optional<UserMembership> findByUserId(Long userId);
}
