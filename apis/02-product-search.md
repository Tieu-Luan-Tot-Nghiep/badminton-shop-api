# Product + Search + Brand + Category Module

## A. PRODUCT MODULE
Base path: /api/products

### Public endpoints

### 1) getProducts
- Method name: getProducts
- Endpoint: GET /api/products
- Chuc nang: Lay danh sach san pham co loc + sort + paging.
- Auth: Public
- Query:
  - category, brand, minPrice, maxPrice, keyword, sortBy, sortDir, page, size
- Response data: PagedResponse<ProductListResponse>
- Code: 200, 400

### 2) checkSlug
- Method name: checkSlug
- Endpoint: GET /api/products/search/existsBySlug
- Chuc nang: Kiem tra slug da ton tai chua.
- Auth: Public
- Query: slug
- Response data: { exists: boolean }
- Code: 200

### 3) getProductById
- Method name: getProductById
- Endpoint: GET /api/products/{id}
- Chuc nang: Lay chi tiet san pham.
- Auth: Public
- Response data: ProductResponse (gom productImages + productVariants)
- Code: 200, 404

### 4) getFeaturedProducts
- Method name: getFeaturedProducts
- Endpoint: GET /api/products/featured
- Chuc nang: Lay san pham noi bat.
- Auth: Public
- Query: limit (default 8)
- Response data: List<ProductListResponse>
- Code: 200

### 5) getNewestProducts
- Method name: getNewestProducts
- Endpoint: GET /api/products/new
- Chuc nang: Lay san pham moi.
- Auth: Public
- Query: limit (default 8)
- Response data: List<ProductListResponse>
- Code: 200

### 6) compareProducts
- Method name: compareProducts
- Endpoint: GET /api/products/compare
- Chuc nang: So sanh 2 variant.
- Auth: Public
- Query: variantIds (2 phan tu)
- Response data: ProductCompareResponse
- Code: 200, 400, 404

### 7) getMyWishlist
- Method name: getMyWishlist
- Endpoint: GET /api/products/wishlist
- Chuc nang: Lay wishlist cua user.
- Auth: Bearer User
- Response data: List<WishlistItemResponse>
- Code: 200, 401

### 8) addToWishlist
- Method name: addToWishlist
- Endpoint: POST /api/products/wishlist/{productId}
- Chuc nang: Them san pham vao wishlist.
- Auth: Bearer User
- Response data: WishlistItemResponse
- Code: 201, 401, 404

### 9) removeFromWishlist
- Method name: removeFromWishlist
- Endpoint: DELETE /api/products/wishlist/{productId}
- Chuc nang: Xoa san pham khoi wishlist.
- Auth: Bearer User
- Response data: null
- Code: 200, 401

### 10) isInWishlist
- Method name: isInWishlist
- Endpoint: GET /api/products/wishlist/{productId}/exists
- Chuc nang: Kiem tra san pham co trong wishlist.
- Auth: Bearer User
- Response data: { exists: boolean }
- Code: 200, 401

### Admin endpoints

### 11) createProduct
- Method name: createProduct
- Endpoint: POST /api/products
- Chuc nang: Tao product metadata.
- Auth: ADMIN
- Request body: ProductRequest
  - name, shortDescription, description, basePrice, categoryId, brandId
- Response data: ProductResponse
- Code: 201, 400, 401, 403

### 12) updateProduct
- Method name: updateProduct
- Endpoint: PUT /api/products/{id}
- Chuc nang: Cap nhat product metadata.
- Auth: ADMIN
- Request body: ProductRequest
- Response data: ProductResponse
- Code: 200, 400, 401, 403, 404

### 13) uploadThumbnail
- Method name: uploadThumbnail
- Endpoint: POST /api/products/{id}/thumbnail
- Chuc nang: Upload thumbnail.
- Auth: ADMIN
- Request: multipart/form-data (file)
- Response data: ProductResponse
- Code: 200, 400, 401, 403

