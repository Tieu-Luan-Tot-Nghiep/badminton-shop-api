# Promotion + Review + Membership Module

## A. PROMOTION MODULE
Base path: /api/promotions

### 1) createPromotion
- Method name: createPromotion
- Endpoint: POST /api/promotions
- Chuc nang: Tao voucher moi.
- Auth: ADMIN
- Request body: PromotionRequest
  - code, discountType, discountValue, minOrderValue
  - maxDiscountAmount, maxUsage
  - startDate, expiryDate, isActive
- Response data: PromotionResponse
- Code: 200, 400, 401, 403

### 2) updatePromotion
- Method name: updatePromotion
- Endpoint: PUT /api/promotions/{id}
- Chuc nang: Cap nhat voucher.
- Auth: ADMIN
- Request body: PromotionRequest
- Response data: PromotionResponse
- Code: 200, 400, 401, 403, 404

### 3) setPromotionActive
- Method name: setPromotionActive
- Endpoint: PATCH /api/promotions/{id}/active?active=true|false
- Chuc nang: Bat/tat voucher.
- Auth: ADMIN
- Response data: PromotionResponse
- Code: 200, 401, 403, 404

### 4) getPromotions
- Method name: getPromotions
- Endpoint: GET /api/promotions
- Chuc nang: Danh sach voucher co phan trang.
- Auth: ADMIN
- Query: page, size, activeOnly
- Response data: Page<PromotionResponse>
- Code: 200, 401, 403

### 5) getPromotionByCode
- Method name: getPromotionByCode
- Endpoint: GET /api/promotions/{code}
- Chuc nang: Lay thong tin voucher theo code.
- Auth: Public
- Response data: PromotionResponse
- Code: 200, 404

### 6) validatePromotion
- Method name: validatePromotion
- Endpoint: POST /api/promotions/validate
- Chuc nang: Validate voucher cho checkout.
- Auth: Public
- Request body:
  - voucherCode
  - itemsAmount
  - shippingFee
- Response data: PromotionValidationResponse
  - valid, message, discountAmount, finalShippingFee, finalTotalAmount
- Code: 200, 400, 404
- Error:
  - Voucher het han
  - Vuot maxUsage
  - Khong dat minOrderValue

---

## B. REVIEW MODULE
Base path: /api/reviews

### 1) createReview
- Method name: createReview
- Endpoint: POST /api/reviews
- Chuc nang: Tao danh gia san pham tu order item.
- Auth: Bearer User
- Request body:
  - orderItemId
  - rating
  - comment
- Response data: ReviewResponse
- Code: 200, 400, 401, 404
- Rule:
  - Chi owner moi duoc review
  - Order phai DELIVERED
  - Moi order item chi review 1 lan

### 2) updateReview
- Method name: updateReview
- Endpoint: PUT /api/reviews/{id}
- Chuc nang: Cap nhat review cua chinh user.
- Auth: Bearer User
- Request body:
  - rating
  - comment
- Response data: ReviewResponse
- Code: 200, 400, 401, 404

### 3) deleteReview
- Method name: deleteReview
- Endpoint: DELETE /api/reviews/{id}
- Chuc nang: Xoa review.
- Auth: Bearer User
- Response data: null
- Code: 200, 401, 404

### 4) getReviewById
- Method name: getReviewById
- Endpoint: GET /api/reviews/{id}
- Chuc nang: Chi tiet review.
- Auth: Public
- Response data: ReviewResponse
- Code: 200, 404

### 5) getReviewsByProduct
- Method name: getReviewsByProduct
- Endpoint: GET /api/reviews/products/{productId}
- Chuc nang: Danh sach review theo product.
- Auth: Public
- Query: page, size
- Response data: Page<ReviewResponse>
- Code: 200, 404

### 6) getReviewSummaryByProduct
- Method name: getReviewSummaryByProduct
- Endpoint: GET /api/reviews/products/{productId}/summary
- Chuc nang: Tong hop diem trung binh + tong review.
- Auth: Public
- Response data: ReviewSummaryResponse
- Code: 200, 404

### 7) getMyReviews
- Method name: getMyReviews
- Endpoint: GET /api/reviews/my
- Chuc nang: Danh sach review cua user hien tai.
- Auth: Bearer User
- Query: page, size
- Response data: Page<ReviewResponse>
- Code: 200, 401

---

## C. MEMBERSHIP MODULE
Base path: /api/memberships

### 1) getMyMembership
- Method name: getMyMembership
- Endpoint: GET /api/memberships/me
- Chuc nang: Lay thong tin hang thanh vien cua user.
- Auth: Bearer User
- Response data: UserMembershipResponse
  - userId, email, tier, currentPoints, totalPoints, pointsToNextTier, nextTierName
- Code: 200, 401, 404

### 2) getMyPointHistory
- Method name: getMyPointHistory
- Endpoint: GET /api/memberships/me/history
- Chuc nang: Lay lich su bien dong diem.
- Auth: Bearer User
- Response data: List<PointHistoryResponse>
- Code: 200, 401, 404
