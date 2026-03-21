package com.badminton.shop.modules.inventory.service;

import com.badminton.shop.modules.inventory.dto.*;

import java.util.List;

public interface InventoryService {

    AvailabilityCheckResponse checkAvailability(AvailabilityCheckRequest request);

    SystemInventoryResponse reserveInventory(SystemInventoryRequest request);

    SystemInventoryResponse commitInventory(SystemInventoryRequest request);

    SystemInventoryResponse rollbackInventory(SystemInventoryRequest request);

    InventorySnapshotResponse stockIn(String operator, StockInRequest request);

    InventorySnapshotResponse stockOut(String operator, StockOutRequest request);

    InventorySnapshotResponse stocktakeAdjust(String operator, StocktakeAdjustmentRequest request);

    InventoryLedgerResponse getLedger(Long variantId, int page, int size);

    List<InventorySnapshotResponse> getLowStockAlerts(Integer threshold);
}
