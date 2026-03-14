package com.badminton.shop.service;

import com.badminton.shop.dto.request.ProductRequest;
import com.badminton.shop.dto.response.PageResponse;
import com.badminton.shop.dto.response.ProductResponse;

public interface ProductService {

    PageResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir);

    PageResponse<ProductResponse> getProductsByCategory(Long categoryId, int page, int size);

    PageResponse<ProductResponse> searchProducts(String keyword, int page, int size);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySlug(String slug);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);
}
