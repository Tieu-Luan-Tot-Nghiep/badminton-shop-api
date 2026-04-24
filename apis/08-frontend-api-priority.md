# Frontend API Priority (MVP -> Nang Cao)

Muc tieu: xep thu tu trien khai man hinh va endpoint de AI Agent generate theo phase, giam phu thuoc cheo va ra duoc ban chay som.

## Nguyen tac uu tien

- Uu tien flow tao doanh thu som: Browse -> Cart -> Checkout -> Place order.
- Uu tien endpoint on dinh, auth ro rang, payload don gian.
- Day endpoint admin va workflow phuc tap sang phase sau.
- Moi phase co Definition of Done (DoD) de de kiem thu.

## Phase 1 - MVP Commerce Core

### Muc tieu
- User dang ky/dang nhap.
- Xem danh sach + chi tiet san pham.
- Quan ly gio hang.
- Dat don (COD/VNPAY) va xem lich su don.

### Man hinh uu tien
1. /login, /register
2. /products, /products/{id}
3. /cart
4. /checkout
5. /payment/vnpay-return
6. /account/orders

### Endpoint can dung
- Auth:
  - POST /api/auth/register
  - POST /api/auth/login
  - POST /api/auth/refresh
  - POST /api/auth/logout
  - GET /api/auth/me
- Catalog:
  - GET /api/products
  - GET /api/products/{id}
  - GET /api/categories
  - GET /api/brands
- Cart:
  - GET /api/cart
  - POST /api/cart/items
  - PUT /api/cart/items/{variantId}
  - DELETE /api/cart/items/{variantId}
- Checkout/Order:
  - GET /api/orders/checkout-context
  - POST /api/orders/preview
  - POST /api/orders
  - GET /api/orders/my
  - GET /api/orders/vnpay-return
- Shipping master data:
  - GET /api/shipping/provinces
  - GET /api/shipping/provinces/{provinceId}/districts
  - GET /api/shipping/districts/{districtId}/wards

### DoD
- User dat duoc don COD thanh cong.
- User dat duoc don VNPAY va quay ve man hinh ket qua.
- Gio hang cap nhat so luong, tong tien dung.
- Co xu ly 401 + refresh token.

## Phase 2 - Customer Experience

### Muc tieu
- Hoan thien tai khoan ca nhan.
- Tang conversion qua search, wishlist, promotion.
- Tang trust qua review.

### Man hinh uu tien
1. /profile
2. /account/addresses
3. /search-by-image
4. /wishlist
5. /products/{id}#reviews
6. /account/reviews
7. /account/membership, /account/membership/history

### Endpoint can dung
- Profile/Address:
  - PUT /api/auth/profile
  - POST /api/auth/profile/avatar
  - POST /api/auth/change-password
  - GET /api/addresses
  - POST /api/addresses
  - PUT /api/addresses/{id}
  - PATCH /api/addresses/{id}/default
  - DELETE /api/addresses/{id}
- Search:
  - GET /api/search/products
  - GET /api/search/products/suggestions
  - GET /api/search/products/trending
  - POST /api/search/products/by-image
  - GET /api/search/products/similar
- Wishlist/Compare:
  - GET /api/products/wishlist
  - POST /api/products/wishlist/{productId}
  - DELETE /api/products/wishlist/{productId}
  - GET /api/products/compare
- Promotion:
  - POST /api/promotions/validate
- Review:
  - GET /api/reviews/products/{productId}
  - GET /api/reviews/products/{productId}/summary
  - POST /api/reviews
  - PUT /api/reviews/{id}
  - DELETE /api/reviews/{id}
  - GET /api/reviews/my
- Membership:
  - GET /api/memberships/me
  - GET /api/memberships/me/history

### DoD
- User cap nhat profile, so dia chi, dia chi mac dinh.
- Search co filter/sort/suggestion + image search.
- Voucher apply o checkout preview.
- User review duoc item da giao.

## Phase 3 - Support + Post-order Workflow

### Muc tieu
- Ho tro chat realtime.
- Quan ly huy don/tra hang cho user.
- Theo doi van don.

### Man hinh uu tien
1. /support/chat
2. /assistant
3. /account/orders/{orderCode}
4. /account/shipping/{shippingCode}

