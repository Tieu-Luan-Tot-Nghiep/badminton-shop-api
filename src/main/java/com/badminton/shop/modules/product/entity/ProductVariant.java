package com.badminton.shop.modules.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE product_variants SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sku;

    private String weight;    // VD: 3U/4U
    private String gripSize;  // VD: G4/G5
    @Column(nullable = false)
    private String size;      // Quần áo, giày
    @Column(nullable = false)
    private String color;     // Màu sắc

    @Column(nullable = false)
    private Double originalPrice;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
