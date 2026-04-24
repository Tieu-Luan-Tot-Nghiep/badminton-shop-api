package com.badminton.shop.modules.search.service;

import com.badminton.shop.modules.search.dto.SearchReindexMessage;

public interface SearchReindexService {
    
    /**
     * Start reindex process via RabbitMQ
     * @param requestedBy User who requested the reindex
     * @return Request ID for tracking
     */
    String startReindex(String requestedBy);
    
    /**
     * Get reindex status
     * @param requestId Request ID
     * @return Status information
     */
    SearchReindexMessage getReindexStatus(String requestId);
    
    /**
     * Process reindex request (called by RabbitMQ consumer)
     * @param message Reindex message
     */
    void processReindex(SearchReindexMessage message);
    
    /**
     * Process batch reindex (called by RabbitMQ consumer)
     * @param batchMessage Batch message
     */
    void processBatchReindex(com.badminton.shop.modules.search.dto.SearchReindexBatchMessage batchMessage);
}