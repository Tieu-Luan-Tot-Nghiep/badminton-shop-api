package com.badminton.shop.modules.search.service.impl;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.product.repository.ProductRepository;
import com.badminton.shop.modules.search.dto.SearchReindexBatchMessage;
import com.badminton.shop.modules.search.dto.SearchReindexMessage;
import com.badminton.shop.modules.search.repository.ProductSearchRepository;
import com.badminton.shop.modules.search.service.ProductSearchService;
import com.badminton.shop.modules.search.service.SearchReindexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchReindexServiceImpl implements SearchReindexService {

    private final RabbitTemplate rabbitTemplate;
    private final ProductRepository productRepository;
    private final ProductSearchService productSearchService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String REINDEX_STATUS_KEY_PREFIX = "search:reindex:status:";
    private static final int BATCH_SIZE = 50; // Smaller batch size for better performance
    private static final int STATUS_TTL_HOURS = 24; // Keep status for 24 hours

    @Override
    public String startReindex(String requestedBy) {
        String requestId = UUID.randomUUID().toString();
        
        log.info("[reindex-rabbitmq] Starting reindex request {} by user {}", requestId, requestedBy);
        
        // Get total product count
        int totalProducts = (int) productRepository.count();
        
        SearchReindexMessage message = SearchReindexMessage.builder()
                .requestId(requestId)
                .requestedBy(requestedBy)
                .requestedAt(LocalDateTime.now())
                .totalProducts(totalProducts)
                .batchSize(BATCH_SIZE)
                .status("STARTED")
                .build();
        
        // Store initial status in Redis
        storeReindexStatus(message);
        
        // Send message to RabbitMQ
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SEARCH_EXCHANGE,
                RabbitMQConfig.SEARCH_REINDEX_ROUTING_KEY,
                message
        );
        
        log.info("[reindex-rabbitmq] Reindex request {} queued successfully", requestId);
        return requestId;
    }

    @Override
    public SearchReindexMessage getReindexStatus(String requestId) {
        String key = REINDEX_STATUS_KEY_PREFIX + requestId;
        SearchReindexMessage status = (SearchReindexMessage) redisTemplate.opsForValue().get(key);
        
        if (status == null) {
            return SearchReindexMessage.builder()
                    .requestId(requestId)
                    .status("NOT_FOUND")
                    .build();
        }
        
        return status;
    }

    @Override
    public void processReindex(SearchReindexMessage message) {
        try {
            log.info("[reindex-rabbitmq] Processing reindex request {}", message.getRequestId());
            
            // Update status to IN_PROGRESS
            message.setStatus("IN_PROGRESS");
            storeReindexStatus(message);
            
            // Get all product IDs
            List<Long> allProductIds = productRepository.findAllForSearchSync()
                    .stream()
                    .map(product -> product.getId())
                    .toList();
            
            log.info("[reindex-rabbitmq] Found {} products to reindex", allProductIds.size());
            
            // Split into batches and send to batch queue
            int totalBatches = (int) Math.ceil((double) allProductIds.size() / BATCH_SIZE);
            
            IntStream.range(0, totalBatches)
                    .forEach(batchIndex -> {
                        int startIndex = batchIndex * BATCH_SIZE;
                        int endIndex = Math.min(startIndex + BATCH_SIZE, allProductIds.size());
                        List<Long> batchProductIds = allProductIds.subList(startIndex, endIndex);
                        
                        SearchReindexBatchMessage batchMessage = SearchReindexBatchMessage.builder()
                                .requestId(message.getRequestId())
                                .batchNumber(batchIndex + 1)
                                .totalBatches(totalBatches)
                                .productIds(batchProductIds)
                                .retryCount(0)
                                .build();
                        
                        rabbitTemplate.convertAndSend(
                                RabbitMQConfig.SEARCH_EXCHANGE,
                                RabbitMQConfig.SEARCH_REINDEX_BATCH_ROUTING_KEY,
                                batchMessage
                        );
                        
                        log.debug("[reindex-rabbitmq] Sent batch {}/{} for request {}", 
                                batchIndex + 1, totalBatches, message.getRequestId());
                    });
            
            log.info("[reindex-rabbitmq] Sent {} batches for request {}", totalBatches, message.getRequestId());
            
        } catch (Exception e) {
            log.error("[reindex-rabbitmq] Failed to process reindex request {}", message.getRequestId(), e);
            message.setStatus("FAILED");
            storeReindexStatus(message);
        }
    }

    @Override
    public void processBatchReindex(SearchReindexBatchMessage batchMessage) {
        try {
            log.info("[reindex-rabbitmq] Processing batch {}/{} for request {} ({} products)", 
                    batchMessage.getBatchNumber(), batchMessage.getTotalBatches(), 
                    batchMessage.getRequestId(), batchMessage.getProductIds().size());
            
            // Process each product in the batch
            for (Long productId : batchMessage.getProductIds()) {
                try {
                    productSearchService.upsertProduct(productId);
                } catch (Exception e) {
                    log.warn("[reindex-rabbitmq] Failed to index product {} in batch {}/{} for request {}", 
                            productId, batchMessage.getBatchNumber(), batchMessage.getTotalBatches(), 
                            batchMessage.getRequestId(), e);
                    // Continue with other products in batch
                }
            }
            
            log.info("[reindex-rabbitmq] Completed batch {}/{} for request {}", 
                    batchMessage.getBatchNumber(), batchMessage.getTotalBatches(), batchMessage.getRequestId());
            
            // Check if this was the last batch
            checkAndUpdateCompletionStatus(batchMessage);
            
        } catch (Exception e) {
            log.error("[reindex-rabbitmq] Failed to process batch {}/{} for request {}", 
                    batchMessage.getBatchNumber(), batchMessage.getTotalBatches(), batchMessage.getRequestId(), e);
            
            // Retry logic
            if (batchMessage.getRetryCount() < 3) {
                batchMessage.setRetryCount(batchMessage.getRetryCount() + 1);
                log.info("[reindex-rabbitmq] Retrying batch {}/{} for request {} (attempt {})", 
                        batchMessage.getBatchNumber(), batchMessage.getTotalBatches(), 
                        batchMessage.getRequestId(), batchMessage.getRetryCount());
                
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.SEARCH_EXCHANGE,
                        RabbitMQConfig.SEARCH_REINDEX_BATCH_ROUTING_KEY,
                        batchMessage
                );
            } else {
                log.error("[reindex-rabbitmq] Max retries exceeded for batch {}/{} for request {}", 
                        batchMessage.getBatchNumber(), batchMessage.getTotalBatches(), batchMessage.getRequestId());
            }
        }
    }
    
    private void storeReindexStatus(SearchReindexMessage message) {
        String key = REINDEX_STATUS_KEY_PREFIX + message.getRequestId();
        redisTemplate.opsForValue().set(key, message, STATUS_TTL_HOURS, TimeUnit.HOURS);
    }
    
    private void checkAndUpdateCompletionStatus(SearchReindexBatchMessage batchMessage) {
        // Use Redis to track completed batches
        String completedBatchesKey = REINDEX_STATUS_KEY_PREFIX + batchMessage.getRequestId() + ":completed";
        Long completedBatches = redisTemplate.opsForValue().increment(completedBatchesKey);
        
        if (completedBatches == null) {
            completedBatches = 1L;
            redisTemplate.opsForValue().set(completedBatchesKey, completedBatches, STATUS_TTL_HOURS, TimeUnit.HOURS);
        }
        
        log.info("[reindex-rabbitmq] Completed {}/{} batches for request {}", 
                completedBatches, batchMessage.getTotalBatches(), batchMessage.getRequestId());
        
        // If all batches completed, update final status
        if (completedBatches >= batchMessage.getTotalBatches()) {
            SearchReindexMessage status = getReindexStatus(batchMessage.getRequestId());
            if (status != null && !"NOT_FOUND".equals(status.getStatus())) {
                status.setStatus("COMPLETED");
                storeReindexStatus(status);
                log.info("[reindex-rabbitmq] Reindex request {} completed successfully", batchMessage.getRequestId());
            }
        }
    }
}