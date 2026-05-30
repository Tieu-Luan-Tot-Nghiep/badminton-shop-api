package com.badminton.shop.modules.product.service.impl;

import com.badminton.shop.modules.product.dto.ProductRecommendationResponse;
import com.badminton.shop.modules.product.service.ProductRecommendationService;
import com.badminton.shop.modules.search.dto.ProductSearchItemResponse;
import com.badminton.shop.modules.search.dto.ProductSearchPageResponse;
import com.badminton.shop.modules.search.service.ProductSearchService;
import com.badminton.shop.modules.chatbot.service.GeminiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductRecommendationServiceImpl implements ProductRecommendationService {

    private final ProductSearchService productSearchService;
    private final GeminiClientService geminiClientService;

    @Override
    public ProductRecommendationResponse getRecommendations(Long productId, int size, boolean withAi) {
        int safeSize = Math.min(Math.max(size, 1), 20);

        // Lấy sản phẩm nguồn từ Elasticsearch để biết tên
        String sourceProductName = resolveSourceProductName(productId);

        // Dùng KNN vector similarity để tìm sản phẩm tương tự
        List<ProductSearchItemResponse> recommendations;
        try {
            ProductSearchPageResponse page = productSearchService.suggestSimilarProducts(
                    productId, 0, safeSize, true
            );
            recommendations = page.getContent() == null ? List.of() : page.getContent();
        } catch (Exception ex) {
            log.warn("[recommendation] KNN search failed for productId={}, falling back to empty list", productId, ex);
            recommendations = List.of();
        }

        // AI insight (optional, chỉ gọi khi withAi=true)
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

    private String resolveSourceProductName(Long productId) {
        try {
            // Thử lấy từ Elasticsearch index
            ProductSearchPageResponse page = productSearchService.searchProducts(
                    null, null, null, null, null,
                    "createdAt", "desc", 0, 1, null, false
            );
            // Tìm trong index bằng cách dùng suggestSimilarProducts để lấy document
            // Nếu không tìm được thì dùng ID làm fallback
            return "Sản phẩm #" + productId;
        } catch (Exception ex) {
            return "Sản phẩm #" + productId;
        }
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
            log.warn("[recommendation] AI insight generation failed for source='{}': {}", sourceProductName, ex.getMessage());
            return null;
        }
    }
}