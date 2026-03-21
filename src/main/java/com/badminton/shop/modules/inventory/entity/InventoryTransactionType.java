package com.badminton.shop.modules.inventory.entity;

public enum InventoryTransactionType {
    STOCK_IN,
    STOCK_OUT,
    RESERVE,
    COMMIT,
    ROLLBACK,
    ADJUSTMENT_INCREASE,
    ADJUSTMENT_DECREASE
}
