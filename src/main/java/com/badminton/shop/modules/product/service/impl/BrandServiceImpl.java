package com.badminton.shop.modules.product.service.impl;

import com.badminton.shop.exception.DuplicateBrandException;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.product.dto.BrandRequest;
import com.badminton.shop.modules.product.dto.BrandResponse;
import com.badminton.shop.modules.product.entity.Brand;
import com.badminton.shop.modules.product.repository.BrandRepository;
import com.badminton.shop.modules.product.service.BrandService;
import com.badminton.shop.utils.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final S3Service s3Service;

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrandBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thương hiệu với slug: " + slug));
        return mapToResponse(brand);
    }

    @Override
    public BrandResponse createBrand(BrandRequest request) {
        if (brandRepository.existsByName(request.getName())) {
            throw new DuplicateBrandException(
                    "Thương hiệu '" + request.getName() + "' đã tồn tại");
        }

        Brand brand = Brand.builder()
            .name(request.getName())
            .slug(generateSlug(request.getName()))
            .description(request.getDescription())
            .build();

        Brand saved = brandRepository.save(brand);
        return mapToResponse(saved);
    }

    @Override
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thương hiệu với id: " + id));

        if (brandRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new DuplicateBrandException(
                    "Thương hiệu '" + request.getName() + "' đã tồn tại");
        }

        brand.setName(request.getName());
        brand.setSlug(generateSlug(request.getName()));
        brand.setDescription(request.getDescription());
        Brand saved = brandRepository.save(brand);
        return mapToResponse(saved);
    }

    @Override
    public BrandResponse uploadLogo(Long id, MultipartFile file) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thương hiệu với id: " + id));

        if (brand.getLogoUrl() != null && !brand.getLogoUrl().isEmpty()) {
            s3Service.deleteFile(brand.getLogoUrl());
        }

        String fileName = brand.getSlug() + "-" + UUID.randomUUID();
        String logoUrl = s3Service.uploadFile("brands", fileName, file);

        brand.setLogoUrl(logoUrl);
        Brand saved = brandRepository.save(brand);
        return mapToResponse(saved);
    }

    @Override
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thương hiệu với id: " + id));
        brandRepository.delete(brand); // Soft delete via @SQLDelete
    }

    // ===== Helper methods =====

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("");
        slug = slug.toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug;
    }

    private BrandResponse mapToResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .description(brand.getDescription())
                .logoUrl(brand.getLogoUrl())
                .build();
    }

}