### 14) toggleStatus
- Method name: toggleStatus
- Endpoint: PATCH /api/products/{id}/status
- Chuc nang: Bat/tat isActive.
- Auth: ADMIN
- Response data: ProductResponse
- Code: 200, 401, 403, 404

### 15) deleteProduct
- Method name: deleteProduct
- Endpoint: DELETE /api/products/{id}
- Chuc nang: Xoa product.
- Auth: ADMIN
- Response data: null
- Code: 200, 401, 403, 404

### 16) getProductVariants
- Method name: getProductVariants
- Endpoint: GET /api/products/{productId}/variants
- Chuc nang: Lay variants cua product.
- Auth: ADMIN
- Response data: List<ProductVariantResponse>
- Code: 200, 401, 403

### 17) createProductVariant
- Method name: createProductVariant
- Endpoint: POST /api/products/{productId}/variants
- Chuc nang: Tao variant.
- Auth: ADMIN
- Request body: ProductVariantRequest
  - sku, size, color, price, stock
- Response data: ProductVariantResponse
- Code: 201, 400, 401, 403

### 18) updateProductVariant
- Method name: updateProductVariant
- Endpoint: PUT /api/products/{productId}/variants/{variantId}
- Chuc nang: Cap nhat variant.
- Auth: ADMIN
- Request body: ProductVariantRequest
- Response data: ProductVariantResponse
- Code: 200, 400, 401, 403, 404

### 19) deleteProductVariant
- Method name: deleteProductVariant
- Endpoint: DELETE /api/products/{productId}/variants/{variantId}
- Chuc nang: Xoa variant.
- Auth: ADMIN
- Response data: null
- Code: 200, 401, 403, 404

### 20) getProductImages
- Method name: getProductImages
- Endpoint: GET /api/products/{productId}/images
- Chuc nang: Lay gallery anh.
- Auth: ADMIN
- Response data: List<ProductImageResponse>
- Code: 200, 401, 403

### 21) uploadProductImage
- Method name: uploadProductImage
- Endpoint: POST /api/products/{productId}/images
- Chuc nang: Upload anh theo mau.
- Auth: ADMIN
- Request: multipart/form-data
  - file
  - color
  - isMain
- Response data: ProductImageResponse
- Code: 201, 400, 401, 403

### 22) updateProductImage
- Method name: updateProductImage
- Endpoint: PUT /api/products/{productId}/images/{imageId}
- Chuc nang: Cap nhat metadata anh.
- Auth: ADMIN
- Request body: ProductImageRequest
- Response data: ProductImageResponse
- Code: 200, 400, 401, 403, 404

### 23) deleteProductImage
- Method name: deleteProductImage
- Endpoint: DELETE /api/products/{productId}/images/{imageId}
- Chuc nang: Xoa anh.
- Auth: ADMIN
- Response data: null
- Code: 200, 401, 403, 404

---

## B. SEARCH MODULE
Base path: /api/search/products

### 1) searchProducts
- Method name: searchProducts
- Endpoint: GET /api/search/products
- Chuc nang: Tim kiem full-text + semantic option + filter.
- Auth: Public
- Query:
  - keyword, category, brand, minPrice, maxPrice
  - sortBy, sortDir, page, size, activeOnly, useSemantic
- Response data: ProductSearchPageResponse
- Code: 200, 400

### 2) reindexProducts
- Method name: reindexProducts
- Endpoint: POST /api/search/products/reindex
- Chuc nang: Re-index Elasticsearch.
- Auth: ADMIN
- Response data: { result: "ok" }
- Code: 200, 401, 403

### 3) searchProductsByImage
- Method name: searchProductsByImage
- Endpoint: POST /api/search/products/by-image
- Chuc nang: Tim san pham bang hinh anh.
- Auth: Public
- Request: multipart/form-data
  - image
  - page, size, activeOnly
