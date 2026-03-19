package com.badminton.shop.modules.product.repository;

import com.badminton.shop.modules.product.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
