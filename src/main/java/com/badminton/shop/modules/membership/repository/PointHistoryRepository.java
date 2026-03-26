package com.badminton.shop.modules.membership.repository;

import com.badminton.shop.modules.membership.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndReferenceIdAndReason(Long userId, Long referenceId, String reason);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointHistory p WHERE p.user.id = :userId AND p.referenceId = :referenceId AND p.reason = :reason")
    Integer sumPointsByUserIdAndReferenceIdAndReason(
            @Param("userId") Long userId,
            @Param("referenceId") Long referenceId,
            @Param("reason") String reason
    );

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointHistory p WHERE p.user.id = :userId AND p.referenceId = :referenceId AND p.reason IN :reasons")
    Integer sumPointsByUserIdAndReferenceIdAndReasons(
            @Param("userId") Long userId,
            @Param("referenceId") Long referenceId,
            @Param("reasons") List<String> reasons
    );
}
