package com.badminton.shop.modules.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchReindexMessage {
    private String requestId;
    private String requestedBy;
    private LocalDateTime requestedAt;
    private int totalProducts;
    private int batchSize;
    private String status; // STARTED, IN_PROGRESS, COMPLETED, FAILED
}