package com.badminton.shop.modules.search.event;

public record ProductSearchSyncEvent(Long productId, ProductSearchSyncAction action) {
}
