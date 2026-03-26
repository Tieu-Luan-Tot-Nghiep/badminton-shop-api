package com.badminton.shop.modules.shipping.service;

import com.badminton.shop.modules.shipping.dto.ghn.GHNOrderRequest;
import com.badminton.shop.modules.shipping.dto.ghn.GHNShippingFeeRequest;
import com.badminton.shop.modules.shipping.dto.response.DistrictResponse;
import com.badminton.shop.modules.shipping.dto.response.ProvinceResponse;
import com.badminton.shop.modules.shipping.dto.response.ShippingOrderResponse;
import com.badminton.shop.modules.shipping.dto.response.WardResponse;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingProvider {

    BigDecimal calculateShippingFee(GHNShippingFeeRequest request);

    ShippingOrderResponse createShippingOrder(GHNOrderRequest request);

    ShippingOrderResponse getShippingDetail(String shippingCode);

    List<ProvinceResponse> getProvinces();

    List<DistrictResponse> getDistricts(Integer provinceId);

    List<WardResponse> getWards(Integer districtId);
}
