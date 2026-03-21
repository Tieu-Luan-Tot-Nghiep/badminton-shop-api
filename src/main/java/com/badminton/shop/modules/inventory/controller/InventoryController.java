package com.badminton.shop.modules.inventory.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.inventory.dto.*;
import com.badminton.shop.modules.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ===== System APIs (for internal service-to-service communication) =====

    @PostMapping("/system/check-availability")
    public ResponseEntity<ApiResponse<AvailabilityCheckResponse>> checkAvailability(
            @Valid @RequestBody AvailabilityCheckRequest request
    ) {
        AvailabilityCheckResponse response = inventoryService.checkAvailability(request);
        return ResponseEntity.ok(ApiResponse.success("Inventory availability checked.", response));
    }

    @PostMapping("/system/reserve")
    public ResponseEntity<ApiResponse<SystemInventoryResponse>> reserveInventory(
            @Valid @RequestBody SystemInventoryRequest request
    ) {
        SystemInventoryResponse response = inventoryService.reserveInventory(request);
        return ResponseEntity.ok(ApiResponse.success("Inventory reserved successfully.", response));
    }

    @PostMapping("/system/commit")
    public ResponseEntity<ApiResponse<SystemInventoryResponse>> commitInventory(
            @Valid @RequestBody SystemInventoryRequest request
    ) {
        SystemInventoryResponse response = inventoryService.commitInventory(request);
        return ResponseEntity.ok(ApiResponse.success("Inventory committed successfully.", response));
    }

    @PostMapping("/system/rollback")
    public ResponseEntity<ApiResponse<SystemInventoryResponse>> rollbackInventory(
            @Valid @RequestBody SystemInventoryRequest request
    ) {
        SystemInventoryResponse response = inventoryService.rollbackInventory(request);
        return ResponseEntity.ok(ApiResponse.success("Inventory rolled back successfully.", response));
    }

    // ===== Admin APIs =====

    @PostMapping("/admin/stock-in")
    public ResponseEntity<ApiResponse<InventorySnapshotResponse>> stockIn(
            Principal principal,
            @Valid @RequestBody StockInRequest request
    ) {
        String operator = principal != null ? principal.getName() : "system";
        InventorySnapshotResponse response = inventoryService.stockIn(operator, request);
        return ResponseEntity.ok(ApiResponse.success("Stock-in created successfully.", response));
    }

    @PostMapping("/admin/stock-out")
    public ResponseEntity<ApiResponse<InventorySnapshotResponse>> stockOut(
            Principal principal,
            @Valid @RequestBody StockOutRequest request
    ) {
        String operator = principal != null ? principal.getName() : "system";
        InventorySnapshotResponse response = inventoryService.stockOut(operator, request);
        return ResponseEntity.ok(ApiResponse.success("Stock-out created successfully.", response));
    }

    @PostMapping("/admin/stocktake-adjustment")
    public ResponseEntity<ApiResponse<InventorySnapshotResponse>> stocktakeAdjustment(
            Principal principal,
            @Valid @RequestBody StocktakeAdjustmentRequest request
    ) {
        String operator = principal != null ? principal.getName() : "system";
        InventorySnapshotResponse response = inventoryService.stocktakeAdjust(operator, request);
        return ResponseEntity.ok(ApiResponse.success("Stocktake adjustment applied successfully.", response));
    }

    @GetMapping("/admin/ledger/{variantId}")
    public ResponseEntity<ApiResponse<InventoryLedgerResponse>> getLedger(
            @PathVariable Long variantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        InventoryLedgerResponse response = inventoryService.getLedger(variantId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Inventory ledger fetched successfully.", response));
    }

    @GetMapping("/admin/low-stock")
    public ResponseEntity<ApiResponse<List<InventorySnapshotResponse>>> getLowStockAlerts(
            @RequestParam(required = false) Integer threshold
    ) {
        List<InventorySnapshotResponse> response = inventoryService.getLowStockAlerts(threshold);
        return ResponseEntity.ok(ApiResponse.success("Low-stock alerts fetched successfully.", response));
    }
}
