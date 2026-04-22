package com.badminton.shop.modules.search.listener;

import com.badminton.shop.modules.search.event.ProductSearchSyncAction;
import com.badminton.shop.modules.search.event.ProductSearchSyncEvent;
import com.badminton.shop.modules.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSearchSyncListener {

    private final ProductSearchService productSearchService;

    @Async("searchSyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductSearchSync(ProductSearchSyncEvent event) {
        if (event.action() == ProductSearchSyncAction.DELETE) {
            productSearchService.deleteProduct(event.productId());
            log.debug("[search-sync] Deleted product {} from Elasticsearch index", event.productId());
            return;
        }

        productSearchService.upsertProduct(event.productId());
        log.debug("[search-sync] Upserted product {} to Elasticsearch index", event.productId());
    }
}
