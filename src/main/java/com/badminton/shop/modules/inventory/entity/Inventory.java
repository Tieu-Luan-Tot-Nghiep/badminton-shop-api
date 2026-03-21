package com.badminton.shop.modules.inventory.entity;

import com.badminton.shop.modules.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    // Backward-compatible field for existing schema where stock_quantity is NOT NULL.
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @PrePersist
    protected void onCreate() {
        if (availableQuantity == null) {
            availableQuantity = 0;
        }
        if (stockQuantity == null) {
            stockQuantity = availableQuantity;
        }
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
        if (lowStockThreshold == null) {
            lowStockThreshold = 5;
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        stockQuantity = availableQuantity;
        updatedAt = LocalDateTime.now();
    }
}
