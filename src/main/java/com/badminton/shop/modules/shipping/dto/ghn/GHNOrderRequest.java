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
public class GHNOrderRequest {
    @JsonProperty("payment_type_id")
    private Integer paymentTypeId;

    private String note;

    @JsonProperty("required_note")
    private String requiredNote;

    @JsonProperty("to_name")
    private String toName;

    @JsonProperty("to_phone")
    private String toPhone;

    @JsonProperty("to_address")
    private String toAddress;

    @JsonProperty("to_ward_code")
    private String toWardCode;

    @JsonProperty("to_district_id")
    private Integer toDistrictId;

    @JsonProperty("from_district_id")
    private Integer fromDistrictId;

    @JsonProperty("cod_amount")
    private Integer codAmount;

    @JsonProperty("insurance_value")
    private Integer insuranceValue;

    @JsonProperty("service_type_id")
    private Integer serviceTypeId;

    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;

    @JsonProperty("client_order_code")
    private String clientOrderCode;

    @Builder.Default
    private List<GHNItem> items = new ArrayList<>();
}
