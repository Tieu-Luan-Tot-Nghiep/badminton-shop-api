# Frontend Screen Mapping Theo Endpoint

Muc tieu: map endpoint backend sang man hinh frontend de AI Agent co the scaffold UI theo dung luong nghiep vu.

## Quy uoc mapping

- List: man hinh danh sach, table/card, filter, sort, paging.
- Detail: man hinh chi tiet 1 doi tuong.
- Create: form tao moi.
- Update: form cap nhat.
- Workflow: man hinh theo luong nghiep vu nhieu buoc.

## 1) Auth + Profile + Address

### Auth
- Route: /login
  - Type: Create (session)
  - Endpoint: POST /api/auth/login
- Route: /register
  - Type: Create
  - Endpoint: POST /api/auth/register
- Route: /forgot-password
  - Type: Workflow
  - Endpoint: POST /api/auth/forgot-password
- Route: /reset-password
  - Type: Update
  - Endpoint: POST /api/auth/reset-password
- Route: /verify-email
  - Type: Workflow
  - Endpoint: GET /api/auth/verify-email
- Route: /profile
  - Type: Detail + Update
  - Endpoint:
    - GET /api/auth/me
    - PUT /api/auth/profile
    - POST /api/auth/profile/avatar
    - POST /api/auth/change-password
    - DELETE /api/auth/account

### Address book
- Route: /account/addresses
  - Type: List + Create + Update
  - Endpoint:
    - GET /api/addresses
    - POST /api/addresses
    - PUT /api/addresses/{id}
    - PATCH /api/addresses/{id}/default
    - DELETE /api/addresses/{id}
- Route: /account/addresses/{id}
  - Type: Detail
  - Endpoint: GET /api/addresses/{id}

## 2) Product + Category + Brand + Search

### Shop catalog
- Route: /products
  - Type: List
  - Endpoint:
    - GET /api/products
    - GET /api/search/products
    - GET /api/search/products/suggestions
    - GET /api/search/products/trending
- Route: /products/{id}
  - Type: Detail
  - Endpoint:
    - GET /api/products/{id}
    - GET /api/reviews/products/{productId}
    - GET /api/reviews/products/{productId}/summary
    - GET /api/search/products/similar
- Route: /compare
  - Type: Workflow
  - Endpoint: GET /api/products/compare
- Route: /wishlist
  - Type: List + Workflow
  - Endpoint:
    - GET /api/products/wishlist
    - POST /api/products/wishlist/{productId}
    - DELETE /api/products/wishlist/{productId}
    - GET /api/products/wishlist/{productId}/exists

### Search by image
- Route: /search-by-image
  - Type: Workflow
  - Endpoint: POST /api/search/products/by-image

### Admin products
- Route: /admin/products
  - Type: List
  - Endpoint: GET /api/products
- Route: /admin/products/new
  - Type: Create
  - Endpoint: POST /api/products
- Route: /admin/products/{id}/edit
  - Type: Update
  - Endpoint:
    - PUT /api/products/{id}
    - PATCH /api/products/{id}/status
    - POST /api/products/{id}/thumbnail
- Route: /admin/products/{id}/variants
  - Type: List + Create + Update
  - Endpoint:
    - GET /api/products/{productId}/variants
    - POST /api/products/{productId}/variants
    - PUT /api/products/{productId}/variants/{variantId}
    - DELETE /api/products/{productId}/variants/{variantId}
- Route: /admin/products/{id}/images
  - Type: List + Create + Update
  - Endpoint:
    - GET /api/products/{productId}/images
    - POST /api/products/{productId}/images
    - PUT /api/products/{productId}/images/{imageId}
    - DELETE /api/products/{productId}/images/{imageId}

### Admin categories
- Route: /admin/categories
  - Type: List + Create + Update
  - Endpoint:
    - GET /api/categories
    - POST /api/categories
    - PUT /api/categories/{id}
    - DELETE /api/categories/{id}

### Admin brands
- Route: /admin/brands
  - Type: List + Create + Update
  - Endpoint:
    - GET /api/brands
    - POST /api/brands
    - PUT /api/brands/{id}
    - POST /api/brands/{id}/logo
    - DELETE /api/brands/{id}

## 3) Cart + Checkout + Orders + Returns

### Cart
- Route: /cart
  - Type: Detail + Update
  - Endpoint:
    - GET /api/cart
    - POST /api/cart/items
    - PUT /api/cart/items/{variantId}
    - DELETE /api/cart/items/{variantId}
    - DELETE /api/cart

