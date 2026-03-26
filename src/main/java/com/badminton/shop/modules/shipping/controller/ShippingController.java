package com.badminton.shop.modules.shipping.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.shipping.dto.response.DistrictResponse;
import com.badminton.shop.modules.shipping.dto.response.ProvinceResponse;
import com.badminton.shop.modules.shipping.dto.response.ShippingOrderResponse;
import com.badminton.shop.modules.shipping.dto.response.WardResponse;
import com.badminton.shop.modules.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getProvinces() {
        return ResponseEntity.ok(ApiResponse.success("Provinces fetched successfully.", shippingService.getProvinces()));
    }

    @GetMapping("/provinces/{provinceId}/districts")
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> getDistricts(@PathVariable Integer provinceId) {
        return ResponseEntity.ok(ApiResponse.success("Districts fetched successfully.", shippingService.getDistricts(provinceId)));
    }

    @GetMapping("/districts/{districtId}/wards")
    public ResponseEntity<ApiResponse<List<WardResponse>>> getWards(@PathVariable Integer districtId) {
        return ResponseEntity.ok(ApiResponse.success("Wards fetched successfully.", shippingService.getWards(districtId)));
    }

    @GetMapping("/orders/{shippingCode}")
    public ResponseEntity<ApiResponse<ShippingOrderResponse>> getShippingDetail(@PathVariable String shippingCode) {
        return ResponseEntity.ok(ApiResponse.success("Shipping detail fetched successfully.", shippingService.getShippingDetail(shippingCode)));
    }
}
