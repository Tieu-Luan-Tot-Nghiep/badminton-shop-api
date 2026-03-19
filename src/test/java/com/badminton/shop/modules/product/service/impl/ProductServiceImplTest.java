package com.badminton.shop.modules.product.service.impl;

import com.badminton.shop.modules.order.repository.CartItemRepository;
import com.badminton.shop.modules.product.dto.ProductRequest;
import com.badminton.shop.modules.product.dto.ProductResponse;
import com.badminton.shop.modules.product.entity.Brand;
import com.badminton.shop.modules.product.entity.Category;
import com.badminton.shop.modules.product.entity.Product;
import com.badminton.shop.modules.product.entity.ProductVariant;
import com.badminton.shop.modules.product.repository.BrandRepository;
import com.badminton.shop.modules.product.repository.CategoryRepository;
import com.badminton.shop.modules.product.repository.ProductRepository;
import com.badminton.shop.modules.search.event.ProductSearchSyncAction;
import com.badminton.shop.modules.search.event.ProductSearchSyncEvent;
import com.badminton.shop.utils.s3.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private S3Service s3Service;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProduct_ShouldSaveAndPublishUpsertEvent() {
        ProductRequest request = ProductRequest.builder()
                .name("Yonex Astrox 88D")
                .shortDescription("Vot danh cho danh doi")
                .description("Mo ta")
                .basePrice(BigDecimal.valueOf(3_500_000))
                .categoryId(10L)
                .brandId(20L)
                .build();

        Category category = Category.builder().id(10L).name("Racket").slug("racket").build();
        Brand brand = Brand.builder().id(20L).name("Yonex").slug("yonex").build();

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(20L)).thenReturn(Optional.of(brand));
        when(productRepository.existsBySlug("yonex-astrox-88d")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(101L);
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("yonex-astrox-88d", response.getSlug());
        assertEquals("Yonex", response.getBrandName());
        assertEquals("Racket", response.getCategoryName());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ProductSearchSyncEvent event = (ProductSearchSyncEvent) eventCaptor.getValue();
        assertEquals(101L, event.productId());
        assertEquals(ProductSearchSyncAction.UPSERT, event.action());
    }

    @Test
    void deleteProduct_ShouldDeleteCartItemsMarkVariantsAndPublishDeleteEvent() {
        ProductVariant v1 = ProductVariant.builder().id(1L).sku("SKU-1").size("L").color("Black").price(100.0).originalPrice(100.0).build();
        ProductVariant v2 = ProductVariant.builder().id(2L).sku("SKU-2").size("M").color("White").price(120.0).originalPrice(120.0).build();

        Product product = Product.builder()
                .id(501L)
                .name("Test Product")
                .slug("test-product")
                .build();
        product.addProductVariant(v1);
        product.addProductVariant(v2);

        when(productRepository.findById(501L)).thenReturn(Optional.of(product));
        doNothing().when(cartItemRepository).deleteAllByVariantIdIn(anyList());

        productService.deleteProduct(501L);

        verify(cartItemRepository).deleteAllByVariantIdIn(eq(List.of(1L, 2L)));
        verify(productRepository).delete(product);

        assertTrue(v1.getIsDeleted());
        assertTrue(v2.getIsDeleted());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ProductSearchSyncEvent event = (ProductSearchSyncEvent) eventCaptor.getValue();
        assertEquals(501L, event.productId());
        assertEquals(ProductSearchSyncAction.DELETE, event.action());
        assertFalse(product.getVariants().isEmpty());
    }

    @Test
    void getFeaturedProducts_ShouldUseSafeLimitAndMapResponse() {
        Product featured = Product.builder()
                .id(11L)
                .name("Featured Product")
                .slug("featured-product")
                .basePrice(BigDecimal.valueOf(1_000_000))
                .brand(Brand.builder().name("Yonex").build())
                .category(Category.builder().name("Racket").build())
                .build();

        when(productRepository.findFeaturedProducts(PageRequest.of(0, 50))).thenReturn(List.of(featured));

        var results = productService.getFeaturedProducts(999);

        assertEquals(1, results.size());
        assertEquals("Featured Product", results.getFirst().getName());
        verify(productRepository).findFeaturedProducts(PageRequest.of(0, 50));
    }

    @Test
    void getNewestProducts_ShouldUseDefaultLowerBoundLimit() {
        Product newest = Product.builder()
                .id(22L)
                .name("Newest Product")
                .slug("newest-product")
                .build();

        when(productRepository.findNewestProducts(PageRequest.of(0, 1))).thenReturn(List.of(newest));

        var results = productService.getNewestProducts(0);

        assertEquals(1, results.size());
        assertEquals("Newest Product", results.getFirst().getName());
        verify(productRepository).findNewestProducts(PageRequest.of(0, 1));
    }
}
