package com.badminton.shop.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityCheckResponse {

    private boolean allAvailable;

    @Builder.Default
    private List<AvailabilityLineResponse> items = new ArrayList<>();
}
