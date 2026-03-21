package com.badminton.shop.modules.inventory.service.impl;

import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.inventory.dto.*;
import com.badminton.shop.modules.inventory.entity.Inventory;
import com.badminton.shop.modules.inventory.entity.InventoryTransaction;
import com.badminton.shop.modules.inventory.entity.InventoryTransactionType;
import com.badminton.shop.modules.inventory.repository.InventoryRepository;
import com.badminton.shop.modules.inventory.repository.InventoryTransactionRepository;
import com.badminton.shop.modules.inventory.service.InventoryService;
import com.badminton.shop.modules.product.entity.Product;
import com.badminton.shop.modules.product.entity.ProductVariant;
import com.badminton.shop.modules.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public AvailabilityCheckResponse checkAvailability(AvailabilityCheckRequest request) {
        List<AvailabilityLineResponse> lines = new ArrayList<>();
        boolean allAvailable = true;

        for (InventoryLineRequest item : request.getItems()) {
            Inventory inventory = ensureInventory(item.getVariantId(), false);
            int available = inventory.getAvailableQuantity();
            boolean ok = available >= item.getQuantity();
            if (!ok) {
                allAvailable = false;
            }

            lines.add(AvailabilityLineResponse.builder()
                    .variantId(item.getVariantId())
                    .requestedQuantity(item.getQuantity())
                    .availableQuantity(available)
                    .available(ok)
                    .build());
        }

        return AvailabilityCheckResponse.builder()
                .allAvailable(allAvailable)
                .items(lines)
                .build();
    }

    @Override
    public SystemInventoryResponse reserveInventory(SystemInventoryRequest request) {
        precheckAvailable(request);

        List<SystemInventoryLineResponse> lines = new ArrayList<>();
        for (InventoryLineRequest item : request.getItems()) {
            Inventory inventory = ensureInventory(item.getVariantId(), true);
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());
            syncVariantStockWithAvailable(inventory);
            inventoryRepository.save(inventory);

            saveTransaction(inventory, InventoryTransactionType.RESERVE, -item.getQuantity(),
                    request.getReferenceCode(), request.getNote(), null, "system");

            lines.add(toSystemLine(item, inventory));
        }

        return SystemInventoryResponse.builder()
                .referenceCode(request.getReferenceCode())
                .items(lines)
                .build();
    }

    @Override
    public SystemInventoryResponse commitInventory(SystemInventoryRequest request) {
        List<SystemInventoryLineResponse> lines = new ArrayList<>();

        for (InventoryLineRequest item : request.getItems()) {
            Inventory inventory = ensureInventory(item.getVariantId(), true);
            if (inventory.getReservedQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Not enough reserved quantity for variantId=" + item.getVariantId());
            }

            inventory.setReservedQuantity(inventory.getReservedQuantity() - item.getQuantity());
            syncVariantStockWithAvailable(inventory);
            inventoryRepository.save(inventory);

            saveTransaction(inventory, InventoryTransactionType.COMMIT, -item.getQuantity(),
                    request.getReferenceCode(), request.getNote(), null, "system");

            lines.add(toSystemLine(item, inventory));
        }

        return SystemInventoryResponse.builder()
                .referenceCode(request.getReferenceCode())
                .items(lines)
                .build();
    }

    @Override
    public SystemInventoryResponse rollbackInventory(SystemInventoryRequest request) {
        List<SystemInventoryLineResponse> lines = new ArrayList<>();

        for (InventoryLineRequest item : request.getItems()) {
            Inventory inventory = inventoryRepository.findWithLockByVariantId(item.getVariantId()).orElse(null);
            if (inventory == null) {
                lines.add(SystemInventoryLineResponse.builder()
                        .variantId(item.getVariantId())
                        .quantity(0)
                        .availableQuantity(0)
                        .reservedQuantity(0)
                        .build());
                continue;
            }

            int reversibleQuantity = Math.min(item.getQuantity(), Math.max(inventory.getReservedQuantity(), 0));
            if (reversibleQuantity <= 0) {
                lines.add(SystemInventoryLineResponse.builder()
                        .variantId(item.getVariantId())
                        .quantity(0)
                        .availableQuantity(inventory.getAvailableQuantity())
                        .reservedQuantity(inventory.getReservedQuantity())
                        .build());
                continue;
            }

            inventory.setReservedQuantity(inventory.getReservedQuantity() - reversibleQuantity);
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reversibleQuantity);
            syncVariantStockWithAvailable(inventory);
            inventoryRepository.save(inventory);

            saveTransaction(inventory, InventoryTransactionType.ROLLBACK, reversibleQuantity,
                    request.getReferenceCode(), request.getNote(), null, "system");

            lines.add(toSystemLine(item, inventory));
        }

        return SystemInventoryResponse.builder()
                .referenceCode(request.getReferenceCode())
                .items(lines)
                .build();
    }

    @Override
    public InventorySnapshotResponse stockIn(String operator, StockInRequest request) {
        Inventory inventory = ensureInventory(request.getVariantId(), true);
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + request.getQuantity());
        syncVariantStockWithAvailable(inventory);
        inventoryRepository.save(inventory);

        saveTransaction(inventory, InventoryTransactionType.STOCK_IN, request.getQuantity(),
                null, request.getNote(), request.getUnitCost(), operator);

        return toSnapshot(inventory);
    }

    @Override
    public InventorySnapshotResponse stockOut(String operator, StockOutRequest request) {
        Inventory inventory = ensureInventory(request.getVariantId(), true);
        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException("Not enough available stock for stock-out.");
        }

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - request.getQuantity());
        syncVariantStockWithAvailable(inventory);
        inventoryRepository.save(inventory);

        saveTransaction(inventory, InventoryTransactionType.STOCK_OUT, -request.getQuantity(),
                null, request.getNote(), null, operator);

        return toSnapshot(inventory);
    }

    @Override
    public InventorySnapshotResponse stocktakeAdjust(String operator, StocktakeAdjustmentRequest request) {
        Inventory inventory = ensureInventory(request.getVariantId(), true);
        int currentAvailable = inventory.getAvailableQuantity();
        int targetAvailable = request.getActualAvailableQuantity();
        int delta = targetAvailable - currentAvailable;

        inventory.setAvailableQuantity(targetAvailable);
        syncVariantStockWithAvailable(inventory);
        inventoryRepository.save(inventory);

        InventoryTransactionType type = delta >= 0
                ? InventoryTransactionType.ADJUSTMENT_INCREASE
                : InventoryTransactionType.ADJUSTMENT_DECREASE;

        saveTransaction(inventory, type, delta, null, request.getNote(), null, operator);

        return toSnapshot(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLedgerResponse getLedger(Long variantId, int page, int size) {
        Inventory inventory = ensureInventory(variantId, false);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<InventoryLedgerEntryResponse> entries = transactionRepository
                .findAllByVariantIdOrderByCreatedAtDesc(variantId, pageable)
                .map(this::toLedgerEntry);

        ProductVariant variant = inventory.getVariant();
        Product product = variant.getProduct();

        return InventoryLedgerResponse.builder()
                .variantId(variant.getId())
                .sku(variant.getSku())
                .productName(product != null ? product.getName() : null)
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .entries(entries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventorySnapshotResponse> getLowStockAlerts(Integer threshold) {
        int safeThreshold = threshold == null ? DEFAULT_LOW_STOCK_THRESHOLD : Math.max(threshold, 0);
        return inventoryRepository.findAllByAvailableQuantityLessThan(safeThreshold).stream()
                .map(this::toSnapshot)
                .toList();
    }

    private void precheckAvailable(SystemInventoryRequest request) {
        for (InventoryLineRequest item : request.getItems()) {
            Inventory inventory = ensureInventory(item.getVariantId(), true);
            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Insufficient inventory for variantId=" + item.getVariantId());
            }
        }
    }

    private Inventory ensureInventory(Long variantId, boolean withLock) {
        if (withLock) {
            return inventoryRepository.findWithLockByVariantId(variantId)
                    .orElseGet(() -> createInventoryFromVariant(variantId));
        }

        return inventoryRepository.findByVariantId(variantId)
                .orElseGet(() -> createInventoryFromVariant(variantId));
    }

    private Inventory createInventoryFromVariant(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        Inventory inventory = Inventory.builder()
                .variant(variant)
                .availableQuantity(variant.getStock() == null ? 0 : variant.getStock())
                .reservedQuantity(0)
                .lowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD)
                .build();

        return inventoryRepository.save(inventory);
    }

    private void syncVariantStockWithAvailable(Inventory inventory) {
        ProductVariant variant = inventory.getVariant();
        variant.setStock(inventory.getAvailableQuantity());
        productVariantRepository.save(variant);
    }

    private void saveTransaction(
            Inventory inventory,
            InventoryTransactionType type,
            Integer quantityChange,
            String referenceCode,
            String note,
            Double unitCost,
            String createdBy
    ) {
        InventoryTransaction tx = InventoryTransaction.builder()
                .inventory(inventory)
                .variant(inventory.getVariant())
                .transactionType(type)
                .quantityChange(quantityChange)
                .balanceAvailable(inventory.getAvailableQuantity())
                .balanceReserved(inventory.getReservedQuantity())
                .referenceCode(referenceCode)
                .note(note)
                .unitCost(unitCost)
                .createdBy(createdBy)
                .build();

        transactionRepository.save(tx);
    }

    private SystemInventoryLineResponse toSystemLine(InventoryLineRequest item, Inventory inventory) {
        return SystemInventoryLineResponse.builder()
                .variantId(item.getVariantId())
                .quantity(item.getQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .build();
    }

    private InventorySnapshotResponse toSnapshot(Inventory inventory) {
        ProductVariant variant = inventory.getVariant();
        Product product = variant.getProduct();

        return InventorySnapshotResponse.builder()
                .variantId(variant.getId())
                .sku(variant.getSku())
                .productName(product != null ? product.getName() : null)
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .lowStockThreshold(inventory.getLowStockThreshold())
                .build();
    }

    private InventoryLedgerEntryResponse toLedgerEntry(InventoryTransaction tx) {
        return InventoryLedgerEntryResponse.builder()
                .id(tx.getId())
                .transactionType(tx.getTransactionType())
                .quantityChange(tx.getQuantityChange())
                .balanceAvailable(tx.getBalanceAvailable())
                .balanceReserved(tx.getBalanceReserved())
                .referenceCode(tx.getReferenceCode())
                .note(tx.getNote())
                .unitCost(tx.getUnitCost())
                .createdBy(tx.getCreatedBy())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
