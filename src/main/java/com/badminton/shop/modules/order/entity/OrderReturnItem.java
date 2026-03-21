package com.badminton.shop.modules.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_return_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private OrderReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer requestedQuantity;

    @Column(nullable = false)
    private Integer receivedQuantity;

    @Enumerated(EnumType.STRING)
    private ReturnItemAction action;
}
