package com.badminton.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items", indexes = {
        @Index(name = "idx_cart_items_cart", columnList = "cart_id"),
        @Index(name = "idx_cart_items_product", columnList = "product_id"),
        @Index(name = "idx_cart_items_cart_product", columnList = "cart_id, product_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
