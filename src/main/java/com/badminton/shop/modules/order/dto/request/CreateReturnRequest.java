package com.badminton.shop.modules.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnRequest {

    @NotBlank(message = "Return reason is required")
    private String reason;

    private String refundMethod;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankName;

    @Builder.Default
    private List<String> evidenceUrls = new ArrayList<>();

    @Valid
    @NotEmpty(message = "Return items cannot be empty")
    @Builder.Default
    private List<ReturnItemRequest> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemRequest {
        @NotNull(message = "orderItemId is required")
        private Long orderItemId;

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be >= 1")
        private Integer quantity;
    }
}
