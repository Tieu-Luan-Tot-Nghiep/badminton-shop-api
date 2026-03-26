package com.badminton.shop.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String receiverName;
    private String phoneNumber;
    private String province;
    private String district;
    private String ward;
    private String specificAddress;
    private Integer ghnProvinceId;
    private Integer ghnDistrictId;
    private String ghnWardCode;
    private Boolean isDefault;
}
