package com.badminton.shop.modules.order.dto.response;

import com.badminton.shop.modules.order.entity.ReturnItemAction;
import com.badminton.shop.modules.order.entity.ReturnRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestResponse {

    private Long id;
    private String orderCode;
    private ReturnRequestStatus status;
    private String reason;
    private String refundMethod;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankName;
    private List<String> evidenceUrls;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<ReturnItemResponse> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemResponse {
        private Long orderItemId;
        private Long variantId;
        private String productName;
        private String sku;
        private Integer requestedQuantity;
        private Integer receivedQuantity;
        private ReturnItemAction action;
    }
}
