package com.badminton.shop.modules.membership.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "membership_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // BRONZE, SILVER, GOLD, DIAMOND

    @Column(nullable = false)
    private Integer minPoints;

    @Column(nullable = false)
    private BigDecimal discountPercent;

    private String benefits;
}
