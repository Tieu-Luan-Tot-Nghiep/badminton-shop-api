package com.badminton.shop.modules.auth.repository;

import com.badminton.shop.modules.auth.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findAllByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE UserAddress ua SET ua.isDeleted = true WHERE ua.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
