package com.badminton.shop.modules.search.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.RangeBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.SortOptions;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.product.entity.Product;
import com.badminton.shop.modules.product.repository.ProductRepository;
import com.badminton.shop.modules.search.document.ProductSearchDocument;
import com.badminton.shop.modules.search.document.SearchQueryLogDocument;
import com.badminton.shop.modules.search.dto.ProductSearchFacetBucketResponse;
import com.badminton.shop.modules.search.dto.ProductSearchFacetsResponse;
import com.badminton.shop.modules.search.dto.ProductSearchItemResponse;
import com.badminton.shop.modules.search.dto.ProductSearchPageResponse;
import com.badminton.shop.modules.search.dto.ProductSearchSuggestionResponse;
import com.badminton.shop.modules.search.dto.ProductSearchTrendingItemResponse;
import com.badminton.shop.modules.search.dto.ProductSearchTrendingResponse;
import com.badminton.shop.modules.search.repository.ProductSearchRepository;
import com.badminton.shop.modules.search.repository.SearchQueryLogRepository;
import com.badminton.shop.modules.search.service.ProductSearchService;
import com.badminton.shop.modules.search.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final SearchQueryLogRepository searchQueryLogRepository;
    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final EmbeddingService embeddingService;

    private static final String PRODUCT_INDEX = "products";
    private static final String AGG_BRANDS = "agg_brands";
    private static final String AGG_CATEGORIES = "agg_categories";
    private static final String AGG_PRICE_RANGES = "agg_price_ranges";
    private static final String SEARCH_LOG_INDEX = "search_query_logs";
    private static final String AGG_TRENDING_KEYWORDS = "agg_trending_keywords";

    @Override
    @Transactional(readOnly = true)
    public ProductSearchPageResponse searchProducts(
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
    ) {
        logSearchKeyword(keyword);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean semanticRequested = Boolean.TRUE.equals(useSemantic) && hasKeyword;

        Query query = buildQuery(keyword, category, brand, minPrice, maxPrice, activeOnly);
        SearchHits<ProductSearchDocument> hits;

        try {
            hits = executeKeywordSearch(query, pageable, sortBy, sortDir, semanticRequested, keyword, true, true);
        } catch (RuntimeException primaryEx) {
            log.warn("[search] primary query failed; semanticRequested={}, keyword='{}', reason={}",
                    semanticRequested, safeLogKeyword(keyword), extractRootCauseMessage(primaryEx));

            if (semanticRequested) {
                try {
                    // Fallback 1: semantic fail -> lexical (disable KNN).
                    hits = executeKeywordSearch(query, pageable, sortBy, sortDir, false, keyword, true, true);
                    log.warn("[search] semantic fallback to lexical succeeded for keyword='{}'", safeLogKeyword(keyword));
                } catch (RuntimeException lexicalEx) {
                    log.warn("[search] lexical fallback failed for keyword='{}', reason={}",
                            safeLogKeyword(keyword), extractRootCauseMessage(lexicalEx));
                    try {
                        // Fallback 2: degrade mode (no aggs, no explicit sort) to bypass mapping conflicts.
                        hits = executeKeywordSearch(query, pageable, sortBy, sortDir, false, keyword, false, false);
                        log.warn("[search] degraded fallback succeeded for keyword='{}'", safeLogKeyword(keyword));
                    } catch (RuntimeException degradedEx) {
                        throw buildSearchFailure(primaryEx, lexicalEx, degradedEx);
                    }
                }
            } else {
                try {
                    // Lexical still fail -> degrade mode.
                    hits = executeKeywordSearch(query, pageable, sortBy, sortDir, false, keyword, false, false);
                    log.warn("[search] degraded fallback succeeded for keyword='{}'", safeLogKeyword(keyword));
                } catch (RuntimeException degradedEx) {
                    throw buildSearchFailure(primaryEx, null, degradedEx);
                }
            }
        }

        List<ProductSearchItemResponse> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toSearchItem)
                .toList();

        return toPageResponse(content, hits, safePage, safeSize);
    }

    private SearchHits<ProductSearchDocument> executeKeywordSearch(
            Query query,
            Pageable pageable,
            String sortBy,
            String sortDir,
            boolean semanticEnabled,
            String keyword,
            boolean includeAggregations,
            boolean includeSort
    ) {
        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(query)
                .withPageable(pageable);

        if (includeAggregations) {
            addFacetAggregations(queryBuilder);
        }

        if (semanticEnabled) {
            float[] queryVector = embeddingService.embed(keyword);
            queryBuilder.withKnnSearches(k -> k
                    .field("my_vector")
                    .queryVector(toFloatList(queryVector))
                    .k(100)
                    .numCandidates(1000)
            );
        }

        if (includeSort) {
            for (SortOptions sortOption : buildSort(sortBy, sortDir)) {
                queryBuilder.withSort(sortOption);
            }
        }

        return elasticsearchOperations.search(
                queryBuilder.build(),
                ProductSearchDocument.class,
                IndexCoordinates.of(PRODUCT_INDEX)
        );
    }

    private IllegalStateException buildSearchFailure(RuntimeException primaryEx, RuntimeException secondaryEx, RuntimeException finalEx) {
        String primaryMessage = extractRootCauseMessage(primaryEx);
        String secondaryMessage = secondaryEx == null ? "N/A" : extractRootCauseMessage(secondaryEx);
        String finalMessage = extractRootCauseMessage(finalEx);

        log.error("[search] failed after all fallbacks. primary='{}', secondary='{}', final='{}'",
                primaryMessage, secondaryMessage, finalMessage, finalEx);

        return new IllegalStateException(
                "Search backend is unavailable or index mapping is incompatible. " +
                        "Primary: " + primaryMessage + "; " +
                        "Secondary: " + secondaryMessage + "; " +
                        "Final: " + finalMessage,
                finalEx
        );
    }

    private String extractRootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return (message == null || message.isBlank()) ? current.getClass().getSimpleName() : message;
    }

    private String safeLogKeyword(String keyword) {
        if (keyword == null) {
            return "<null>";
        }
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        return normalized.length() > 100 ? normalized.substring(0, 100) + "..." : normalized;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSearchPageResponse semanticSearch(String query, int page, int size) {
        logSearchKeyword(query);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        float[] queryVector = embeddingService.embed(query);

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .filter(f -> f.term(t -> t.field("isDeleted").value(false)))
                        .filter(f -> f.term(t -> t.field("isActive").value(true)))
                ))
            .withKnnSearches(k -> k
                .field("my_vector")
                .queryVector(toFloatList(queryVector))
                .k(100)
                .numCandidates(1000)
            )
                .withAggregation(AGG_BRANDS, buildBrandAggregation())
                .withAggregation(AGG_CATEGORIES, buildCategoryAggregation())
                .withAggregation(AGG_PRICE_RANGES, buildPriceRangeAggregation())
                .withPageable(pageable)
                .build();

        SearchHits<ProductSearchDocument> hits = elasticsearchOperations.search(
                nativeQuery,
                ProductSearchDocument.class,
                IndexCoordinates.of(PRODUCT_INDEX)
        );

        List<ProductSearchItemResponse> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toSearchItem)
                .toList();

        return toPageResponse(content, hits, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSearchPageResponse searchProductsByImage(MultipartFile image, int page, int size, Boolean activeOnly) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image is required for image search");
        }
        if (!embeddingService.isImageEmbeddingAvailable()) {
            throw new IllegalStateException("CLIP image embedding provider is not configured");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        float[] queryVector;
        try {
            queryVector = embeddingService.embedImage(image.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Cannot read image file for search", e);
        }
        if (!hasNonZeroMagnitude(queryVector)) {
            throw new IllegalArgumentException("Cannot generate valid image embedding from uploaded file");
        }

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    b.filter(f -> f.term(t -> t.field("isDeleted").value(false)));
                    if (activeOnly != null) {
                        b.filter(f -> f.term(t -> t.field("isActive").value(activeOnly)));
                    }
                    return b;
                }))
                .withKnnSearches(k -> k
                        .field("clip_image_vector")
                        .queryVector(toFloatList(queryVector))
                        .k(100)
                        .numCandidates(1000)
                )
                .withAggregation(AGG_BRANDS, buildBrandAggregation())
                .withAggregation(AGG_CATEGORIES, buildCategoryAggregation())
                .withAggregation(AGG_PRICE_RANGES, buildPriceRangeAggregation())
                .withPageable(pageable)
                .build();

        SearchHits<ProductSearchDocument> hits = elasticsearchOperations.search(
                nativeQuery,
                ProductSearchDocument.class,
                IndexCoordinates.of(PRODUCT_INDEX)
        );

        List<ProductSearchItemResponse> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toSearchItem)
                .toList();

        return toPageResponse(content, hits, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSearchPageResponse suggestSimilarProducts(Long productId, int page, int size, Boolean activeOnly) {
        ProductSearchDocument sourceDocument = productSearchRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in search index with id: " + productId));

        float[] sourceVector = sourceDocument.getMyVector();
        if (!hasNonZeroMagnitude(sourceVector)) {
            throw new IllegalStateException("Source product does not have a valid semantic vector");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    b.filter(f -> f.term(t -> t.field("isDeleted").value(false)));
                    if (activeOnly != null) {
                        b.filter(f -> f.term(t -> t.field("isActive").value(activeOnly)));
                    }
                    b.mustNot(mn -> mn.ids(i -> i.values(String.valueOf(productId))));
                    return b;
                }))
                .withKnnSearches(k -> k
                    .field("my_vector")
                    .queryVector(toFloatList(sourceVector))
                    .k(100)
                    .numCandidates(1000)
                )
                .withAggregation(AGG_BRANDS, buildBrandAggregation())
                .withAggregation(AGG_CATEGORIES, buildCategoryAggregation())
                .withAggregation(AGG_PRICE_RANGES, buildPriceRangeAggregation())
                .withPageable(pageable)
                .build();

        SearchHits<ProductSearchDocument> hits = elasticsearchOperations.search(
                nativeQuery,
                ProductSearchDocument.class,
                IndexCoordinates.of(PRODUCT_INDEX)
        );

        List<ProductSearchItemResponse> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .filter(document -> !productId.equals(document.getId()))
                .map(this::toSearchItem)
                .toList();

        return toPageResponse(content, hits, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSearchSuggestionResponse suggestKeywords(String query, int size) {
        String normalizedQuery = query == null ? "" : query.trim();
        int safeSize = Math.min(Math.max(size, 1), 20);
        if (normalizedQuery.isBlank()) {
            return ProductSearchSuggestionResponse.builder()
                    .query(normalizedQuery)
                    .suggestions(Collections.emptyList())
                    .build();
        }

        int fetchSize = Math.max(safeSize * 4, 20);
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .filter(f -> f.term(t -> t.field("isDeleted").value(false)))
                        .filter(f -> f.term(t -> t.field("isActive").value(true)))
                        .should(s -> s.matchPhrasePrefix(m -> m.field("name").query(normalizedQuery)))
                .should(s -> s.prefix(p -> p.field("brandName").value(normalizedQuery).caseInsensitive(true)))
                .should(s -> s.prefix(p -> p.field("categoryName").value(normalizedQuery).caseInsensitive(true)))
                        .minimumShouldMatch("1")
                ))
                .withPageable(PageRequest.of(0, fetchSize))
                .build();

        SearchHits<ProductSearchDocument> hits = elasticsearchOperations.search(
                nativeQuery,
                ProductSearchDocument.class,
                IndexCoordinates.of(PRODUCT_INDEX)
        );

        Set<String> uniqueSuggestions = new LinkedHashSet<>();
        for (SearchHit<ProductSearchDocument> hit : hits.getSearchHits()) {
            ProductSearchDocument document = hit.getContent();
            addSuggestionIfMatches(uniqueSuggestions, document.getName(), normalizedQuery, safeSize);
            addSuggestionIfMatches(uniqueSuggestions, document.getCategoryName(), normalizedQuery, safeSize);
            addSuggestionIfMatches(uniqueSuggestions, document.getBrandName(), normalizedQuery, safeSize);

            String categoryBrand = buildCategoryBrandSuggestion(document);
            addSuggestionIfMatches(uniqueSuggestions, categoryBrand, normalizedQuery, safeSize);

            if (uniqueSuggestions.size() >= safeSize) {
                break;
            }
        }

        return ProductSearchSuggestionResponse.builder()
                .query(normalizedQuery)
                .suggestions(new ArrayList<>(uniqueSuggestions))
                .build();
    }

        @Override
        @Transactional(readOnly = true)
        public ProductSearchTrendingResponse getTrendingSearches(int days, int size) {
        int safeDays = Math.min(Math.max(days, 1), 30);
        int safeSize = Math.min(Math.max(size, 1), 20);
        String fromDateTime = LocalDateTime.now().minusDays(safeDays).toString();

        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(q -> q.bool(b -> b
                .filter(f -> f.exists(e -> e.field("keyword")))
                .filter(f -> f.range(r -> r.date(d -> d.field("searchedAt").gte(fromDateTime))))
            ))
            .withAggregation(AGG_TRENDING_KEYWORDS,
                Aggregation.of(a -> a.terms(t -> t.field("keyword").size(safeSize))))
            .withPageable(PageRequest.of(0, 1))
            .build();

        SearchHits<SearchQueryLogDocument> hits = elasticsearchOperations.search(
            nativeQuery,
            SearchQueryLogDocument.class,
            IndexCoordinates.of(SEARCH_LOG_INDEX)
        );

        return ProductSearchTrendingResponse.builder()
            .days(safeDays)
            .items(extractTrendingItems(hits))
            .build();
        }

    @Override
    public void upsertProduct(Long productId) {
        productRepository.findByIdForSearchSync(productId)
                .map(this::toSearchDocument)
                .ifPresent(productSearchRepository::save);
    }

    @Override
    public void deleteProduct(Long productId) {
        productSearchRepository.deleteById(productId);
    }

    @Override
    public void reindexAllProducts() {
        List<ProductSearchDocument> documents = productRepository.findAllForSearchSync().stream()
                .map(this::toSearchDocument)
                .toList();
        productSearchRepository.saveAll(documents);
    }

    private Query buildQuery(
            String keyword,
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean activeOnly
    ) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasBrand = brand != null && !brand.isBlank();

        if (!hasKeyword && !hasCategory && !hasBrand && minPrice == null && maxPrice == null && activeOnly == null) {
            return Query.of(q -> q.matchAll(ma -> ma));
        }

        return Query.of(q -> q.bool(b -> {
            b.filter(f -> f.term(t -> t.field("isDeleted").value(false)));

            if (hasKeyword) {
                b.must(m -> m.multiMatch(mm -> mm
                        .query(keyword)
                        .fields("name^3", "shortDescription^2", "description", "brandName", "categoryName")
                        .fuzziness("AUTO")
                ));
            }
            if (hasCategory) {
                b.filter(f -> f.term(t -> t.field("categorySlug").value(category)));
            }
            if (hasBrand) {
                b.filter(f -> f.term(t -> t.field("brandSlug").value(brand)));
            }
            if (minPrice != null) {
                b.filter(f -> f.range(r -> r.number(n -> n.field("basePrice").gte(minPrice.doubleValue()))));
            }
            if (maxPrice != null) {
                b.filter(f -> f.range(r -> r.number(n -> n.field("basePrice").lte(maxPrice.doubleValue()))));
            }
            if (activeOnly != null) {
                b.filter(f -> f.term(t -> t.field("isActive").value(activeOnly)));
            }
            return b;
        }));
    }

    private List<SortOptions> buildSort(String sortBy, String sortDir) {
        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        String safeSortDir = (sortDir == null || sortDir.isBlank()) ? "desc" : sortDir;

        SortOrder order = "asc".equalsIgnoreCase(safeSortDir) ? SortOrder.Asc : SortOrder.Desc;

        List<SortOptions> sortOptions = new ArrayList<>();

        switch (safeSortBy.toLowerCase(Locale.ROOT)) {
            case "price" -> sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("basePrice").order(order))));
            case "name" -> sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("name").order(order))));
            default -> sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("createdAt").order(order))));
        }

        sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("id").order(SortOrder.Desc))));
        return sortOptions;
    }

    private ProductSearchDocument toSearchDocument(Product product) {
        String combinedText = String.format("%s %s %s %s %s",
                product.getName(),
                product.getBrand() != null ? product.getBrand().getName() : "",
                product.getCategory() != null ? product.getCategory().getName() : "",
                product.getShortDescription() != null ? product.getShortDescription() : "",
                product.getDescription() != null ? product.getDescription() : ""
        );

        float[] clipVector = embeddingService.embedImageFromUrl(product.getThumbnailUrl());
        if (!hasNonZeroMagnitude(clipVector)) {
            clipVector = null;
        }

        return ProductSearchDocument.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .description(product.getDescription())
                .thumbnailUrl(product.getThumbnailUrl())
                .basePrice(product.getBasePrice() == null ? null : product.getBasePrice().doubleValue())
                .brandName(product.getBrand() == null ? null : product.getBrand().getName())
                .brandSlug(product.getBrand() == null ? null : product.getBrand().getSlug())
                .categoryName(product.getCategory() == null ? null : product.getCategory().getName())
                .categorySlug(product.getCategory() == null ? null : product.getCategory().getSlug())
                .isActive(product.getIsActive())
                .isDeleted(product.getIsDeleted())
                .myVector(embeddingService.embed(combinedText))
                .clipImageVector(clipVector)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductSearchItemResponse toSearchItem(ProductSearchDocument document) {
        return ProductSearchItemResponse.builder()
                .id(document.getId())
                .name(document.getName())
                .slug(document.getSlug())
                .shortDescription(document.getShortDescription())
                .thumbnailUrl(document.getThumbnailUrl())
                .basePrice(document.getBasePrice())
                .brandName(document.getBrandName())
                .categoryName(document.getCategoryName())
                .isActive(document.getIsActive())
                .build();
    }

    private List<Float> toFloatList(float[] values) {
        List<Float> result = new ArrayList<>(values.length);
        for (float value : values) {
            result.add(value);
        }
        return result;
    }

    private void addFacetAggregations(NativeQueryBuilder queryBuilder) {
        queryBuilder
                .withAggregation(AGG_BRANDS, buildBrandAggregation())
                .withAggregation(AGG_CATEGORIES, buildCategoryAggregation())
                .withAggregation(AGG_PRICE_RANGES, buildPriceRangeAggregation());
    }

    private Aggregation buildBrandAggregation() {
        return Aggregation.of(a -> a.terms(t -> t.field("brandName").size(20)));
    }

    private Aggregation buildCategoryAggregation() {
        return Aggregation.of(a -> a.terms(t -> t.field("categoryName").size(20)));
    }

    private Aggregation buildPriceRangeAggregation() {
        return Aggregation.of(a -> a.range(r -> r
                .field("basePrice")
                .ranges(rr -> rr.key("under_500k").to(500_000.0))
                .ranges(rr -> rr.key("500k_1m").from(500_000.0).to(1_000_000.0))
                .ranges(rr -> rr.key("1m_2m").from(1_000_000.0).to(2_000_000.0))
                .ranges(rr -> rr.key("2m_5m").from(2_000_000.0).to(5_000_000.0))
                .ranges(rr -> rr.key("over_5m").from(5_000_000.0))
        ));
    }

    private ProductSearchPageResponse toPageResponse(
            List<ProductSearchItemResponse> content,
            SearchHits<ProductSearchDocument> hits,
            int page,
            int size
    ) {
        long totalElements = hits.getTotalHits();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return ProductSearchPageResponse.builder()
                .content(content)
                .facets(extractFacets(hits))
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(page >= Math.max(totalPages - 1, 0))
                .build();
    }

    private ProductSearchFacetsResponse extractFacets(SearchHits<ProductSearchDocument> hits) {
        if (!hits.hasAggregations() || !(hits.getAggregations() instanceof ElasticsearchAggregations aggregations)) {
            return ProductSearchFacetsResponse.builder()
                    .brands(Collections.emptyList())
                    .categories(Collections.emptyList())
                    .priceRanges(Collections.emptyList())
                    .build();
        }

        return ProductSearchFacetsResponse.builder()
                .brands(extractTermsBuckets(aggregations, AGG_BRANDS))
                .categories(extractTermsBuckets(aggregations, AGG_CATEGORIES))
                .priceRanges(extractRangeBuckets(aggregations, AGG_PRICE_RANGES))
                .build();
    }

    private List<ProductSearchFacetBucketResponse> extractTermsBuckets(ElasticsearchAggregations aggregations, String aggName) {
        var wrapped = aggregations.get(aggName);
        if (wrapped == null) {
            return Collections.emptyList();
        }

        Aggregate aggregate = wrapped.aggregation().getAggregate();
        if (aggregate == null || !aggregate.isSterms() || aggregate.sterms().buckets().isArray() == false) {
            return Collections.emptyList();
        }

        List<ProductSearchFacetBucketResponse> buckets = new ArrayList<>();
        for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
            buckets.add(ProductSearchFacetBucketResponse.builder()
                    .key(fieldValueToString(bucket.key()))
                    .count(bucket.docCount())
                    .build());
        }
        return buckets;
    }

    private List<ProductSearchFacetBucketResponse> extractRangeBuckets(ElasticsearchAggregations aggregations, String aggName) {
        var wrapped = aggregations.get(aggName);
        if (wrapped == null) {
            return Collections.emptyList();
        }

        Aggregate aggregate = wrapped.aggregation().getAggregate();
        if (aggregate == null || !aggregate.isRange() || aggregate.range().buckets().isArray() == false) {
            return Collections.emptyList();
        }

        List<ProductSearchFacetBucketResponse> buckets = new ArrayList<>();
        for (RangeBucket bucket : aggregate.range().buckets().array()) {
            String key = bucket.key() == null ? "unknown" : bucket.key();
            buckets.add(ProductSearchFacetBucketResponse.builder()
                    .key(key)
                    .count(bucket.docCount())
                    .build());
        }
        return buckets;
    }

    private String fieldValueToString(FieldValue value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        if (value.isString()) {
            return value.stringValue();
        }
        if (value.isLong()) {
            return String.valueOf(value.longValue());
        }
        if (value.isDouble()) {
            return String.valueOf(value.doubleValue());
        }
        if (value.isBoolean()) {
            return String.valueOf(value.booleanValue());
        }
        if (value.isAny()) {
            return value.anyValue().toString();
        }
        return value._toJsonString();
    }

    private boolean hasNonZeroMagnitude(float[] values) {
        if (values == null || values.length == 0) {
            return false;
        }
        for (float value : values) {
            if (value != 0f) {
                return true;
            }
        }
        return false;
    }

    private void addSuggestionIfMatches(Set<String> suggestions, String candidate, String query, int maxSize) {
        if (suggestions.size() >= maxSize || candidate == null || candidate.isBlank()) {
            return;
        }
        String normalizedCandidate = candidate.trim();
        String lowerCandidate = normalizedCandidate.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        if (lowerCandidate.contains(lowerQuery)) {
            suggestions.add(normalizedCandidate);
        }
    }

    private String buildCategoryBrandSuggestion(ProductSearchDocument document) {
        if (document.getCategoryName() == null || document.getCategoryName().isBlank()) {
            return null;
        }
        if (document.getBrandName() == null || document.getBrandName().isBlank()) {
            return document.getCategoryName();
        }
        return document.getCategoryName() + " " + document.getBrandName();
    }

    private void logSearchKeyword(String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (normalized == null) {
            return;
        }

        searchQueryLogRepository.save(SearchQueryLogDocument.builder()
                .keyword(normalized)
                .searchedAt(LocalDateTime.now())
                .build());
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private List<ProductSearchTrendingItemResponse> extractTrendingItems(SearchHits<SearchQueryLogDocument> hits) {
        if (!hits.hasAggregations() || !(hits.getAggregations() instanceof ElasticsearchAggregations aggregations)) {
            return Collections.emptyList();
        }

        var wrapped = aggregations.get(AGG_TRENDING_KEYWORDS);
        if (wrapped == null) {
            return Collections.emptyList();
        }

        Aggregate aggregate = wrapped.aggregation().getAggregate();
        if (aggregate == null || !aggregate.isSterms() || !aggregate.sterms().buckets().isArray()) {
            return Collections.emptyList();
        }

        List<ProductSearchTrendingItemResponse> items = new ArrayList<>();
        for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
            items.add(ProductSearchTrendingItemResponse.builder()
                    .keyword(fieldValueToString(bucket.key()))
                    .count(bucket.docCount())
                    .build());
        }
        return items;
    }
}
