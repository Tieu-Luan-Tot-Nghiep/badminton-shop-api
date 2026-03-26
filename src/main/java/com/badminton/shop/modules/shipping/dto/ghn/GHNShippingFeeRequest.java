package com.badminton.shop.modules.shipping.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class GHNShippingFeeRequest {
    @JsonProperty("from_district_id")
    private Integer fromDistrictId;

    @JsonProperty("to_district_id")
    private Integer toDistrictId;

    @JsonProperty("to_ward_code")
    private String toWardCode;

    @JsonProperty("service_type_id")
    private Integer serviceTypeId;

    @JsonProperty("insurance_value")
    private Integer insuranceValue;

    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;

    @Builder.Default
    private List<GHNItem> items = new ArrayList<>();
}