### Endpoint can dung
- Chat REST:
  - GET /api/chat/rooms/me
  - GET /api/chat/rooms/{roomId}/messages
  - GET /api/chat/unread-count
  - POST /api/chat/rooms/{roomId}/read
  - POST /api/chat/upload
- Chat STOMP:
  - SEND /app/chat.send
  - SEND /app/chat.read
  - SUB /topic/chat.room.{roomId}
  - SUB /topic/chat.room.{roomId}.read
  - SUB /user/queue/chat.sent
  - SUB /user/queue/chat.unread
- Chatbot:
  - POST /api/chatbot/ask
  - GET /api/chatbot/session-state
  - POST /api/chatbot/close-session
- Order lifecycle:
  - POST /api/orders/{orderCode}/cancel
  - POST /api/orders/{orderCode}/returns
  - GET /api/shipping/orders/{shippingCode}

### DoD
- Chat realtime gui/nhan tin nhan hoat dong.
- User tao duoc return request.
- User xem duoc tracking van don.

## Phase 4 - Admin Operations

### Muc tieu
- Van hanh catalog, kho, khuyen mai, dashboard.
- Quan ly inbox chat admin va workflow tra hang.

### Man hinh uu tien
1. /admin/dashboard
2. /admin/products, /admin/products/{id}/edit
3. /admin/categories, /admin/brands
4. /admin/inventory
5. /admin/promotions
6. /admin/chat
7. /admin/returns

### Endpoint can dung
- Dashboard:
  - GET /api/admin/dashboard/revenue
  - GET /api/admin/dashboard/revenue-by-brand
  - GET /api/admin/dashboard/top-selling
  - GET /api/admin/dashboard/inventory-value
- Product admin:
  - POST /api/products
  - PUT /api/products/{id}
  - PATCH /api/products/{id}/status
  - POST /api/products/{id}/thumbnail
  - GET/POST/PUT/DELETE /api/products/{productId}/variants...
  - GET/POST/PUT/DELETE /api/products/{productId}/images...
  - POST /api/search/products/reindex
- Category/Brand admin:
  - POST/PUT/DELETE /api/categories/{id}
  - POST/PUT/DELETE /api/brands/{id}
  - POST /api/brands/{id}/logo
- Inventory admin:
  - POST /api/inventory/admin/stock-in
  - POST /api/inventory/admin/stock-out
  - POST /api/inventory/admin/stocktake-adjustment
  - GET /api/inventory/admin/ledger/{variantId}
  - GET /api/inventory/admin/low-stock
- Promotion admin:
  - GET /api/promotions
  - POST /api/promotions
  - PUT /api/promotions/{id}
  - PATCH /api/promotions/{id}/active
- Chat admin:
  - GET /api/chat/admin/inbox
- Return admin:
  - POST /api/orders/admin/returns/{returnRequestId}/approve
  - POST /api/orders/admin/returns/{returnRequestId}/reject
  - POST /api/orders/admin/returns/{returnRequestId}/receive
  - POST /api/orders/admin/returns/{returnRequestId}/refund
  - POST /api/orders/admin/{orderCode}/confirm-cod

### DoD
- Admin thao tac catalog/kho/promotion day du.
- Dashboard co chart doanh thu + top selling + inventory value.
- Xu ly return tu approve -> refund hoan chinh.

## Phase 5 - Hardening + Optimization

### Muc tieu
- Hoan thien chat luong san pham frontend.
- Toi uu hieu nang, tra loi loi ro rang, theo doi su kien.

### Cong viec
- Tich hop error mapping theo statusCode/message toan he thong.
- Skeleton/loading state + empty state cho tat ca list/detail.
- Retry strategy cho endpoint co tinh on dinh thap.
- Audit UX cho 401/403 + role guard + deep-link.
- Theo doi analytics cho search, cart abandon, checkout drop.

### DoD
- Core Web Vitals dat muc chap nhan.
- Ty le loi UI giam, thong bao loi ro rang.
- Co dashboard metric frontend cho funnel mua hang.

## Goi y thu tu generate cho AI Agent

1. Generate routing + auth guard + API client truoc.
2. Sinh screen theo thu tu phase 1 -> 2 -> 3 -> 4.
3. Moi screen bat buoc co:
   - data model (request/response)
   - loading/error/empty state
   - role guard neu can
4. Moi phase xong thi chay smoke test luong chinh truoc khi sang phase tiep.
