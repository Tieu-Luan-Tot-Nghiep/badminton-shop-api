package com.badminton.shop.modules.search.controller;

import com.badminton.shop.modules.search.dto.ProductSearchPageResponse;
import com.badminton.shop.modules.search.dto.ProductSearchSuggestionResponse;
import com.badminton.shop.modules.search.dto.ProductSearchTrendingResponse;
import com.badminton.shop.modules.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/search/products")
@RequiredArgsConstructor
@CrossOrigin(origins = {
    "http://localhost:63342",
    "http://127.0.0.1:63342",
    "http://localhost:8080",
    "http://127.0.0.1:8080"
})
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @GetMapping
    public ResponseEntity<ProductSearchPageResponse> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "true") Boolean activeOnly,
            @RequestParam(defaultValue = "false") Boolean useSemantic
    ) {
        ProductSearchPageResponse response = productSearchService.searchProducts(
                keyword, category, brand, minPrice, maxPrice, sortBy, sortDir, page, size, activeOnly, useSemantic
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> reindexProducts() {
        productSearchService.reindexAllProducts();
        return ResponseEntity.ok(Map.of("message", "Product index reindex completed"));
    }

    @PostMapping(value = "/by-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductSearchPageResponse> searchProductsByImage(
            @RequestPart("image") MultipartFile image,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "true") Boolean activeOnly
    ) {
        ProductSearchPageResponse response = productSearchService.searchProductsByImage(image, page, size, activeOnly);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/similar")
    public ResponseEntity<ProductSearchPageResponse> suggestSimilarProducts(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "true") Boolean activeOnly
    ) {
        ProductSearchPageResponse response = productSearchService.suggestSimilarProducts(productId, page, size, activeOnly);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ProductSearchSuggestionResponse> suggestKeywords(
            @RequestParam String query,
            @RequestParam(defaultValue = "8") int size
    ) {
        ProductSearchSuggestionResponse response = productSearchService.suggestKeywords(query, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trending")
    public ResponseEntity<ProductSearchTrendingResponse> getTrendingSearches(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int size
    ) {
        ProductSearchTrendingResponse response = productSearchService.getTrendingSearches(days, size);
        return ResponseEntity.ok(response);
    }
}
