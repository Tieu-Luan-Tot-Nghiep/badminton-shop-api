package com.badminton.shop.modules.product.controller;

import com.badminton.shop.modules.product.dto.PagedResponse;
import com.badminton.shop.modules.product.dto.ProductImageRequest;
import com.badminton.shop.modules.product.dto.ProductImageResponse;
import com.badminton.shop.modules.product.dto.ProductListResponse;
import com.badminton.shop.modules.product.dto.ProductRequest;
import com.badminton.shop.modules.product.dto.ProductResponse;
import com.badminton.shop.modules.product.dto.ProductVariantRequest;
import com.badminton.shop.modules.product.dto.ProductVariantResponse;
import com.badminton.shop.modules.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
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
    public ResponseEntity<PagedResponse<ProductListResponse>> getProducts(
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
        return ResponseEntity.ok(PagedResponse.from(products));
        }

    @GetMapping("/search/existsBySlug")
    public ResponseEntity<Map<String, Boolean>> checkSlug(@RequestParam String slug) {
        boolean exists = productService.existsBySlug(slug);
        return ResponseEntity.ok(Collections.singletonMap("exists", exists));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getPublicProductBySlug(slug));
    }

    // ===== Admin APIs =====

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return new ResponseEntity<>(productService.createProduct(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PostMapping("/{id}/thumbnail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> uploadThumbnail(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productService.uploadThumbnail(id, file));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductVariantResponse>> getProductVariants(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductVariants(productId));
    }

    @PostMapping("/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> createProductVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        return new ResponseEntity<>(productService.createProductVariant(productId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> updateProductVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(productService.updateProductVariant(productId, variantId, request));
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProductVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        productService.deleteProductVariant(productId, variantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{productId}/images")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductImageResponse>> getProductImages(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductImages(productId));
    }

    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductImageResponse> uploadProductImage(
            @PathVariable Long productId,
            @ModelAttribute @Valid ProductImageRequest request,
            @RequestParam("file") MultipartFile file) {
        return new ResponseEntity<>(productService.uploadProductImage(productId, request, file), HttpStatus.CREATED);
    }

    @PutMapping("/{productId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductImageResponse> updateProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @Valid @RequestBody ProductImageRequest request) {
        return ResponseEntity.ok(productService.updateProductImage(productId, imageId, request));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        productService.deleteProductImage(productId, imageId);
        return ResponseEntity.noContent().build();
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
}
