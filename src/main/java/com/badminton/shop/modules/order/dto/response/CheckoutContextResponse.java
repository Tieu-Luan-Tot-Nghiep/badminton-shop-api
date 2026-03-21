package com.badminton.shop.modules.order.dto.response;

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
public class CheckoutContextResponse {

    @Builder.Default
    private List<CheckoutAddressResponse> addresses = new ArrayList<>();

    private CheckoutAddressResponse defaultAddress;
}
