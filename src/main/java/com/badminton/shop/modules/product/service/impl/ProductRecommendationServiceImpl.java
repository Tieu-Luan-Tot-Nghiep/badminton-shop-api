package com.badminton.shop.modules.product.service.impl;

import com.badminton.shop.modules.chatbot.service.GeminiClientService;
import com.badminton.shop.modules.product.dto.ProductRecommendationResponse;
import com.badminton.shop.modules.product.entity.Product;
import com.badminton.shop.modules.product.repository.ProductRepository;
import com.badminton.shop.modules.product.service.ProductRecommendationService;
import com.badminton.shop.modules.search.dto.ProductSearchItemResponse;
import com.badminton.shop.modules.search.dto.ProductSearchPageResponse;
import com.badminton.shop.modules.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductRecommendationServiceImpl implements ProductRecommendationService {

    private final ProductSearchService productSearchService;
    private final GeminiClientService geminiClientService;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductRecommendationResponse getRecommendations(Long productId, int size, boolean withAi) {
        int safeSize = Math.min(Math.max(size, 1), 20);

        // Lấy thông tin sản phẩm nguồn từ DB
        Product sourceProduct = productRepository.findById(productId).orElse(null);
        String sourceProductName = sourceProduct != null ? sourceProduct.getName() : "Sản phẩm #" + productId;

        // Tầng 1: KNN vector similarity từ Elasticsearch (nếu đã reindex)
        // Gọi trong transaction riêng để tránh rollback-only contamination
        List<ProductSearchItemResponse> recommendations = fetchFromElasticsearch(productId, safeSize);

        // Tầng 2: DB fallback — cùng category (khi Elasticsearch rỗng hoặc lỗi)
        if (recommendations.isEmpty() && sourceProduct != null) {
            recommendations = fetchFromDb(sourceProduct, safeSize);
        }

        // AI insight (optional)
        String aiInsight = null;
        if (withAi && !recommendations.isEmpty()) {
            aiInsight = generateAiInsight(sourceProductName, recommendations);
        }

        return ProductRecommendationResponse.builder()
                .sourceProductId(productId)
                .sourceProductName(sourceProductName)
                .recommendations(recommendations)
                .aiInsight(aiInsight)
                .aiInsightEnabled(withAi)
                .total(recommendations.size())
                .build();
    }

    /**
     * Gọi Elasticsearch trong transaction độc lập (REQUIRES_NEW) để tránh
     * "rollback-only" contamination khi ResourceNotFoundException được throw
     * bên trong @Transactional(readOnly=true) của suggestSimilarProducts.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, readOnly = true)
    public List<ProductSearchItemResponse> fetchFromElasticsearch(Long productId, int size) {
        try {
            ProductSearchPageResponse page = productSearchService.suggestSimilarProducts(
                    productId, 0, size, true
            );
            List<ProductSearchItemResponse> results = page.getContent() == null ? List.of() : page.getContent();
            log.debug("[recommendation] KNN found {} results for productId={}", results.size(), productId);
            return results;
        } catch (com.badminton.shop.exception.ResourceNotFoundException ex) {
            log.debug("[recommendation] Product {} not in Elasticsearch index, will use DB fallback", productId);
            return List.of();
        } catch (IllegalStateException ex) {
            log.debug("[recommendation] Product {} has no vector, will use DB fallback: {}", productId, ex.getMessage());
            return List.of();
        } catch (Exception ex) {
            log.debug("[recommendation] KNN failed for productId={}, will use DB fallback: {}", productId, ex.getMessage());
            return List.of();
        }
    }

    /**
     * Fallback: lấy sản phẩm cùng category từ DB.
     * Nếu cùng category không đủ, bổ sung bằng sản phẩm mới nhất.
     */
    private List<ProductSearchItemResponse> fetchFromDb(Product sourceProduct, int size) {
        Long categoryId = sourceProduct.getCategory() != null ? sourceProduct.getCategory().getId() : null;

        List<Product> dbProducts;
        if (categoryId != null) {
            dbProducts = productRepository.findSameCategoryProducts(
                    categoryId, sourceProduct.getId(), PageRequest.of(0, size)
            );
            log.debug("[recommendation] DB same-category found {} results for productId={}", dbProducts.size(), sourceProduct.getId());

            // Bổ sung nếu chưa đủ size
            if (dbProducts.size() < size) {
                int remaining = size - dbProducts.size();
                List<Long> excludeIds = dbProducts.stream().map(Product::getId).toList();
                List<Product> extra = productRepository.findRecentProductsExcluding(
                        sourceProduct.getId(), PageRequest.of(0, remaining + excludeIds.size())
                ).stream()
                        .filter(p -> !excludeIds.contains(p.getId()))
                        .limit(remaining)
                        .toList();
                dbProducts = new java.util.ArrayList<>(dbProducts);
                dbProducts.addAll(extra);
            }
        } else {
            dbProducts = productRepository.findRecentProductsExcluding(
                    sourceProduct.getId(), PageRequest.of(0, size)
            );
        }

        return dbProducts.stream().map(this::toSearchItem).toList();
    }

    private ProductSearchItemResponse toSearchItem(Product product) {
        return ProductSearchItemResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .thumbnailUrl(product.getThumbnailUrl())
                .basePrice(product.getBasePrice() == null ? null : product.getBasePrice().doubleValue())
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .isActive(product.getIsActive())
                .build();
    }

    private String generateAiInsight(String sourceProductName, List<ProductSearchItemResponse> recommendations) {
        try {
            StringBuilder productList = new StringBuilder();
            int limit = Math.min(recommendations.size(), 5);
            for (int i = 0; i < limit; i++) {
                ProductSearchItemResponse item = recommendations.get(i);
                productList.append(i + 1).append(". ")
                        .append(item.getName())
                        .append(" | Giá: ").append(item.getBasePrice())
                        .append(" | Brand: ").append(item.getBrandName())
                        .append("\n");
            }

            String prompt = "Bạn là chuyên gia tư vấn cầu lông. "
                    + "Khách hàng đang xem sản phẩm: \"" + sourceProductName + "\".\n"
                    + "Hệ thống đã gợi ý các sản phẩm tương tự sau:\n"
                    + productList
                    + "\nHãy viết 2-3 câu ngắn gọn bằng tiếng Việt, giải thích tại sao những sản phẩm này "
                    + "phù hợp để xem thêm khi đang quan tâm đến \"" + sourceProductName + "\". "
                    + "Tập trung vào điểm tương đồng về công dụng, thương hiệu hoặc phân khúc giá. "
                    + "Không liệt kê lại tên sản phẩm, chỉ giải thích lý do gợi ý.";

            return geminiClientService.generateAnswer(prompt);
        } catch (Exception ex) {
            log.warn("[recommendation] AI insight generation failed: {}", ex.getMessage());
            return null;
        }
    }
}
