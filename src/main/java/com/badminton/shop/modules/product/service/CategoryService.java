package com.badminton.shop.modules.product.service;

import com.badminton.shop.modules.product.dto.CategoryRequest;
import com.badminton.shop.modules.product.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategoriesTree();
    CategoryResponse getCategoryBySlug(String slug);
    CategoryResponse createCategory(CategoryRequest request);
    List<CategoryResponse> createCategories(List<CategoryRequest> requests);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
}
