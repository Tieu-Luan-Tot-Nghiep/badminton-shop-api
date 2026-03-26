package com.badminton.shop.modules.shipping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistrictResponse {
    private Integer districtId;
    private String districtName;
    private Integer provinceId;
}