### Checkout
- Route: /checkout
  - Type: Workflow
  - Endpoint:
    - GET /api/orders/checkout-context
    - POST /api/orders/preview
    - POST /api/orders
    - POST /api/promotions/validate
    - GET /api/shipping/provinces
    - GET /api/shipping/provinces/{provinceId}/districts
    - GET /api/shipping/districts/{districtId}/wards

### Payment callback page
- Route: /payment/vnpay-return
  - Type: Workflow
  - Endpoint: GET /api/orders/vnpay-return

### My orders
- Route: /account/orders
  - Type: List
  - Endpoint: GET /api/orders/my
- Route: /account/orders/{orderCode}
  - Type: Detail + Workflow
  - Endpoint:
    - POST /api/orders/{orderCode}/cancel
    - POST /api/orders/{orderCode}/returns

### Return workflow (admin)
- Route: /admin/returns
  - Type: Workflow
  - Endpoint:
    - POST /api/orders/admin/returns/{returnRequestId}/approve
    - POST /api/orders/admin/returns/{returnRequestId}/reject
    - POST /api/orders/admin/returns/{returnRequestId}/receive
    - POST /api/orders/admin/returns/{returnRequestId}/refund

### Order workflow (admin)
- Route: /admin/orders/{orderCode}
  - Type: Workflow
  - Endpoint: POST /api/orders/admin/{orderCode}/confirm-cod

## 4) Promotion + Review + Membership

### Promotions (admin)
- Route: /admin/promotions
  - Type: List + Create + Update
  - Endpoint:
    - GET /api/promotions
    - POST /api/promotions
    - PUT /api/promotions/{id}
    - PATCH /api/promotions/{id}/active
- Route: /admin/promotions/{id}
  - Type: Detail
  - Endpoint: GET /api/promotions/{code}

### Reviews
- Route: /account/reviews
  - Type: List
  - Endpoint: GET /api/reviews/my
- Route: /products/{productId}#reviews
  - Type: List + Create + Update
  - Endpoint:
    - GET /api/reviews/products/{productId}
    - POST /api/reviews
    - PUT /api/reviews/{id}
    - DELETE /api/reviews/{id}

### Membership
- Route: /account/membership
  - Type: Detail
  - Endpoint: GET /api/memberships/me
- Route: /account/membership/history
  - Type: List
  - Endpoint: GET /api/memberships/me/history

## 5) Chat + Chatbot

### Customer support chat
- Route: /support/chat
  - Type: Workflow
  - Endpoint REST:
    - GET /api/chat/rooms/me
    - GET /api/chat/rooms/{roomId}/messages
    - GET /api/chat/unread-count
    - POST /api/chat/rooms/{roomId}/read
    - POST /api/chat/upload
  - STOMP:
    - SEND /app/chat.send
    - SEND /app/chat.read
    - SUB /topic/chat.room.{roomId}
    - SUB /topic/chat.room.{roomId}.read
    - SUB /user/queue/chat.sent
    - SUB /user/queue/chat.unread

### Admin chat inbox
- Route: /admin/chat
  - Type: List + Workflow
  - Endpoint:
    - GET /api/chat/admin/inbox
    - GET /api/chat/rooms/{roomId}/messages
    - POST /api/chat/rooms/{roomId}/read

### Chatbot
- Route: /assistant
  - Type: Workflow
  - Endpoint:
    - POST /api/chatbot/ask
    - GET /api/chatbot/session-state
    - POST /api/chatbot/close-session

## 6) Inventory + Shipping + Dashboard (Admin)

### Inventory
- Route: /admin/inventory
  - Type: List + Workflow
  - Endpoint:
    - GET /api/inventory/admin/low-stock
    - GET /api/inventory/admin/ledger/{variantId}
    - POST /api/inventory/admin/stock-in
    - POST /api/inventory/admin/stock-out
    - POST /api/inventory/admin/stocktake-adjustment

### Shipping tracking
- Route: /account/shipping/{shippingCode}
  - Type: Detail
  - Endpoint: GET /api/shipping/orders/{shippingCode}

### Dashboard
- Route: /admin/dashboard
  - Type: List (analytics widgets/charts)
  - Endpoint:
    - GET /api/admin/dashboard/revenue
    - GET /api/admin/dashboard/revenue-by-brand
    - GET /api/admin/dashboard/top-selling
    - GET /api/admin/dashboard/inventory-value

## 7) Auth Guard va Role Guard de AI generate

- Public routes: /login, /register, /products, /products/{id}, /compare, /search-by-image
- User routes: /cart, /checkout, /account/*, /support/chat, /assistant
- Admin routes: /admin/*

Goi y middleware:
- requireAuth: kiem tra access token.
- requireRole("ADMIN"): chan route admin.
- on401 interceptor: call refresh token va retry request.
