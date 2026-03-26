package com.badminton.shop.modules.membership.repository;

import com.badminton.shop.modules.membership.entity.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier, Long> {
    Optional<MembershipTier> findByName(String name);

    @Query("SELECT t FROM MembershipTier t WHERE t.minPoints <= :points ORDER BY t.minPoints DESC LIMIT 1")
    Optional<MembershipTier> findEligibleTierByPoints(Integer points);
    
    @Query("SELECT t FROM MembershipTier t ORDER BY t.minPoints ASC LIMIT 1")
    Optional<MembershipTier> findLowestTier();

    Optional<MembershipTier> findFirstByMinPointsGreaterThanOrderByMinPointsAsc(Integer points);
}
