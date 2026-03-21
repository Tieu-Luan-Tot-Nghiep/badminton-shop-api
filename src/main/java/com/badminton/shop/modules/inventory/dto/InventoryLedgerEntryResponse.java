package com.badminton.shop.modules.inventory.dto;

import com.badminton.shop.modules.inventory.entity.InventoryTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLedgerEntryResponse {
    private Long id;
    private InventoryTransactionType transactionType;
    private Integer quantityChange;
    private Integer balanceAvailable;
    private Integer balanceReserved;
    private String referenceCode;
    private String note;
    private Double unitCost;
    private String createdBy;
    private LocalDateTime createdAt;
}
