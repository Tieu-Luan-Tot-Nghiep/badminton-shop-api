package com.badminton.shop.modules.product.service;

import com.badminton.shop.modules.product.dto.ProductImageRequest;
import com.badminton.shop.modules.product.dto.ProductImageResponse;
import com.badminton.shop.modules.product.dto.ProductListResponse;
import com.badminton.shop.modules.product.dto.ProductRequest;
import com.badminton.shop.modules.product.dto.ProductResponse;
import com.badminton.shop.modules.product.dto.ProductVariantRequest;
import com.badminton.shop.modules.product.dto.ProductVariantResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    // Public APIs
    Page<ProductListResponse> getPublicProducts(
            String category, String brand,
            BigDecimal minPrice, BigDecimal maxPrice,
            String keyword, Pageable pageable);

    boolean existsBySlug(String slug);

    ProductResponse getPublicProductBySlug(String slug);

    List<ProductListResponse> getFeaturedProducts(int limit);

    List<ProductListResponse> getNewestProducts(int limit);

    // Admin APIs
    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    List<ProductVariantResponse> getProductVariants(Long productId);

    ProductVariantResponse createProductVariant(Long productId, ProductVariantRequest request);

    ProductVariantResponse updateProductVariant(Long productId, Long variantId, ProductVariantRequest request);

    void deleteProductVariant(Long productId, Long variantId);

    List<ProductImageResponse> getProductImages(Long productId);

    ProductImageResponse uploadProductImage(Long productId, ProductImageRequest request, MultipartFile file);

    ProductImageResponse updateProductImage(Long productId, Long imageId, ProductImageRequest request);

    void deleteProductImage(Long productId, Long imageId);

    ProductResponse uploadThumbnail(Long id, MultipartFile file);

    ProductResponse toggleStatus(Long id);

    void deleteProduct(Long id);
}
