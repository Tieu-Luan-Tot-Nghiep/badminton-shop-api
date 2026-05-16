package com.badminton.shop.modules.admin.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.admin.dto.AdminInventoryProductResponse;
import com.badminton.shop.modules.admin.dto.AdminInventoryVariantResponse;
import com.badminton.shop.modules.product.entity.Product;
import com.badminton.shop.modules.product.entity.ProductVariant;
import com.badminton.shop.modules.product.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventory/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {

    private final ProductRepository productRepository;

    public AdminInventoryController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<AdminInventoryProductResponse>>> getProducts() {
        List<Product> products = productRepository.findAllWithVariants();

        List<AdminInventoryProductResponse> response = products.stream()
                .map(p -> {
                    List<AdminInventoryVariantResponse> variants = p.getVariants().stream()
                            .map(v -> new AdminInventoryVariantResponse(
                                    v.getId(),
                                    buildVariantName(v),
                                    v.getStock()
                            ))
                            .collect(Collectors.toList());

                    int total = variants.stream()
                            .mapToInt(v -> v.getQuantity() == null ? 0 : v.getQuantity())
                            .sum();

                    return new AdminInventoryProductResponse(p.getId(), p.getName(), total, variants);
                })
                .sorted(Comparator.comparingInt(AdminInventoryProductResponse::getTotalQuantity))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Get inventory successful", response));
    }

    private String buildVariantName(ProductVariant v) {
        StringBuilder sb = new StringBuilder();
        if (v.getSize() != null) sb.append(v.getSize());
        if (v.getColor() != null) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(v.getColor());
        }
        if (sb.length() == 0) {
            return v.getSku();
        }
        return sb.toString();
    }
}
