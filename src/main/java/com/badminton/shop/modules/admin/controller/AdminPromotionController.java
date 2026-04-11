package com.badminton.shop.modules.admin.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.promotion.dto.response.PromotionResponse;
import com.badminton.shop.modules.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/promotions/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionService promotionService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPromotionById(@PathVariable Long id) {
        PromotionResponse response = promotionService.getPromotionById(id);
        return ResponseEntity.ok(ApiResponse.success("Get promotion successful", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponse.success("Delete promotion successful", null));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPromotionStats() {
        Map<String, Object> stats = promotionService.adminGetPromotionStats();
        return ResponseEntity.ok(ApiResponse.success("Get promotion stats successful", stats));
    }

    @GetMapping("/{id}/usages")
    public ResponseEntity<ApiResponse<Page<Object>>> getPromotionUsages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Object> usages = promotionService.adminGetPromotionUsages(id, page, size);
        return ResponseEntity.ok(ApiResponse.success("Get promotion usages successful", usages));
    }
}
