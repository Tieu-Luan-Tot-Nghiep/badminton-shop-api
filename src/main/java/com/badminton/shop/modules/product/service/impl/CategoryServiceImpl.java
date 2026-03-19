package com.badminton.shop.modules.product.service.impl;

import com.badminton.shop.exception.CategoryHasChildrenException;
import com.badminton.shop.exception.DuplicateCategoryException;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.product.dto.CategoryRequest;
import com.badminton.shop.modules.product.dto.CategoryResponse;
import com.badminton.shop.modules.product.entity.Category;
import com.badminton.shop.modules.product.repository.CategoryRepository;
import com.badminton.shop.modules.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

        private static final String CACHE_CATEGORY_TREE = "categoryTree";
        private static final String CACHE_CATEGORY_BY_SLUG = "categoryBySlug";

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
        @Cacheable(cacheNames = CACHE_CATEGORY_TREE, key = "'all'")
    public List<CategoryResponse> getAllCategoriesTree() {
        List<Category> rootCategories = categoryRepository.findAllByParentCategoryIsNull();
        return rootCategories.stream()
                .map(this::mapToResponseTree)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_CATEGORY_BY_SLUG, key = "#slug")
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với slug: " + slug));
        return mapToResponseTree(category);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CATEGORY_TREE, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CATEGORY_BY_SLUG, allEntries = true)
    })
    public CategoryResponse createCategory(CategoryRequest request) {
        Category parentCategory = null;

        if (request.getParentId() != null) {
            parentCategory = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy danh mục cha với id: " + request.getParentId()));
        }

        // Validate duplicate name at the same level
        if (categoryRepository.existsByNameAndParentCategory(request.getName(), parentCategory)) {
            throw new DuplicateCategoryException(
                    "Tên danh mục '" + request.getName() + "' đã tồn tại ở cùng cấp độ");
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(generateSlug(request.getName()))
                .description(request.getDescription())
                .parentCategory(parentCategory)
                .build();

        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CATEGORY_TREE, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CATEGORY_BY_SLUG, allEntries = true)
    })
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với id: " + id));

        Category parentCategory = null;
        if (request.getParentId() != null) {
            parentCategory = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy danh mục cha với id: " + request.getParentId()));
        }

        // Validate duplicate name at the same level (excluding itself)
        if (categoryRepository.existsByNameAndParentCategoryAndIdNot(request.getName(), parentCategory, id)) {
            throw new DuplicateCategoryException(
                    "Tên danh mục '" + request.getName() + "' đã tồn tại ở cùng cấp độ");
        }

        category.setName(request.getName());
        category.setSlug(generateSlug(request.getName()));
        category.setDescription(request.getDescription());
        category.setParentCategory(parentCategory);

        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_CATEGORY_TREE, allEntries = true),
            @CacheEvict(cacheNames = CACHE_CATEGORY_BY_SLUG, allEntries = true)
    })
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với id: " + id));

        // Block deletion if category still has active children
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            throw new CategoryHasChildrenException(
                    "Không thể xóa danh mục '" + category.getName()
                            + "' vì vẫn còn danh mục con đang hoạt động");
        }

        categoryRepository.delete(category);
    }

    // ===== Helper methods =====

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("");
        slug = slug.toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug;
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(category.getParentCategory() != null ? category.getParentCategory().getId() : null)
                .build();
    }

    private CategoryResponse mapToResponseTree(Category category) {
        List<CategoryResponse> children = null;
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            children = category.getSubCategories().stream()
                    .map(this::mapToResponseTree)
                    .collect(Collectors.toList());
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(category.getParentCategory() != null ? category.getParentCategory().getId() : null)
                .children(children)
                .build();
    }
}
