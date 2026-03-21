package com.badminton.shop.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class AvailabilityCheckRequest {

    @Valid
    @NotEmpty(message = "items cannot be empty")
    @Builder.Default
    private List<InventoryLineRequest> items = new ArrayList<>();
}
