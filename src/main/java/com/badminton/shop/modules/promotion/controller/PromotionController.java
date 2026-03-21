package com.badminton.shop.modules.promotion.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.promotion.dto.request.PromotionRequest;
import com.badminton.shop.modules.promotion.dto.request.PromotionValidationRequest;
import com.badminton.shop.modules.promotion.dto.response.PromotionResponse;
import com.badminton.shop.modules.promotion.dto.response.PromotionValidationResponse;
import com.badminton.shop.modules.promotion.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(@Valid @RequestBody PromotionRequest request) {
        PromotionResponse response = promotionService.createPromotion(request);
        return ResponseEntity.ok(ApiResponse.success("Promotion created successfully.", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromotionResponse>> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionRequest request
    ) {
        PromotionResponse response = promotionService.updatePromotion(id, request);
        return ResponseEntity.ok(ApiResponse.success("Promotion updated successfully.", response));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromotionResponse>> setPromotionActive(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        PromotionResponse response = promotionService.setPromotionActive(id, active);
        return ResponseEntity.ok(ApiResponse.success("Promotion status updated successfully.", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PromotionResponse>>> getPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") Boolean activeOnly
    ) {
        Page<PromotionResponse> response = promotionService.getPromotions(page, size, activeOnly);
        return ResponseEntity.ok(ApiResponse.success("Promotions fetched successfully.", response));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPromotionByCode(@PathVariable String code) {
        PromotionResponse response = promotionService.getPromotionByCode(code);
        return ResponseEntity.ok(ApiResponse.success("Promotion fetched successfully.", response));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<PromotionValidationResponse>> validatePromotion(
            @Valid @RequestBody PromotionValidationRequest request
    ) {
        PromotionValidationResponse response = promotionService.validatePromotion(
                request.getVoucherCode(),
                request.getItemsAmount(),
                request.getShippingFee()
        );
        return ResponseEntity.ok(ApiResponse.success("Promotion validated successfully.", response));
    }
}
