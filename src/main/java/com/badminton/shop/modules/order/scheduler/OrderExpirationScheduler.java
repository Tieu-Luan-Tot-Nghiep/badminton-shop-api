package com.badminton.shop.modules.order.scheduler;

import com.badminton.shop.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.order.expire-check-ms:60000}")
    public void autoCancelExpiredVnpayOrders() {
        int affected = orderService.autoCancelExpiredPendingVnpayOrders();
        if (affected > 0) {
            log.info("Auto-cancelled {} expired VNPAY orders.", affected);
        }
    }
}
