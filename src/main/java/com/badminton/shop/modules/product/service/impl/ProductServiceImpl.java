package com.badminton.shop.modules.product.service.impl;

import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.order.repository.CartItemRepository;
import com.badminton.shop.modules.product.dto.ProductCompareItemResponse;
import com.badminton.shop.modules.product.dto.ProductCompareResponse;
import com.badminton.shop.modules.product.dto.ProductImageRequest;
import com.badminton.shop.modules.product.dto.ProductImageResponse;
import com.badminton.shop.modules.product.dto.ProductListResponse;
import com.badminton.shop.modules.product.dto.ProductRequest;
import com.badminton.shop.modules.product.dto.ProductResponse;
import com.badminton.shop.modules.product.dto.ProductVariantRequest;
import com.badminton.shop.modules.product.dto.ProductVariantResponse;
import com.badminton.shop.modules.product.dto.WishlistItemResponse;
import com.badminton.shop.modules.product.entity.Brand;
import com.badminton.shop.modules.product.entity.Category;
import com.badminton.shop.modules.product.entity.Product;
import com.badminton.shop.modules.product.entity.ProductImage;
import com.badminton.shop.modules.product.entity.ProductVariant;
import com.badminton.shop.modules.product.entity.ProductWishlist;
import com.badminton.shop.modules.product.repository.BrandRepository;
import com.badminton.shop.modules.product.repository.CategoryRepository;
import com.badminton.shop.modules.product.repository.ProductRepository;
import com.badminton.shop.modules.product.repository.ProductVariantRepository;
import com.badminton.shop.modules.product.repository.ProductWishlistRepository;
import com.badminton.shop.modules.review.dto.response.ReviewResponse;
import com.badminton.shop.modules.review.entity.Review;
import com.badminton.shop.modules.review.repository.ReviewRepository;
import com.badminton.shop.modules.product.service.ProductService;
import com.badminton.shop.modules.search.event.ProductSearchSyncAction;
import com.badminton.shop.modules.search.event.ProductSearchSyncEvent;
import com.badminton.shop.utils.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final String CACHE_FEATURED = "productFeatured";
    private static final String CACHE_NEWEST = "productNewest";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductWishlistRepository productWishlistRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final ReviewRepository reviewRepository;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;

    // ===== Public APIs =====

    @Override
    @Transactional(readOnly = true)
    public Page<ProductListResponse> getPublicProducts(
            String category, String brand,
            BigDecimal minPrice, BigDecimal maxPrice,
            String keyword, Pageable pageable) {

        Page<Product> products = productRepository.findAllPublicProducts(
                category, brand, minPrice, maxPrice, keyword, pageable);

        return products.map(this::mapToListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        return productRepository.existsBySlug(slug);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getPublicProductById(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm với id: " + id));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_FEATURED, key = "'limit:' + #limit")
    public List<ProductListResponse> getFeaturedProducts(int limit) {
        int safeLimit = normalizeLimit(limit);
        return productRepository.findFeaturedProducts(PageRequest.of(0, safeLimit)).stream()
                .map(this::mapToListResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_NEWEST, key = "'limit:' + #limit")
    public List<ProductListResponse> getNewestProducts(int limit) {
        int safeLimit = normalizeLimit(limit);
        return productRepository.findNewestProducts(PageRequest.of(0, safeLimit)).stream()
                .map(this::mapToListResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCompareResponse compareVariants(List<Long> variantIds) {
        List<Long> sanitizedVariantIds = variantIds == null
                ? List.of()
                : variantIds.stream().filter(java.util.Objects::nonNull).toList();

        if (sanitizedVariantIds.size() != 2) {
            throw new IllegalArgumentException("Vui lòng chọn đúng 2 variant để so sánh");
        }
        if (sanitizedVariantIds.get(0).equals(sanitizedVariantIds.get(1))) {
            throw new IllegalArgumentException("2 variant so sánh phải khác nhau");
        }

        List<ProductVariant> variants = productVariantRepository.findAllById(sanitizedVariantIds);
        if (variants.size() != 2) {
            throw new ResourceNotFoundException("Không tìm thấy đủ 2 variant để so sánh");
        }

        Map<Long, ProductVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        List<ProductCompareItemResponse> items = new ArrayList<>();
        for (Long variantId : sanitizedVariantIds) {
            ProductVariant variant = variantMap.get(variantId);
            if (variant == null) {
                throw new ResourceNotFoundException("Không tìm thấy variant với id: " + variantId);
            }

            Product product = variant.getProduct();
            if (product == null || !Boolean.TRUE.equals(product.getIsActive())) {
                throw new IllegalArgumentException("Variant thuộc sản phẩm không khả dụng để so sánh");
            }

            items.add(ProductCompareItemResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .slug(product.getSlug())
                    .thumbnailUrl(product.getThumbnailUrl())
                    .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                    .variantId(variant.getId())
                    .sku(variant.getSku())
                    .price(variant.getPrice())
                    .weight(variant.getWeight())
                    .gripSize(variant.getGripSize())
                    .stiffness(variant.getStiffness())
                    .balancePoint(variant.getBalancePoint())
                    .build());
        }

        return ProductCompareResponse.builder().items(items).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getMyWishlist(String principalName) {
        User user = findUserByPrincipal(principalName);
        return productWishlistRepository.findAllByUserIdWithProduct(user.getId())
                .stream()
                .map(this::mapToWishlistItem)
                .toList();
    }

    @Override
    public WishlistItemResponse addToWishlist(String principalName, Long productId) {
        User user = findUserByPrincipal(principalName);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + productId));

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new IllegalArgumentException("Chỉ có thể thêm sản phẩm đang hoạt động vào yêu thích");
        }

        ProductWishlist wishlist = productWishlistRepository.findByUserIdAndProductId(user.getId(), productId)
                .orElseGet(() -> productWishlistRepository.save(ProductWishlist.builder()
                        .user(user)
                        .product(product)
                        .build()));

        return mapToWishlistItem(wishlist);
    }

    @Override
    public void removeFromWishlist(String principalName, Long productId) {
        User user = findUserByPrincipal(principalName);
        productWishlistRepository.findByUserIdAndProductId(user.getId(), productId)
                .ifPresent(productWishlistRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(String principalName, Long productId) {
        User user = findUserByPrincipal(principalName);
        return productWishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
    }

    // ===== Admin APIs =====

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_FEATURED, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NEWEST, allEntries = true)
        })
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy danh mục với id: " + request.getCategoryId()));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thương hiệu với id: " + request.getBrandId()));

        String slug = generateUniqueSlug(request.getName());

        Product product = Product.builder()
            .name(request.getName())
            .slug(slug)
            .shortDescription(request.getShortDescription())
            .description(request.getDescription())
            .basePrice(request.getBasePrice())
            .category(category)
            .brand(brand)
            .build();

        Product saved = productRepository.save(product);
        publishProductSearchSyncEvent(saved.getId(), ProductSearchSyncAction.UPSERT);
        return mapToResponse(saved);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_FEATURED, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NEWEST, allEntries = true)
        })
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm với id: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy danh mục với id: " + request.getCategoryId()));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thương hiệu với id: " + request.getBrandId()));

        // Chỉ regenerate slug nếu tên thay đổi
        if (!product.getName().equals(request.getName())) {
            product.setSlug(generateUniqueSlug(request.getName()));
        }

        product.setName(request.getName());
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setCategory(category);
        product.setBrand(brand);

        Product saved = productRepository.save(product);
        publishProductSearchSyncEvent(saved.getId(), ProductSearchSyncAction.UPSERT);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getProductVariants(Long productId) {
        Product product = findProductOrThrow(productId);
        return mapToVariantResponses(product);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_FEATURED, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NEWEST, allEntries = true)
        })
    public ProductVariantResponse createProductVariant(Long productId, ProductVariantRequest request) {
        Product product = findProductOrThrow(productId);
        ProductVariant variant = ProductVariant.builder()
                .sku(request.getSku())
                .weight(request.getWeight())
                .gripSize(request.getGripSize())
                .stiffness(request.getStiffness())
                .balancePoint(request.getBalancePoint())
                .size(request.getSize())
                .color(request.getColor())
                .price(request.getPrice())
                .originalPrice(request.getPrice())
                .stock(request.getStock())
            .shippingWeightGrams(request.getShippingWeightGrams())
            .shippingLengthCm(request.getShippingLengthCm())
            .shippingWidthCm(request.getShippingWidthCm())
            .shippingHeightCm(request.getShippingHeightCm())
                .build();
        product.addProductVariant(variant);
        productRepository.save(product);
        return mapToVariantResponse(variant);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_FEATURED, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NEWEST, allEntries = true)
        })
    public ProductVariantResponse updateProductVariant(Long productId, Long variantId, ProductVariantRequest request) {
        Product product = findProductOrThrow(productId);
        ProductVariant variant = findProductVariant(product, variantId);
        variant.setSku(request.getSku());
        variant.setWeight(request.getWeight());
        variant.setGripSize(request.getGripSize());
        variant.setStiffness(request.getStiffness());
        variant.setBalancePoint(request.getBalancePoint());
        variant.setSize(request.getSize());
        variant.setColor(request.getColor());
        variant.setPrice(request.getPrice());
        variant.setOriginalPrice(request.getPrice());
        variant.setStock(request.getStock());
        variant.setShippingWeightGrams(request.getShippingWeightGrams());
        variant.setShippingLengthCm(request.getShippingLengthCm());
        variant.setShippingWidthCm(request.getShippingWidthCm());
        variant.setShippingHeightCm(request.getShippingHeightCm());
        productRepository.save(product);
        return mapToVariantResponse(variant);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_FEATURED, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NEWEST, allEntries = true)
        })
    public void deleteProductVariant(Long productId, Long variantId) {
        Product product = findProductOrThrow(productId);
        ProductVariant variant = findProductVariant(product, variantId);
        cartItemRepository.deleteAllByVariantIdIn(Collections.singletonList(variantId));
        product.removeProductVariant(variant);
        productRepository.save(product);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_FEATURED, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NEWEST, allEntries = true)
        })
    public ProductResponse uploadThumbnail(Long id, MultipartFile file) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm với id: " + id));

        if (product.getThumbnailUrl() != null && !product.getThumbnailUrl().isEmpty()) {
            s3Service.deleteFile(product.getThumbnailUrl());
        }

        String fileName = product.getSlug() + "-" + UUID.randomUUID();
        String thumbnailUrl = s3Service.uploadFile("products", fileName, file);

        product.setThumbnailUrl(thumbnailUrl);
        Product saved = productRepository.save(product);
        publishProductSearchSyncEvent(saved.getId(), ProductSearchSyncAction.UPSERT);
        return mapToResponse(saved);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_FEATURED, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NEWEST, allEntries = true)
        })
    public ProductResponse toggleStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm với id: " + id));

        product.setIsActive(!product.getIsActive());
        Product saved = productRepository.save(product);
        publishProductSearchSyncEvent(saved.getId(), ProductSearchSyncAction.UPSERT);
        return mapToResponse(saved);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_FEATURED, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NEWEST, allEntries = true)
        })
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm với id: " + id));

        // 1. Xóa cart items chứa các variant của sản phẩm này
        List<Long> variantIds = product.getVariants().stream()
                .map(ProductVariant::getId)
                .collect(Collectors.toList());

        if (!variantIds.isEmpty()) {
            cartItemRepository.deleteAllByVariantIdIn(variantIds);
            log.info("Đã xóa cart items chứa {} variants của sản phẩm '{}'",
                    variantIds.size(), product.getName());
        }

        // 2. Soft-delete tất cả variants
        for (ProductVariant variant : product.getVariants()) {
            variant.setIsDeleted(true);
        }

        for (ProductImage image : product.getProductImages()) {
            deleteFileIfExists(image.getImageUrl());
        }

        // 3. Soft-delete sản phẩm (via @SQLDelete)
        productRepository.delete(product);
        publishProductSearchSyncEvent(id, ProductSearchSyncAction.DELETE);
        log.info("Đã xóa mềm sản phẩm '{}' và {} variants",
                product.getName(), variantIds.size());
    }

    // ===== Helper methods =====

    private String generateUniqueSlug(String name) {
        String baseSlug = generateSlug(name);

        // Nếu slug chưa tồn tại → dùng luôn
        if (!productRepository.existsBySlug(baseSlug)) {
            return baseSlug;
        }

        // Nếu trùng → thêm suffix random
        String uniqueSlug;
        do {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            uniqueSlug = baseSlug + "-" + suffix;
        } while (productRepository.existsBySlug(uniqueSlug));

        return uniqueSlug;
    }

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

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> getProductImages(Long productId) {
        Product product = findProductOrThrow(productId);
        return mapToImageResponses(product);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_FEATURED, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NEWEST, allEntries = true)
        })
    public ProductImageResponse uploadProductImage(Long productId, ProductImageRequest request, MultipartFile file) {
        Product product = findProductOrThrow(productId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Ảnh sản phẩm không được để trống");
        }

        String sanitizedColor = request.getColor().trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        if (sanitizedColor.isBlank()) {
            sanitizedColor = "image";
        }

        String fileName = String.format("%s/%s-%s", product.getSlug(), sanitizedColor, UUID.randomUUID());
        String imageUrl = s3Service.uploadFile("products", fileName, file);

        ProductImage image = ProductImage.builder()
                .imageUrl(imageUrl)
                .color(request.getColor())
                .isMain(Boolean.TRUE.equals(request.getIsMain()))
                .build();
        product.addProductImage(image);
        productRepository.save(product);
        return mapToImageResponse(image);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_FEATURED, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NEWEST, allEntries = true)
        })
    public ProductImageResponse updateProductImage(Long productId, Long imageId, ProductImageRequest request) {
        Product product = findProductOrThrow(productId);
        ProductImage image = findProductImage(product, imageId);
        image.setColor(request.getColor());
        image.setIsMain(Boolean.TRUE.equals(request.getIsMain()));
        productRepository.save(product);
        return mapToImageResponse(image);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_FEATURED, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NEWEST, allEntries = true)
        })
    public void deleteProductImage(Long productId, Long imageId) {
        Product product = findProductOrThrow(productId);
        ProductImage image = findProductImage(product, imageId);
        product.removeProductImage(image);
        productRepository.save(product);
        deleteFileIfExists(image.getImageUrl());
    }

    private ProductResponse mapToResponse(Product product) {
        List<ReviewResponse> latestReviews = reviewRepository.findTop3ByProductIdOrderByCreatedAtDesc(product.getId())
            .stream()
            .map(this::mapToReviewResponse)
            .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .description(product.getDescription())
                .thumbnailUrl(product.getThumbnailUrl())
                .basePrice(product.getBasePrice())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .categorySlug(product.getCategory() != null ? product.getCategory().getSlug() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .brandSlug(product.getBrand() != null ? product.getBrand().getSlug() : null)
                .productImages(mapToImageResponses(product))
                .productVariants(mapToVariantResponses(product))
                .latestReviews(latestReviews)
                .build();
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .username(review.getUser() != null ? review.getUser().getUsername() : null)
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .orderItemId(review.getOrderItem() != null ? review.getOrderItem().getId() : null)
                .build();
    }

    private ProductListResponse mapToListResponse(Product product) {
        return ProductListResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .thumbnailUrl(product.getThumbnailUrl())
                .basePrice(product.getBasePrice())
                .rate(resolveProductRate(product.getId()))
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .build();
    }

    private Double resolveProductRate(Long productId) {
        Double avgRating = reviewRepository.averageRatingByProductId(productId);
        if (avgRating == null) {
            return 0.0d;
        }
        return Math.round(avgRating * 100.0d) / 100.0d;
    }

    private List<ProductImageResponse> mapToImageResponses(Product product) {
        return product.getProductImages().stream()
                .map(this::mapToImageResponse)
                .collect(Collectors.toList());
    }

    private ProductImageResponse mapToImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .color(image.getColor())
                .isMain(image.getIsMain())
                .build();
    }

        private List<ProductVariantResponse> mapToVariantResponses(Product product) {
        return product.getVariants().stream()
            .map(this::mapToVariantResponse)
            .collect(Collectors.toList());
        }

        private ProductVariantResponse mapToVariantResponse(ProductVariant variant) {
        return ProductVariantResponse.builder()
            .id(variant.getId())
            .sku(variant.getSku())
                .weight(variant.getWeight())
                .gripSize(variant.getGripSize())
                .stiffness(variant.getStiffness())
                .balancePoint(variant.getBalancePoint())
            .size(variant.getSize())
            .color(variant.getColor())
            .price(variant.getPrice())
            .stock(variant.getStock())
            .shippingWeightGrams(variant.getShippingWeightGrams())
            .shippingLengthCm(variant.getShippingLengthCm())
            .shippingWidthCm(variant.getShippingWidthCm())
            .shippingHeightCm(variant.getShippingHeightCm())
            .build();
        }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm với id: " + productId));
    }

    private ProductImage findProductImage(Product product, Long imageId) {
        return product.getProductImages().stream()
                .filter(image -> image.getId() != null && image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy ảnh sản phẩm với id: " + imageId));
    }

        private ProductVariant findProductVariant(Product product, Long variantId) {
        return product.getVariants().stream()
            .filter(variant -> variant.getId() != null && variant.getId().equals(variantId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                "Không tìm thấy variant với id: " + variantId));
        }

        private WishlistItemResponse mapToWishlistItem(ProductWishlist wishlist) {
            Product product = wishlist.getProduct();
            return WishlistItemResponse.builder()
                    .wishlistId(wishlist.getId())
                    .productId(product != null ? product.getId() : null)
                    .productName(product != null ? product.getName() : null)
                    .slug(product != null ? product.getSlug() : null)
                    .thumbnailUrl(product != null ? product.getThumbnailUrl() : null)
                    .basePrice(product != null ? product.getBasePrice() : null)
                    .brandName(product != null && product.getBrand() != null ? product.getBrand().getName() : null)
                    .categoryName(product != null && product.getCategory() != null ? product.getCategory().getName() : null)
                    .addedAt(wishlist.getCreatedAt())
                    .build();
        }

        private User findUserByPrincipal(String principalName) {
            return userRepository.findByEmail(principalName)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + principalName));
        }

    private void deleteFileIfExists(String url) {
        if (url != null && !url.isBlank()) {
            s3Service.deleteFile(url);
        }
    }

    private void publishProductSearchSyncEvent(Long productId, ProductSearchSyncAction action) {
        eventPublisher.publishEvent(new ProductSearchSyncEvent(productId, action));
    }

    private int normalizeLimit(int limit) {
        return Math.min(Math.max(limit, 1), 50);
    }
}
