package com.badminton.shop.modules.product.repository;

import com.badminton.shop.modules.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByParentCategoryIsNull();

    Optional<Category> findBySlug(String slug);

    boolean existsByNameAndParentCategory(String name, Category parentCategory);

    boolean existsByNameAndParentCategoryAndIdNot(String name, Category parentCategory, Long id);
}