- Response data: ProductSearchPageResponse
- Code: 200, 400

### 4) suggestSimilarProducts
- Method name: suggestSimilarProducts
- Endpoint: GET /api/search/products/similar
- Chuc nang: Goi y san pham tuong tu theo productId.
- Auth: Public
- Query: productId, page, size, activeOnly
- Response data: ProductSearchPageResponse
- Code: 200, 404

### 5) suggestKeywords
- Method name: suggestKeywords
- Endpoint: GET /api/search/products/suggestions
- Chuc nang: Goi y tu khoa tim kiem.
- Auth: Public
- Query: query, size
- Response data: ProductSearchSuggestionResponse
- Code: 200

### 6) getTrendingSearches
- Method name: getTrendingSearches
- Endpoint: GET /api/search/products/trending
- Chuc nang: Lay xu huong tim kiem.
- Auth: Public
- Query: days, size
- Response data: ProductSearchTrendingResponse
- Code: 200

---

## C. BRAND MODULE
Base path: /api/brands

### 1) getAllBrands
- Method name: getAllBrands
- Endpoint: GET /api/brands
- Chuc nang: Lay danh sach brand.
- Auth: Public
- Response data: List<BrandResponse>
- Code: 200

### 2) getBrandBySlug
- Method name: getBrandBySlug
- Endpoint: GET /api/brands/{slug}
- Chuc nang: Lay brand theo slug.
- Auth: Public
- Response data: BrandResponse
- Code: 200, 404

### 3) createBrand
- Method name: createBrand
- Endpoint: POST /api/brands
- Chuc nang: Tao brand.
- Auth: ADMIN
- Request body: BrandRequest
- Response data: BrandResponse
- Code: 201, 400, 401, 403

### 4) updateBrand
- Method name: updateBrand
- Endpoint: PUT /api/brands/{id}
- Chuc nang: Cap nhat brand.
- Auth: ADMIN
- Request body: BrandRequest
- Response data: BrandResponse
- Code: 200, 400, 401, 403, 404

### 5) uploadLogo
- Method name: uploadLogo
- Endpoint: POST /api/brands/{id}/logo
- Chuc nang: Upload logo brand.
- Auth: ADMIN
- Request: multipart/form-data (file)
- Response data: BrandResponse
- Code: 200, 400, 401, 403

### 6) deleteBrand
- Method name: deleteBrand
- Endpoint: DELETE /api/brands/{id}
- Chuc nang: Xoa brand.
- Auth: ADMIN
- Response data: null
- Code: 200, 401, 403, 404

---

## D. CATEGORY MODULE
Base path: /api/categories

### 1) getAllCategories
- Method name: getAllCategories
- Endpoint: GET /api/categories
- Chuc nang: Lay category tree.
- Auth: Public
- Response data: List<CategoryResponse>
- Code: 200

### 2) getCategoryBySlug
- Method name: getCategoryBySlug
- Endpoint: GET /api/categories/{slug}
- Chuc nang: Lay category theo slug.
- Auth: Public
- Response data: CategoryResponse
- Code: 200, 404

### 3) createCategory
- Method name: createCategory
- Endpoint: POST /api/categories
- Chuc nang: Tao category.
- Auth: ADMIN
- Request body: CategoryRequest
- Response data: CategoryResponse
- Code: 201, 400, 401, 403

### 4) updateCategory
- Method name: updateCategory
- Endpoint: PUT /api/categories/{id}
- Chuc nang: Cap nhat category.
- Auth: ADMIN
- Request body: CategoryRequest
- Response data: CategoryResponse
- Code: 200, 400, 401, 403, 404

### 5) deleteCategory
- Method name: deleteCategory
- Endpoint: DELETE /api/categories/{id}
- Chuc nang: Xoa category.
- Auth: ADMIN
- Response data: null
- Code: 200, 401, 403, 404
