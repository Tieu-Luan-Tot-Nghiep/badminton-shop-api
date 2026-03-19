package com.badminton.shop.modules.search.service;

import com.badminton.shop.modules.search.dto.ProductSearchPageResponse;
import com.badminton.shop.modules.search.dto.ProductSearchSuggestionResponse;
import com.badminton.shop.modules.search.dto.ProductSearchTrendingResponse;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public interface ProductSearchService {

    ProductSearchPageResponse searchProducts(
            String keyword,
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy,
            String sortDir,
            int page,
            int size,
            Boolean activeOnly,
            Boolean useSemantic
    );

    ProductSearchPageResponse semanticSearch(String query, int page, int size);

    ProductSearchPageResponse searchProductsByImage(MultipartFile image, int page, int size, Boolean activeOnly);

    ProductSearchPageResponse suggestSimilarProducts(Long productId, int page, int size, Boolean activeOnly);

    ProductSearchSuggestionResponse suggestKeywords(String query, int size);

    ProductSearchTrendingResponse getTrendingSearches(int days, int size);

    void upsertProduct(Long productId);

    void deleteProduct(Long productId);

    void reindexAllProducts();
}
