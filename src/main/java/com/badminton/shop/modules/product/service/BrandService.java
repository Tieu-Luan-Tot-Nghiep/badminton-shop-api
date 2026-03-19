package com.badminton.shop.modules.product.service;

import com.badminton.shop.modules.product.dto.BrandRequest;
import com.badminton.shop.modules.product.dto.BrandResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BrandService {
    List<BrandResponse> getAllBrands();
    BrandResponse getBrandBySlug(String slug);
    BrandResponse createBrand(BrandRequest request);
    BrandResponse updateBrand(Long id, BrandRequest request);
    BrandResponse uploadLogo(Long id, MultipartFile file);
    void deleteBrand(Long id);
}
