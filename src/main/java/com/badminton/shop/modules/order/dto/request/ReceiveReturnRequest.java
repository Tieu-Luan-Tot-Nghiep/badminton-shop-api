package com.badminton.shop.modules.order.dto.request;

import com.badminton.shop.modules.order.entity.ReturnItemAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
public class ReceiveReturnRequest {

    private String note;

    @Valid
    @NotEmpty(message = "items cannot be empty")
    @Builder.Default
    private List<ReceiveItem> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiveItem {
        @NotNull(message = "orderItemId is required")
        private Long orderItemId;

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be >= 1")
        private Integer quantity;

        @NotNull(message = "action is required")
        private ReturnItemAction action;
    }
}
