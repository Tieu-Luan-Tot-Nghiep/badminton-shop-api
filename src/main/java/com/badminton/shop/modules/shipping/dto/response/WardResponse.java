package com.badminton.shop.modules.shipping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardResponse {
    private String wardCode;
    private String wardName;
    private Integer districtId;
}
