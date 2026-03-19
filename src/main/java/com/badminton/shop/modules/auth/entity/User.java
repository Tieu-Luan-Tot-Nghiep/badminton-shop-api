package com.badminton.shop.modules.auth.entity;

import com.badminton.shop.modules.order.entity.Cart;
import com.badminton.shop.modules.order.entity.Order;
import com.badminton.shop.modules.review.entity.Review;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String fullName;

    private LocalDate birthDate;

    @Column(nullable = true)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private AuthProvider provider;

    private String providerId;

    @Column(unique = true, nullable = false)
    private String email;

    private String avatar;

    @Column(unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserAddress> addresses = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (isEmailVerified == null) isEmailVerified = false;
        if (isDeleted == null) isDeleted = false;
        if (provider == null) provider = AuthProvider.LOCAL;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
