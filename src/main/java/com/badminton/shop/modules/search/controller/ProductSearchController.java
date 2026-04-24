package com.badminton.shop.modules.search.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.search.dto.ProductSearchPageResponse;
import com.badminton.shop.modules.search.dto.ProductSearchSuggestionResponse;
import com.badminton.shop.modules.search.dto.ProductSearchTrendingResponse;
import com.badminton.shop.modules.search.dto.SearchReindexMessage;
import com.badminton.shop.modules.search.service.ProductSearchService;
import com.badminton.shop.modules.search.service.SearchReindexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    private final SearchReindexService searchReindexService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProductSearchPageResponse>> searchProducts(
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
        return ResponseEntity.ok(ApiResponse.success("Product search completed successfully.", response));
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> reindexProducts(Authentication authentication) {
        try {
            String requestedBy = authentication.getName();
            String requestId = searchReindexService.startReindex(requestedBy);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Product reindex started successfully via RabbitMQ",
                    Map.of(
                            "requestId", requestId,
                            "status", "QUEUED",
                            "message", "Reindex process has been queued and will be processed in background"
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Failed to start product reindex: " + e.getMessage(),
                    Map.of("result", "error", "status", "failed", "error", e.getMessage())
            ));
        }
    }

    @GetMapping("/reindex/status/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SearchReindexMessage>> getReindexStatus(@PathVariable String requestId) {
        try {
            SearchReindexMessage status = searchReindexService.getReindexStatus(requestId);
            return ResponseEntity.ok(ApiResponse.success("Reindex status retrieved successfully", status));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Failed to get reindex status: " + e.getMessage(),
                    SearchReindexMessage.builder()
                            .requestId(requestId)
                            .status("ERROR")
                            .build()
            ));
        }
    }

    @GetMapping("/reindex/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveReindexRequests() {
        try {
            // This is a simple implementation - in production you might want to store active requests in Redis
            return ResponseEntity.ok(ApiResponse.success(
                    "Active reindex requests retrieved successfully",
                    Map.of(
                            "message", "Use specific requestId to check status",
                            "endpoint", "/api/search/products/reindex/status/{requestId}"
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Failed to get active reindex requests: " + e.getMessage(),
                    Map.of("error", e.getMessage())
            ));
        }
    }

    @PostMapping("/reindex/legacy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> reindexProductsLegacy() {
        try {
            CompletableFuture<Void> reindexFuture = productSearchService.reindexAllProducts();
            
            // Wait for completion with 2-minute timeout to avoid Cloudflare 524 error
            reindexFuture.get(2, TimeUnit.MINUTES);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Product index reindex completed successfully",
                    Map.of("result", "completed", "status", "success")
            ));
        } catch (java.util.concurrent.TimeoutException e) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Product reindex is running in background (timeout after 2 minutes)",
                    Map.of("result", "timeout", "status", "processing", "message", "Reindex operation continues in background to avoid Cloudflare timeout")
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Product reindex failed: " + e.getMessage(),
                    Map.of("result", "error", "status", "failed", "error", e.getMessage())
            ));
        }
    }

    @GetMapping("/debug")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugSearch(
            @RequestParam(defaultValue = "test") String keyword
    ) {
        try {
            // Test basic search
            ProductSearchPageResponse searchResult = productSearchService.searchProducts(
                    keyword, null, null, null, null, "createdAt", "desc", 0, 5, true, false
            );
            
            Map<String, Object> debugInfo = Map.of(
                    "keyword", keyword,
                    "totalResults", searchResult.getTotalElements(),
                    "results", searchResult.getContent().stream()
                            .map(item -> Map.of(
                                    "id", item.getId(),
                                    "name", item.getName(),
                                    "brandName", item.getBrandName() != null ? item.getBrandName() : "N/A",
                                    "categoryName", item.getCategoryName() != null ? item.getCategoryName() : "N/A"
                            ))
                            .toList()
            );
            
            return ResponseEntity.ok(ApiResponse.success("Debug search completed", debugInfo));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Debug search failed: " + e.getMessage(),
                    Map.of("error", e.getMessage(), "stackTrace", e.getStackTrace())
            ));
        }
    }

    @PostMapping(value = "/by-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductSearchPageResponse>> searchProductsByImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "true") Boolean activeOnly
    ) {
        ProductSearchPageResponse response = productSearchService.searchProductsByImage(image, page, size, activeOnly);
        return ResponseEntity.ok(ApiResponse.success("Image search completed successfully.", response));
    }

    @GetMapping("/similar")
    public ResponseEntity<ApiResponse<ProductSearchPageResponse>> suggestSimilarProducts(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "true") Boolean activeOnly
    ) {
        ProductSearchPageResponse response = productSearchService.suggestSimilarProducts(productId, page, size, activeOnly);
        return ResponseEntity.ok(ApiResponse.success("Similar product suggestions fetched successfully.", response));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<ProductSearchSuggestionResponse>> suggestKeywords(
            @RequestParam String query,
            @RequestParam(defaultValue = "8") int size
    ) {
        ProductSearchSuggestionResponse response = productSearchService.suggestKeywords(query, size);
        return ResponseEntity.ok(ApiResponse.success("Keyword suggestions fetched successfully.", response));
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<ProductSearchTrendingResponse>> getTrendingSearches(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int size
    ) {
        ProductSearchTrendingResponse response = productSearchService.getTrendingSearches(days, size);
        return ResponseEntity.ok(ApiResponse.success("Trending searches fetched successfully.", response));
    }
}
