package com.badminton.shop.modules.inventory.service.impl;

import com.badminton.shop.modules.inventory.dto.*;
import com.badminton.shop.modules.inventory.entity.Inventory;
import com.badminton.shop.modules.inventory.entity.InventoryTransaction;
import com.badminton.shop.modules.inventory.entity.InventoryTransactionType;
import com.badminton.shop.modules.inventory.repository.InventoryRepository;
import com.badminton.shop.modules.inventory.repository.InventoryTransactionRepository;
import com.badminton.shop.modules.product.entity.Product;
import com.badminton.shop.modules.product.entity.ProductVariant;
import com.badminton.shop.modules.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private ProductVariant variant;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        Product product = Product.builder()
                .id(10L)
                .name("Yonex Astrox 88D")
                .build();

        variant = ProductVariant.builder()
                .id(1L)
                .sku("SKU-001")
                .stock(10)
                .product(product)
                .build();

        inventory = Inventory.builder()
                .id(100L)
                .variant(variant)
                .availableQuantity(10)
                .reservedQuantity(0)
                .lowStockThreshold(5)
                .build();

                Mockito.lenient().when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
                Mockito.lenient().when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));
                Mockito.lenient().when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void reserveInventory_ShouldMoveAvailableToReserved() {
        when(inventoryRepository.findWithLockByVariantId(1L)).thenReturn(Optional.of(inventory));

        SystemInventoryRequest request = SystemInventoryRequest.builder()
                .referenceCode("ORD-001")
                .note("reserve")
                .items(List.of(InventoryLineRequest.builder().variantId(1L).quantity(3).build()))
                .build();

        SystemInventoryResponse response = inventoryService.reserveInventory(request);

        assertNotNull(response);
        assertEquals("ORD-001", response.getReferenceCode());
        assertEquals(7, inventory.getAvailableQuantity());
        assertEquals(3, inventory.getReservedQuantity());
        assertEquals(7, variant.getStock());

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(InventoryTransactionType.RESERVE, captor.getValue().getTransactionType());
        assertEquals(-3, captor.getValue().getQuantityChange());
    }

    @Test
    void commitInventory_ShouldDeductFromReservedOnly() {
        inventory.setAvailableQuantity(7);
        inventory.setReservedQuantity(3);
        variant.setStock(7);
        when(inventoryRepository.findWithLockByVariantId(1L)).thenReturn(Optional.of(inventory));

        SystemInventoryRequest request = SystemInventoryRequest.builder()
                .referenceCode("ORD-001")
                .note("commit")
                .items(List.of(InventoryLineRequest.builder().variantId(1L).quantity(2).build()))
                .build();

        SystemInventoryResponse response = inventoryService.commitInventory(request);

        assertNotNull(response);
        assertEquals(7, inventory.getAvailableQuantity());
        assertEquals(1, inventory.getReservedQuantity());
        assertEquals(7, variant.getStock());

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(InventoryTransactionType.COMMIT, captor.getValue().getTransactionType());
        assertEquals(-2, captor.getValue().getQuantityChange());
    }

    @Test
    void rollbackInventory_ShouldRestoreReservedToAvailable() {
        inventory.setAvailableQuantity(7);
        inventory.setReservedQuantity(3);
        variant.setStock(7);
        when(inventoryRepository.findWithLockByVariantId(1L)).thenReturn(Optional.of(inventory));

        SystemInventoryRequest request = SystemInventoryRequest.builder()
                .referenceCode("ORD-001")
                .note("rollback")
                .items(List.of(InventoryLineRequest.builder().variantId(1L).quantity(2).build()))
                .build();

        SystemInventoryResponse response = inventoryService.rollbackInventory(request);

        assertNotNull(response);
        assertEquals(9, inventory.getAvailableQuantity());
        assertEquals(1, inventory.getReservedQuantity());
        assertEquals(9, variant.getStock());

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(InventoryTransactionType.ROLLBACK, captor.getValue().getTransactionType());
        assertEquals(2, captor.getValue().getQuantityChange());
    }

    @Test
    void stocktakeAdjust_ShouldCreateDecreaseAdjustmentWhenActualLower() {
        inventory.setAvailableQuantity(10);
        inventory.setReservedQuantity(1);
        variant.setStock(10);
        when(inventoryRepository.findWithLockByVariantId(1L)).thenReturn(Optional.of(inventory));

        StocktakeAdjustmentRequest request = StocktakeAdjustmentRequest.builder()
                .variantId(1L)
                .actualAvailableQuantity(8)
                .note("monthly stocktake")
                .build();

        InventorySnapshotResponse response = inventoryService.stocktakeAdjust("admin@shop.com", request);

        assertNotNull(response);
        assertEquals(8, response.getAvailableQuantity());
        assertEquals(8, variant.getStock());

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(InventoryTransactionType.ADJUSTMENT_DECREASE, captor.getValue().getTransactionType());
        assertEquals(-2, captor.getValue().getQuantityChange());
    }

    @Test
    void getLowStockAlerts_ShouldReturnOnlyBelowThreshold() {
        Inventory low1 = Inventory.builder()
                .id(1L)
                .variant(variant)
                .availableQuantity(2)
                .reservedQuantity(0)
                .lowStockThreshold(5)
                .build();

        ProductVariant variant2 = ProductVariant.builder()
                .id(2L)
                .sku("SKU-002")
                .stock(1)
                .product(Product.builder().id(11L).name("Lining Turbo") .build())
                .build();

        Inventory low2 = Inventory.builder()
                .id(2L)
                .variant(variant2)
                .availableQuantity(1)
                .reservedQuantity(0)
                .lowStockThreshold(5)
                .build();

        when(inventoryRepository.findAllByAvailableQuantityLessThan(5)).thenReturn(List.of(low1, low2));

        List<InventorySnapshotResponse> responses = inventoryService.getLowStockAlerts(5);

        assertEquals(2, responses.size());
        assertTrue(responses.stream().anyMatch(r -> r.getVariantId().equals(1L) && r.getAvailableQuantity() == 2));
        assertTrue(responses.stream().anyMatch(r -> r.getVariantId().equals(2L) && r.getAvailableQuantity() == 1));
        assertFalse(responses.isEmpty());
    }
}
