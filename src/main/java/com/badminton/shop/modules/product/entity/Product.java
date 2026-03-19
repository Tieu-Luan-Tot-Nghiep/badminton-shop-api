package com.badminton.shop.modules.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE products SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(Types.VARCHAR) // Ép kiểu dữ liệu JDBC về VARCHAR
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "base_price", precision = 15, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ProductVariant> variants = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ProductImage> productImages = new LinkedHashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addProductImage(ProductImage image) {
        productImages.add(image);
        image.setProduct(this);
    }

    public void removeProductImage(ProductImage image) {
        if (productImages.remove(image)) {
            image.setProduct(null);
        }
    }

    public void clearProductImages() {
        for (ProductImage image : new LinkedHashSet<>(productImages)) {
            removeProductImage(image);
        }
    }

    public void addProductVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this);
    }

    public void removeProductVariant(ProductVariant variant) {
        if (variants.remove(variant)) {
            variant.setProduct(null);
        }
    }

    public void clearProductVariants() {
        for (ProductVariant variant : new LinkedHashSet<>(variants)) {
            removeProductVariant(variant);
        }
    }
}
