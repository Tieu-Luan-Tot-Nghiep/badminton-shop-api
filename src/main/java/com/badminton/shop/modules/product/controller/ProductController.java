package com.badminton.shop.modules.product.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.product.dto.PagedResponse;
import com.badminton.shop.modules.product.dto.ProductImageRequest;
import com.badminton.shop.modules.product.dto.ProductImageResponse;
import com.badminton.shop.modules.product.dto.ProductListResponse;
import com.badminton.shop.modules.product.dto.ProductRequest;
import com.badminton.shop.modules.product.dto.ProductResponse;
import com.badminton.shop.modules.product.dto.ProductCompareResponse;
import com.badminton.shop.modules.product.dto.ProductVariantRequest;
import com.badminton.shop.modules.product.dto.ProductVariantResponse;
import com.badminton.shop.modules.product.dto.WishlistItemResponse;
import com.badminton.shop.modules.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ===== Public APIs =====

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductListResponse>>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductListResponse> products = productService.getPublicProducts(
            category, brand, minPrice, maxPrice, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully.", PagedResponse.from(products)));
        }

    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<PagedResponse<ProductListResponse>>> getProductsByCategoryId(
            @PathVariable Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductListResponse> products = productService.getPublicProductsByCategoryId(
                categoryId, brand, minPrice, maxPrice, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully.", PagedResponse.from(products)));
    }

    @GetMapping("/search/existsBySlug")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkSlug(@RequestParam String slug) {
        boolean exists = productService.existsBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(
                "Slug availability checked successfully.",
                Collections.singletonMap("exists", exists)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Product fetched successfully.", productService.getPublicProductById(id)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductListResponse>>> getFeaturedProducts(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Featured products fetched successfully.", productService.getFeaturedProducts(limit)));
    }

    @GetMapping("/new")
    public ResponseEntity<ApiResponse<List<ProductListResponse>>> getNewestProducts(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Newest products fetched successfully.", productService.getNewestProducts(limit)));
    }

    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<ProductCompareResponse>> compareProducts(
            @RequestParam List<Long> variantIds) {
        return ResponseEntity.ok(ApiResponse.success(
                "Product compare data fetched successfully.",
                productService.compareVariants(variantIds)
        ));
    }

    @GetMapping("/wishlist")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> getMyWishlist(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Wishlist fetched successfully.",
                productService.getMyWishlist(principal.getName())
        ));
    }

    @PostMapping("/wishlist/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> addToWishlist(
            Principal principal,
            @PathVariable Long productId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                HttpStatus.CREATED,
                "Added to wishlist successfully.",
                productService.addToWishlist(principal.getName(), productId)
        ));
    }

    @DeleteMapping("/wishlist/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> removeFromWishlist(
            Principal principal,
            @PathVariable Long productId) {
        productService.removeFromWishlist(principal.getName(), productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist successfully.", null));
    }

    @GetMapping("/wishlist/{productId}/exists")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> isInWishlist(
            Principal principal,
            @PathVariable Long productId) {
        boolean exists = productService.isInWishlist(principal.getName(), productId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist status fetched successfully.", Map.of("exists", exists)));
    }

    // ===== Admin APIs =====

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<ProductListResponse>>> getAdminProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Sort sort = buildAdminSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductListResponse> products = productService.getAdminProducts(
                category, brand, minPrice, maxPrice, keyword, isActive, isDeleted, pageable);
        return ResponseEntity.ok(ApiResponse.success("Admin products fetched successfully.", PagedResponse.from(products)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Product created successfully.", productService.createProduct(request)));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> createProducts(
            @Valid @RequestBody List<@Valid ProductRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Products created successfully.", productService.createProducts(requests)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully.", productService.updateProduct(id, request)));
    }

    @PostMapping("/{id}/thumbnail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> uploadThumbnail(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Product thumbnail uploaded successfully.", productService.uploadThumbnail(id, file)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Product status toggled successfully.", productService.toggleStatus(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully.", null));
    }

    @GetMapping("/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getProductVariants(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Product variants fetched successfully.", productService.getProductVariants(productId)));
    }

    @PostMapping("/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createProductVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Product variant created successfully.", productService.createProductVariant(productId, request)));
    }

        @PostMapping("/{productId}/variants/bulk")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> createProductVariants(
            @PathVariable Long productId,
            @Valid @RequestBody List<@Valid ProductVariantRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(HttpStatus.CREATED, "Product variants created successfully.", productService.createProductVariants(productId, requests)));
        }

    @PutMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateProductVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Product variant updated successfully.",
                productService.updateProductVariant(productId, variantId, request)
        ));
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteProductVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        productService.deleteProductVariant(productId, variantId);
        return ResponseEntity.ok(ApiResponse.success("Product variant deleted successfully.", null));
    }

    @GetMapping("/{productId}/images")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getProductImages(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Product images fetched successfully.", productService.getProductImages(productId)));
    }

    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductImageResponse>> uploadProductImage(
            @PathVariable Long productId,
            @ModelAttribute @Valid ProductImageRequest request,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Product image uploaded successfully.", productService.uploadProductImage(productId, request, file)));
    }

        @PostMapping(value = "/{productId}/images/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<List<ProductImageResponse>>> uploadProductImages(
            @PathVariable Long productId,
            @RequestPart("metadata") @Valid List<@Valid ProductImageRequest> requests,
            @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(HttpStatus.CREATED, "Product images uploaded successfully.", productService.uploadProductImages(productId, requests, files)));
        }

    @PutMapping("/{productId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductImageResponse>> updateProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @Valid @RequestBody ProductImageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Product image updated successfully.",
                productService.updateProductImage(productId, imageId, request)
        ));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        productService.deleteProductImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Product image deleted successfully.", null));
    }

    // ===== Helpers =====

    private Sort buildSort(String sortBy, String sortDir) {
        // Hỗ trợ sort shortcuts: price_asc, price_desc, newest, name_asc
        return switch (sortBy) {
            case "price" -> Sort.by(sortDir.equalsIgnoreCase("asc")
                    ? Sort.Order.asc("basePrice") : Sort.Order.desc("basePrice"));
            case "name" -> Sort.by(sortDir.equalsIgnoreCase("asc")
                    ? Sort.Order.asc("name") : Sort.Order.desc("name"));
            default -> Sort.by(sortDir.equalsIgnoreCase("asc")
                    ? Sort.Order.asc("createdAt") : Sort.Order.desc("createdAt"));
        };
    }

    private Sort buildAdminSort(String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        return switch (sortBy) {
            case "price" -> JpaSort.unsafe(direction, "basePrice");
            case "name" -> JpaSort.unsafe(direction, "name");
            case "updatedAt" -> JpaSort.unsafe(direction, "updatedAt");
            case "id" -> JpaSort.unsafe(direction, "id");
            default -> JpaSort.unsafe(direction, "createdAt");
        };
    }
}
