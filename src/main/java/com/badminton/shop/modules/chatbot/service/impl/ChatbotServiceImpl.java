package com.badminton.shop.modules.chatbot.service.impl;

import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.chatbot.document.ChatHistoryDocument;
import com.badminton.shop.modules.chatbot.dto.ChatbotAskResponse;
import com.badminton.shop.modules.chatbot.dto.ChatbotCloseSessionResponse;
import com.badminton.shop.modules.chatbot.dto.ChatbotProductSuggestion;
import com.badminton.shop.modules.chatbot.dto.ChatbotSessionStateResponse;
import com.badminton.shop.modules.chatbot.repository.ChatHistoryRepository;
import com.badminton.shop.modules.chatbot.service.ChatbotService;
import com.badminton.shop.modules.chatbot.service.GeminiClientService;
import com.badminton.shop.modules.search.dto.ProductSearchItemResponse;
import com.badminton.shop.modules.search.dto.ProductSearchPageResponse;
import com.badminton.shop.modules.search.service.EmbeddingService;
import com.badminton.shop.modules.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private static final Pattern MILLION_PATTERN = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*(triệu|tr)", Pattern.CASE_INSENSITIVE);
    private static final Pattern THOUSAND_PATTERN = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*(nghìn|ngàn|k)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPACT_TR_PATTERN = Pattern.compile("(\\d+)\\s*tr\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;
    private final ProductSearchService productSearchService;
    private final ChatHistoryRepository chatHistoryRepository;
    private final EmbeddingService embeddingService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final GeminiClientService geminiClientService;

    private final ConcurrentHashMap<Long, SessionContext> activeSessions = new ConcurrentHashMap<>();

    @Value("${app.chatbot.history.index:chat_history}")
    private String chatHistoryIndex;

    @Override
    public ChatbotAskResponse ask(String principalEmail, String question) {
        User user = getUserByPrincipal(principalEmail);
        SessionContext session = activeSessions.computeIfAbsent(user.getId(), key -> SessionContext.newSession());

        boolean hadContext = session.turnCount() > 0;
        session.addTurn("user", question);

        MemoryRecall memoryRecall = hadContext ? MemoryRecall.empty() : recallMemory(user.getId(), question);

        String retrievalQuery = buildRetrievalQuery(question, memoryRecall.snippet());
        List<ProductSearchItemResponse> productItems = retrieveCandidateProducts(retrievalQuery, question);
        session.mergeRecommendedProducts(extractProductNames(productItems));

        String prompt = buildAdvisorPrompt(question, memoryRecall, productItems, session);
        String answer;
        try {
            answer = geminiClientService.generateAnswer(prompt);
        } catch (IllegalStateException ex) {
            log.warn("Gemini is unavailable, using local fallback advisor response.", ex);
            answer = buildLocalFallbackAnswer(question, memoryRecall, productItems, ex);
        }

        session.addTurn("assistant", answer);

        return ChatbotAskResponse.builder()
                .answer(answer)
                .recoveredFromMemory(memoryRecall.found())
                .recoveredMemorySnippet(memoryRecall.snippet())
                .productSuggestions(productItems.stream().map(this::toSuggestion).toList())
                .sessionTurnCount(session.turnCount())
                .sessionUpdatedAt(session.getUpdatedAt())
                .build();
    }

    @Override
    public ChatbotCloseSessionResponse closeSession(String principalEmail) {
        User user = getUserByPrincipal(principalEmail);
        SessionContext session = activeSessions.get(user.getId());

        if (session == null || session.turnCount() == 0) {
            return ChatbotCloseSessionResponse.builder()
                    .persisted(false)
                    .summary("No active chatbot session to close.")
                    .recommendedProducts(List.of())
                    .persistedAt(LocalDateTime.now())
                    .build();
        }

        String summary = summarizeSession(session);
        ChatHistoryDocument doc = ChatHistoryDocument.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .content(summary)
                .recommendedProducts(new ArrayList<>(session.getRecommendedProducts()))
                .memoryVector(embeddingService.embed(summary))
                .createdAt(LocalDateTime.now())
                .build();

        chatHistoryRepository.save(doc);
        activeSessions.remove(user.getId());

        return ChatbotCloseSessionResponse.builder()
                .persisted(true)
                .summary(summary)
                .recommendedProducts(doc.getRecommendedProducts())
                .persistedAt(doc.getCreatedAt())
                .build();
    }

    @Override
    public ChatbotSessionStateResponse getSessionState(String principalEmail) {
        User user = getUserByPrincipal(principalEmail);
        SessionContext session = activeSessions.get(user.getId());

        if (session == null || session.turnCount() == 0) {
            return ChatbotSessionStateResponse.builder()
                    .active(false)
                    .turnCount(0)
                    .startedAt(null)
                    .updatedAt(null)
                    .recommendedProducts(List.of())
                    .build();
        }

        return ChatbotSessionStateResponse.builder()
                .active(true)
                .turnCount(session.turnCount())
                .startedAt(session.getStartedAt())
                .updatedAt(session.getUpdatedAt())
                .recommendedProducts(new ArrayList<>(session.getRecommendedProducts()))
                .build();
    }

    private MemoryRecall recallMemory(Long userId, String question) {
        try {
            float[] queryVector = embeddingService.embed(question);
            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.term(t -> t.field("userId").value(userId)))
                    .withKnnSearches(k -> k
                            .field("memory_vector")
                            .queryVector(toFloatList(queryVector))
                            .k(3)
                            .numCandidates(50)
                    )
                    .build();

            SearchHits<ChatHistoryDocument> hits = elasticsearchOperations.search(
                    query,
                    ChatHistoryDocument.class,
                    IndexCoordinates.of(chatHistoryIndex)
            );

            Optional<ChatHistoryDocument> topHit = hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .findFirst();

            if (topHit.isPresent()) {
                return new MemoryRecall(true, topHit.get().getContent());
            }
        } catch (Exception ex) {
            log.warn("Cannot run vector recall for user {}", userId, ex);
        }

        List<ChatHistoryDocument> latestMemories = chatHistoryRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        if (!latestMemories.isEmpty()) {
            return new MemoryRecall(true, latestMemories.get(0).getContent());
        }

        return MemoryRecall.empty();
    }

    private List<ProductSearchItemResponse> retrieveCandidateProducts(String retrievalQuery, String rawQuestion) {
        Long budget = extractBudgetVnd(rawQuestion);
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        if (budget != null) {
            minPrice = BigDecimal.valueOf(Math.max(0L, Math.round(budget * 0.7)));
            maxPrice = BigDecimal.valueOf(Math.round(budget * 1.2));
        }

        try {
            ProductSearchPageResponse page = productSearchService.searchProducts(
                    retrievalQuery,
                    null,
                    null,
                    minPrice,
                    maxPrice,
                    "createdAt",
                    "desc",
                    0,
                    3,
                    true,
                    true
            );
            return page.getContent() == null ? List.of() : page.getContent();
        } catch (Exception ex) {
            log.warn("Product retrieval failed for query: {}", retrievalQuery, ex);
            return List.of();
        }
    }

    private String buildRetrievalQuery(String question, String recalledMemory) {
        if (recalledMemory == null || recalledMemory.isBlank()) {
            return question;
        }
        return question + "\nRelevant memory: " + recalledMemory;
    }

    private String buildAdvisorPrompt(
            String question,
            MemoryRecall memoryRecall,
            List<ProductSearchItemResponse> products,
            SessionContext session
    ) {
        StringBuilder productBlock = new StringBuilder();
        if (products.isEmpty()) {
            productBlock.append("- Không có sản phẩm phù hợp từ Elasticsearch tại thời điểm này.\n");
        } else {
            int index = 1;
            for (ProductSearchItemResponse product : products) {
                productBlock.append(index++)
                        .append(". ")
                        .append(product.getName())
                        .append(" | Giá: ")
                        .append(product.getBasePrice())
                        .append(" | Brand: ")
                        .append(product.getBrandName())
                        .append(" | Mô tả: ")
                        .append(product.getShortDescription())
                        .append("\n");
            }
        }

        String memorySection = memoryRecall.found()
                ? memoryRecall.snippet()
                : "Không có lịch sử tư vấn trước đó.";

        return "Bạn là chuyên gia tư vấn cầu lông của shop. "
                + "Hãy trả lời bằng tiếng Việt, giọng thân thiện, ưu tiên thực tế cho người mới.\n"
                + "Nếu câu hỏi có rủi ro kỹ thuật (ví dụ mức căng lưới), hãy nêu rõ ngưỡng an toàn và lời khuyên sử dụng.\n"
                + "Không bịa thông số ngoài dữ liệu đã có. Nếu thiếu dữ liệu thì nói rõ là cần kiểm tra thêm.\n\n"
                + "Lịch sử đã lưu (memory):\n"
                + memorySection + "\n\n"
                + "Tóm tắt session hiện tại:\n"
                + summarizeSession(session) + "\n\n"
                + "Dữ liệu sản phẩm truy xuất từ Elasticsearch:\n"
                + productBlock + "\n"
                + "Câu hỏi khách hàng:\n"
                + question + "\n\n"
                + "Yêu cầu trả lời:"
                + "\n1) Trả lời trực tiếp vào câu hỏi hiện tại"
                + "\n2) Nếu có thể, gợi ý tối đa 2 lựa chọn phù hợp"
                + "\n3) Kết thúc bằng 1 câu hỏi follow-up ngắn để chốt nhu cầu.";
    }

    private String summarizeSession(SessionContext session) {
        String latestUserQuestion = session.getLatestUserQuestion();
        String products = session.getRecommendedProducts().isEmpty()
                ? "chưa gợi ý sản phẩm cụ thể"
                : String.join(", ", session.getRecommendedProducts());

        return "Khách đã trao đổi về nhu cầu: " + latestUserQuestion
                + ". Đã tư vấn các sản phẩm: " + products + ".";
    }

    private String buildLocalFallbackAnswer(
            String question,
            MemoryRecall memoryRecall,
            List<ProductSearchItemResponse> products,
            IllegalStateException cause
    ) {
        StringBuilder sb = new StringBuilder();
        boolean quotaError = isQuotaError(cause);
        boolean safetyStop = isSafetyStopError(cause);

        if (safetyStop) {
            sb.append("Một phần nội dung vừa rồi có thể bị bộ lọc an toàn của AI chặn, nên mình chuyển sang tư vấn theo dữ liệu sản phẩm của shop nhé.\n\n");
        } else if (quotaError) {
            sb.append("Hiện tại hệ thống AI đang bận do vượt quota tạm thời, nên mình tư vấn nhanh theo dữ liệu sản phẩm của shop nhé.\n\n");
        } else {
            sb.append("Hiện tại hệ thống AI tạm thời chưa phản hồi được, nên mình tư vấn nhanh theo dữ liệu sản phẩm của shop nhé.\n\n");
        }
        sb.append("Câu hỏi của bạn: ").append(question).append("\n");

        if (memoryRecall.found() && memoryRecall.snippet() != null && !memoryRecall.snippet().isBlank()) {
            sb.append("Ngữ cảnh trước đó: ").append(memoryRecall.snippet()).append("\n");
        }

        if (products == null || products.isEmpty()) {
            sb.append("Hiện chưa có sản phẩm đủ khớp để gợi ý ngay. Bạn cho mình thêm ngân sách, thương hiệu ưu tiên và lối đánh để mình lọc chính xác hơn.\n");
        } else {
            sb.append("Gợi ý nhanh từ Elasticsearch:\n");
            int limit = Math.min(products.size(), 2);
            for (int i = 0; i < limit; i++) {
                ProductSearchItemResponse product = products.get(i);
                sb.append(i + 1)
                        .append(") ")
                        .append(product.getName())
                        .append(" - ")
                        .append(product.getBasePrice())
                        .append(" VND")
                        .append(product.getShortDescription() != null && !product.getShortDescription().isBlank()
                                ? " (" + product.getShortDescription() + ")"
                                : "")
                        .append("\n");
            }
        }

        if (safetyStop) {
            sb.append("\nBạn có thể diễn đạt lại theo hướng cụ thể về thông số vợt, ngân sách và lối đánh để mình tư vấn chuẩn hơn nhé?");
        } else if (quotaError) {
            sb.append("\nMình sẽ tự động dùng lại AI ngay khi quota khả dụng. Bạn có muốn mình so sánh chi tiết 2 mẫu gợi ý ở trên không?");
        } else {
            sb.append("\nBạn muốn mình ưu tiên tiêu chí nào trước: nhẹ tay, công/thủ, hay độ bền khung?");
        }

        return sb.toString();
    }

    private boolean isQuotaError(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String message = throwable.getMessage();
        if (message != null) {
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("quota")
                    || normalized.contains("resource_exhausted")
                    || normalized.contains("429")) {
                return true;
            }
        }
        return isQuotaError(throwable.getCause());
    }

    private boolean isSafetyStopError(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String message = throwable.getMessage();
        if (message != null) {
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("safety") || normalized.contains("recitation") || normalized.contains("blocked")) {
                return true;
            }
        }
        return isSafetyStopError(throwable.getCause());
    }

    private ChatbotProductSuggestion toSuggestion(ProductSearchItemResponse item) {
        return ChatbotProductSuggestion.builder()
                .id(item.getId())
                .name(item.getName())
                .slug(item.getSlug())
                .basePrice(item.getBasePrice())
                .brandName(item.getBrandName())
                .shortDescription(item.getShortDescription())
                .build();
    }

    private List<String> extractProductNames(List<ProductSearchItemResponse> items) {
        return items.stream().map(ProductSearchItemResponse::getName).toList();
    }

    private Long extractBudgetVnd(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalized = normalize(text);
        Matcher compactMatcher = COMPACT_TR_PATTERN.matcher(normalized);
        if (compactMatcher.find()) {
            long millionPart = Long.parseLong(compactMatcher.group(1));
            long hundredPart = Long.parseLong(compactMatcher.group(2));
            if (hundredPart <= 9) {
                return millionPart * 1_000_000L + hundredPart * 100_000L;
            }
            return millionPart * 1_000_000L + hundredPart * 10_000L;
        }

        Matcher millionMatcher = MILLION_PATTERN.matcher(normalized);
        if (millionMatcher.find()) {
            double value = parseDecimalNumber(millionMatcher.group(1));
            if (!millionMatcher.group(1).contains(".") && !millionMatcher.group(1).contains(",")
                    && normalized.contains("ruoi")) {
                value += 0.5;
            }
            return Math.round(value * 1_000_000d);
        }

        Matcher thousandMatcher = THOUSAND_PATTERN.matcher(normalized);
        if (thousandMatcher.find()) {
            double value = parseDecimalNumber(thousandMatcher.group(1));
            return Math.round(value * 1_000d);
        }

        return null;
    }

    private double parseDecimalNumber(String raw) {
        String safe = raw.replace(',', '.');
        try {
            return Double.parseDouble(safe);
        } catch (NumberFormatException ex) {
            return 0d;
        }
    }

    private String normalize(String text) {
        String noAccent = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccent.toLowerCase(Locale.ROOT);
    }

    private List<Float> toFloatList(float[] values) {
        List<Float> result = new ArrayList<>(values.length);
        for (float value : values) {
            result.add(value);
        }
        return result;
    }

    private User getUserByPrincipal(String principalEmail) {
        return userRepository.findByEmail(principalEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + principalEmail));
    }

    private record MemoryRecall(boolean found, String snippet) {
        static MemoryRecall empty() {
            return new MemoryRecall(false, null);
        }
    }

    private static final class SessionContext {
        private final LocalDateTime startedAt;
        private LocalDateTime updatedAt;
        private final List<ChatTurn> turns;
        private final Set<String> recommendedProducts;

        private SessionContext() {
            this.startedAt = LocalDateTime.now();
            this.updatedAt = this.startedAt;
            this.turns = new ArrayList<>();
            this.recommendedProducts = new LinkedHashSet<>();
        }

        static SessionContext newSession() {
            return new SessionContext();
        }

        synchronized void addTurn(String role, String content) {
            turns.add(new ChatTurn(role, content, LocalDateTime.now()));
            updatedAt = LocalDateTime.now();
        }

        synchronized int turnCount() {
            return turns.size();
        }

        synchronized LocalDateTime getStartedAt() {
            return startedAt;
        }

        synchronized LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        synchronized String getLatestUserQuestion() {
            for (int i = turns.size() - 1; i >= 0; i--) {
                ChatTurn turn = turns.get(i);
                if ("user".equals(turn.role())) {
                    return turn.content();
                }
            }
            return "không có";
        }

        synchronized Set<String> getRecommendedProducts() {
            return new LinkedHashSet<>(recommendedProducts);
        }

        synchronized void mergeRecommendedProducts(List<String> productNames) {
            if (productNames != null) {
                recommendedProducts.addAll(productNames.stream()
                        .filter(name -> name != null && !name.isBlank())
                        .toList());
                updatedAt = LocalDateTime.now();
            }
        }
    }

    private record ChatTurn(String role, String content, LocalDateTime createdAt) {
    }
}
