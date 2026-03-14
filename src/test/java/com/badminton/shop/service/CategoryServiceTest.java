package com.badminton.shop.service;

import com.badminton.shop.dto.request.CategoryRequest;
import com.badminton.shop.dto.response.CategoryResponse;
import com.badminton.shop.entity.Category;
import com.badminton.shop.exception.DuplicateResourceException;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.repository.CategoryRepository;
import com.badminton.shop.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Vợt Cầu Lông")
                .slug("vot-cau-long")
                .description("Các loại vợt cầu lông")
                .active(true)
                .build();
    }

    @Test
    void getAllActiveCategories_shouldReturnActiveCategoriesOnly() {
        when(categoryRepository.findAllByActiveTrue()).thenReturn(List.of(category));

        List<CategoryResponse> result = categoryService.getAllActiveCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Vợt Cầu Lông");
        assertThat(result.get(0).getSlug()).isEqualTo("vot-cau-long");
    }

    @Test
    void getCategoryById_whenExists_shouldReturnCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse result = categoryService.getCategoryById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Vợt Cầu Lông");
    }

    @Test
    void getCategoryById_whenNotExists_shouldThrowResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Danh mục");
    }

    @Test
    void createCategory_whenSlugNotExists_shouldCreateSuccessfully() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Giầy Cầu Lông");

        when(categoryRepository.existsBySlug(any())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(
                Category.builder()
                        .id(2L)
                        .name("Giầy Cầu Lông")
                        .slug("giay-cau-long")
                        .active(true)
                        .build()
        );

        CategoryResponse result = categoryService.createCategory(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Giầy Cầu Lông");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void createCategory_whenSlugExists_shouldThrowDuplicateResourceException() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Vợt Cầu Lông");

        when(categoryRepository.existsBySlug("vot-cau-long")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_whenExists_shouldDeactivateCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        categoryService.deleteCategory(1L);

        verify(categoryRepository, times(1)).save(argThat(c -> !c.isActive()));
    }
}
