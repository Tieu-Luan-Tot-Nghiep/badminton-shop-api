package com.badminton.shop.modules.membership.entity;

import com.badminton.shop.modules.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tier_id", nullable = false)
    private MembershipTier tier;

    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer currentPoints = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer totalPoints = 0;
}
