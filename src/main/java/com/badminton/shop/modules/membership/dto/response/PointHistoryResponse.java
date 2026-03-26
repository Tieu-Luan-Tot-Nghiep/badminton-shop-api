package com.badminton.shop.modules.membership.dto.response;

import java.time.LocalDateTime;

public record PointHistoryResponse(
    Long id,
    Integer points,
    String reason,
    Long referenceId,
    LocalDateTime createdAt
) {}
