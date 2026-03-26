package com.badminton.shop.modules.shipping.service;

import com.badminton.shop.modules.shipping.dto.request.ShippingFeeCalculationRequest;
import com.badminton.shop.modules.shipping.dto.request.ShippingOrderCreationRequest;
import com.badminton.shop.modules.shipping.dto.response.DistrictResponse;
import com.badminton.shop.modules.shipping.dto.response.ProvinceResponse;
import com.badminton.shop.modules.shipping.dto.response.ShippingOrderResponse;
import com.badminton.shop.modules.shipping.dto.response.WardResponse;

import java.math.BigDecimal;
import java.util.List;

public interface ShippingService {

    BigDecimal calculateShippingFee(ShippingFeeCalculationRequest request);

    ShippingOrderResponse createShippingOrder(ShippingOrderCreationRequest request);

    ShippingOrderResponse getShippingDetail(String shippingCode);

    List<ProvinceResponse> getProvinces();

    List<DistrictResponse> getDistricts(Integer provinceId);

    List<WardResponse> getWards(Integer districtId);
}
