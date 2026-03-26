package com.badminton.shop.modules.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "user_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE user_addresses SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false")
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String receiverName;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String ward;

    @Column(nullable = false)
    private String specificAddress;

    // Cached GHN master-data IDs/codes for stable shipping fee/order creation.
    private Integer ghnProvinceId;

    private Integer ghnDistrictId;

    private String ghnWardCode;

    private Boolean isDefault;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
