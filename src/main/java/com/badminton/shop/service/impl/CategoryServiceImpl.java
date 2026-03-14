package com.badminton.shop.service.impl;

import com.badminton.shop.dto.request.CategoryRequest;
import com.badminton.shop.dto.response.CategoryResponse;
import com.badminton.shop.entity.Category;
import com.badminton.shop.exception.DuplicateResourceException;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.repository.CategoryRepository;
import com.badminton.shop.service.CategoryService;
import com.badminton.shop.util.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findAllByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "slug", slug));
        return toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String slug = AppUtils.toSlug(request.getName());

        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Danh mục với tên '" + request.getName() + "' đã tồn tại");
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Tạo danh mục mới ID: {}, tên: {}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findById(id);
        String newSlug = AppUtils.toSlug(request.getName());

        if (!category.getSlug().equals(newSlug) && categoryRepository.existsBySlug(newSlug)) {
            throw new DuplicateResourceException("Danh mục với tên '" + request.getName() + "' đã tồn tại");
        }

        category.setName(request.getName());
        category.setSlug(newSlug);
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category updated = categoryRepository.save(category);
        log.info("Cập nhật danh mục ID: {}", updated.getId());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findById(id);
        category.setActive(false);
        categoryRepository.save(category);
        log.info("Vô hiệu hóa danh mục ID: {}", id);
    }

    private Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .active(category.isActive())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
