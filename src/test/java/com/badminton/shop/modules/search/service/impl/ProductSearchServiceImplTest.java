package com.badminton.shop.modules.search.service.impl;

import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.product.repository.ProductRepository;
import com.badminton.shop.modules.search.document.ProductSearchDocument;
import com.badminton.shop.modules.search.document.SearchQueryLogDocument;
import com.badminton.shop.modules.search.dto.ProductSearchSuggestionResponse;
import com.badminton.shop.modules.search.dto.ProductSearchTrendingResponse;
import com.badminton.shop.modules.search.repository.ProductSearchRepository;
import com.badminton.shop.modules.search.repository.SearchQueryLogRepository;
import com.badminton.shop.modules.search.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceImplTest {

    @Mock
    private ProductSearchRepository productSearchRepository;
    @Mock
    private SearchQueryLogRepository searchQueryLogRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ElasticsearchOperations elasticsearchOperations;
    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private ProductSearchServiceImpl productSearchService;

    @Test
    void suggestKeywords_BlankQuery_ShouldReturnEmptyAndSkipSearch() {
        ProductSearchSuggestionResponse response = productSearchService.suggestKeywords("   ", 8);

        assertNotNull(response);
        assertEquals("", response.getQuery());
        assertTrue(response.getSuggestions().isEmpty());
        verifyNoInteractions(elasticsearchOperations);
        verify(searchQueryLogRepository, never()).save(any(SearchQueryLogDocument.class));
    }

    @Test
    void searchProductsByImage_NullImage_ShouldThrowBadRequest() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productSearchService.searchProductsByImage(null, 0, 12, true)
        );

        assertTrue(ex.getMessage().contains("Image is required"));
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void searchProductsByImage_WhenProviderUnavailable_ShouldThrowServiceUnavailable() {
        MockMultipartFile file = new MockMultipartFile("image", "img.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(embeddingService.isImageEmbeddingAvailable()).thenReturn(false);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> productSearchService.searchProductsByImage(file, 0, 12, true)
        );

        assertTrue(ex.getMessage().contains("not configured"));
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void suggestSimilarProducts_WhenProductMissing_ShouldThrowNotFound() {
        when(productSearchRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productSearchService.suggestSimilarProducts(999L, 0, 12, true));
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void suggestSimilarProducts_WhenSourceVectorInvalid_ShouldThrowIllegalState() {
        ProductSearchDocument source = ProductSearchDocument.builder()
                .id(1L)
                .myVector(new float[]{0f, 0f, 0f})
                .build();
        when(productSearchRepository.findById(1L)).thenReturn(Optional.of(source));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> productSearchService.suggestSimilarProducts(1L, 0, 12, true)
        );

        assertTrue(ex.getMessage().contains("valid semantic vector"));
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void getTrendingSearches_WhenNoAggregations_ShouldReturnEmptyItemsAndClampedDays() {
        @SuppressWarnings("unchecked")
        SearchHits<SearchQueryLogDocument> hits = org.mockito.Mockito.mock(SearchHits.class);
        when(hits.hasAggregations()).thenReturn(false);
        when(elasticsearchOperations.search(any(Query.class), eq(SearchQueryLogDocument.class), any(IndexCoordinates.class))).thenReturn(hits);

        ProductSearchTrendingResponse response = productSearchService.getTrendingSearches(99, 50);

        assertNotNull(response);
        assertEquals(30, response.getDays());
        assertTrue(response.getItems().isEmpty());
        verify(elasticsearchOperations).search(any(Query.class), eq(SearchQueryLogDocument.class), any(IndexCoordinates.class));
    }
}
