package com.badminton.shop.modules.product.entity;

import com.badminton.shop.modules.inventory.entity.Inventory;
import com.badminton.shop.modules.order.entity.CartItem;
import com.badminton.shop.modules.order.entity.OrderItem;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sku;

    private String weight;    // VD: 3U/4U
    private String gripSize;  // VD: G4/G5
    private String size;      // Quần áo, giày
    private String color;     // Màu sắc

    @Column(nullable = false)
    private Double originalPrice;

    @Column(nullable = false)
    private Double price;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToOne(mappedBy = "variant", cascade = CascadeType.ALL)
    private Inventory inventory;

    @OneToMany(mappedBy = "variant")
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "variant")
    private List<OrderItem> orderItems = new ArrayList<>();
}
