package com.badminton.shop.modules.search.consumer;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.search.dto.SearchReindexBatchMessage;
import com.badminton.shop.modules.search.dto.SearchReindexMessage;
import com.badminton.shop.modules.search.service.SearchReindexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchReindexConsumer {

    private final SearchReindexService searchReindexService;

    @RabbitListener(queues = RabbitMQConfig.SEARCH_REINDEX_QUEUE)
    public void handleReindexRequest(SearchReindexMessage message) {
        log.info("[reindex-consumer] Received reindex request: {}", message.getRequestId());
        try {
            searchReindexService.processReindex(message);
        } catch (Exception e) {
            log.error("[reindex-consumer] Error processing reindex request: {}", message.getRequestId(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.SEARCH_REINDEX_BATCH_QUEUE, concurrency = "3-5")
    public void handleBatchReindex(SearchReindexBatchMessage batchMessage) {
        log.debug("[reindex-consumer] Received batch {}/{} for request: {}", 
                batchMessage.getBatchNumber(), batchMessage.getTotalBatches(), batchMessage.getRequestId());
        try {
            searchReindexService.processBatchReindex(batchMessage);
        } catch (Exception e) {
            log.error("[reindex-consumer] Error processing batch {}/{} for request: {}", 
                    batchMessage.getBatchNumber(), batchMessage.getTotalBatches(), batchMessage.getRequestId(), e);
        }
    }
}