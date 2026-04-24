# Order + Cart Module

## A. CART MODULE
Base path: /api/cart

### 1) getMyCart
- Method name: getMyCart
- Endpoint: GET /api/cart
- Chuc nang: Lay gio hang hien tai.
- Auth: Bearer User
- Response data: CartResponse
- Code: 200, 401

### 2) addItem
- Method name: addItem
- Endpoint: POST /api/cart/items
- Chuc nang: Them variant vao gio.
- Auth: Bearer User
- Request body:
  - variantId: long
  - quantity: int
- Response data: CartResponse
- Code: 200, 400, 401, 404

### 3) updateItemQuantity
- Method name: updateItemQuantity
- Endpoint: PUT /api/cart/items/{variantId}
- Chuc nang: Cap nhat so luong item.
- Auth: Bearer User
- Request body:
  - quantity: int
- Response data: CartResponse
- Code: 200, 400, 401, 404

### 4) removeItem
- Method name: removeItem
- Endpoint: DELETE /api/cart/items/{variantId}
- Chuc nang: Xoa item khoi gio.
- Auth: Bearer User
- Response data: CartResponse
- Code: 200, 401, 404

### 5) clearCart
- Method name: clearCart
- Endpoint: DELETE /api/cart
- Chuc nang: Xoa toan bo gio.
- Auth: Bearer User
- Response data: CartResponse
- Code: 200, 401

---

## B. ORDER MODULE
Base path: /api/orders

### 1) getCheckoutContext
- Method name: getCheckoutContext
- Endpoint: GET /api/orders/checkout-context
- Chuc nang: Lay dia chi da luu + default address cho checkout.
- Auth: Bearer User
- Response data: CheckoutContextResponse
- Code: 200, 401

### 2) previewOrder
- Method name: previewOrder
- Endpoint: POST /api/orders/preview
- Chuc nang: Preview tong tien don (itemsAmount, shippingFee, totalAmount).
- Auth: Bearer User
- Request body: CreateOrderRequest
  - items[] { variantId, quantity }
  - addressId (optional)
  - voucherCode (optional)
  - receiverName, receiverPhone, shippingAddress (fallback)
  - orderNote
  - paymentMethod
- Response data: OrderPreviewResponse
- Code: 200, 400, 401
- Error:
  - Khong map duoc dia chi GHN
  - Het hang / voucher invalid

### 3) purchase
- Method name: purchase
- Endpoint: POST /api/orders
- Chuc nang: Dat don chinh thuc.
- Auth: Bearer User
- Request body: CreateOrderRequest
- Response data: OrderResponse
  - co the chua paymentUrl neu paymentMethod = VNPAY
  - shippingCode, shippingProvider, shippingExpectedDeliveryAt
- Code: 200, 400, 401

### 4) getMyOrders
- Method name: getMyOrders
- Endpoint: GET /api/orders/my
- Chuc nang: Lay lich su don cua user.
- Auth: Bearer User
- Query: page, size
- Response data: Page<OrderResponse>
- Code: 200, 401

### 5) vnpayReturn
- Method name: vnpayReturn
- Endpoint: GET /api/orders/vnpay-return
- Chuc nang: Frontend endpoint sau redirect VNPAY.
- Auth: Public
- Query: cac tham so vnp_*
- Response data: Map<String, String>
  - orderCode, paymentStatus, orderStatus, responseCode
- Code: 200

### 6) vnpayIpn
- Method name: vnpayIpn
- Endpoint: GET /api/orders/vnpay-ipn
- Chuc nang: Backend IPN webhook tu VNPAY.
- Auth: Public (VNPAY server)
- Query: cac tham so vnp_*
- Response body: JSON chuan VNPAY
  - RspCode, Message
- Code: 200

### 7) cancelOrder
- Method name: cancelOrder
- Endpoint: POST /api/orders/{orderCode}/cancel
- Chuc nang: Khach huy don.
- Auth: Bearer User
- Request body:
  - reason
- Response data: OrderResponse
- Code: 200, 400, 401, 404

### 8) confirmCodOrder
- Method name: confirmCodOrder
- Endpoint: POST /api/orders/admin/{orderCode}/confirm-cod
- Chuc nang: Admin xac nhan don COD.
- Auth: ADMIN
- Query: note (optional)
- Response data: OrderResponse
- Code: 200, 401, 403, 404

### 9) createReturnRequest
- Method name: createReturnRequest
- Endpoint: POST /api/orders/{orderCode}/returns
- Chuc nang: Tao yeu cau tra hang/hoan tien.
- Auth: Bearer User
- Request body: CreateReturnRequest
  - reason, refundMethod, bankAccountName, bankAccountNumber, bankName
  - evidenceUrls[]
  - items[] { orderItemId, quantity }
- Response data: ReturnRequestResponse
- Code: 200, 400, 401, 404

### 10) approveReturnRequest
- Method name: approveReturnRequest
- Endpoint: POST /api/orders/admin/returns/{returnRequestId}/approve
- Chuc nang: Admin duyet yeu cau tra hang.
- Auth: ADMIN
- Query: note (optional)
- Response data: ReturnRequestResponse
- Code: 200, 401, 403, 404

### 11) rejectReturnRequest
- Method name: rejectReturnRequest
- Endpoint: POST /api/orders/admin/returns/{returnRequestId}/reject
- Chuc nang: Admin tu choi yeu cau tra hang.
- Auth: ADMIN
- Query: note (required)
- Response data: ReturnRequestResponse
- Code: 200, 400, 401, 403, 404

### 12) receiveReturnedItems
- Method name: receiveReturnedItems
- Endpoint: POST /api/orders/admin/returns/{returnRequestId}/receive
- Chuc nang: Kho tiep nhan hang hoan.
- Auth: ADMIN
- Request body: ReceiveReturnRequest
  - note
  - items[] { orderItemId, quantity, action: RESTOCK|SCRAP }
- Response data: ReturnRequestResponse
- Code: 200, 400, 401, 403, 404

### 13) markReturnRefunded
- Method name: markReturnRefunded
- Endpoint: POST /api/orders/admin/returns/{returnRequestId}/refund
- Chuc nang: Danh dau da hoan tien.
- Auth: ADMIN
- Query: note (optional)
- Response data: ReturnRequestResponse
- Code: 200, 401, 403, 404

## Gia tri paymentMethod
- COD
- VNPAY
- MOMO
- CREDIT_CARD
