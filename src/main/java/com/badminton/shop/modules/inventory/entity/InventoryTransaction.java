package com.badminton.shop.modules.inventory.entity;

import com.badminton.shop.modules.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private InventoryTransactionType transactionType;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "balance_available", nullable = false)
    private Integer balanceAvailable;

    @Column(name = "balance_reserved", nullable = false)
    private Integer balanceReserved;

    @Column(name = "reference_code")
    private String referenceCode;

    @Column(name = "note")
    private String note;

    @Column(name = "unit_cost")
    private Double unitCost;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
