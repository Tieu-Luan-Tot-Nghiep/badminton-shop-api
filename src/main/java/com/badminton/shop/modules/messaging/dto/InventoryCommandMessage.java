package com.badminton.shop.modules.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCommandMessage implements Serializable {

    private InventoryCommandType commandType;
    private String referenceCode;
    private String note;

    @Builder.Default
    private List<InventoryCommandLine> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryCommandLine implements Serializable {
        private Long variantId;
        private Integer quantity;
    }
}
