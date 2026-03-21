package com.badminton.shop.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class SystemInventoryRequest {

    @NotBlank(message = "referenceCode is required")
    private String referenceCode;

    private String note;

    @Valid
    @NotEmpty(message = "items cannot be empty")
    @Builder.Default
    private List<InventoryLineRequest> items = new ArrayList<>();
}
