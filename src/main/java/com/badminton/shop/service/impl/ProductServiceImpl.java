package com.badminton.shop.service.impl;

import com.badminton.shop.dto.request.ProductRequest;
import com.badminton.shop.dto.response.PageResponse;
import com.badminton.shop.dto.response.ProductResponse;
import com.badminton.shop.entity.Category;
import com.badminton.shop.entity.Product;
import com.badminton.shop.exception.DuplicateResourceException;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.repository.CategoryRepository;
import com.badminton.shop.repository.ProductRepository;
import com.badminton.shop.service.ProductService;
import com.badminton.shop.util.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findAllByActiveTrue(pageable);
        return AppUtils.toPageResponse(productPage, productPage.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByCategory(Long categoryId, int page, int size) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Danh mục", "id", categoryId);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> productPage = productRepository.findAllByCategoryIdAndActiveTrue(categoryId, pageable);
        return AppUtils.toPageResponse(productPage, productPage.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> productPage = productRepository.searchByKeyword(keyword, pageable);
        return AppUtils.toPageResponse(productPage, productPage.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "slug", slug));
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", request.getCategoryId()));

        String slug = AppUtils.toSlug(request.getName());
        if (productRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Sản phẩm với tên '" + request.getName() + "' đã tồn tại");
        }

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .price(request.getPrice())
                .salePrice(request.getSalePrice())
                .stockQuantity(request.getStockQuantity())
                .brand(request.getBrand())
                .imageUrl(request.getImageUrl())
                .category(category)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Product saved = productRepository.save(product);
        log.info("Tạo sản phẩm mới ID: {}, tên: {}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findById(id);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", request.getCategoryId()));

        String newSlug = AppUtils.toSlug(request.getName());
        if (!product.getSlug().equals(newSlug) && productRepository.existsBySlug(newSlug)) {
            throw new DuplicateResourceException("Sản phẩm với tên '" + request.getName() + "' đã tồn tại");
        }

        product.setName(request.getName());
        product.setSlug(newSlug);
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSalePrice(request.getSalePrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setBrand(request.getBrand());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updated = productRepository.save(product);
        log.info("Cập nhật sản phẩm ID: {}", updated.getId());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findById(id);
        product.setActive(false);
        productRepository.save(product);
        log.info("Vô hiệu hóa sản phẩm ID: {}", id);
    }

    private Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .effectivePrice(product.getEffectivePrice())
                .stockQuantity(product.getStockQuantity())
                .brand(product.getBrand())
                .imageUrl(product.getImageUrl())
                .active(product.isActive())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
