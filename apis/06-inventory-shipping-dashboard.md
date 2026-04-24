# Inventory + Shipping + Dashboard Module

## A. INVENTORY MODULE
Base path: /api/inventory

### 1) checkAvailability
- Method name: checkAvailability
- Endpoint: POST /api/inventory/system/check-availability
- Chuc nang: Check ton kha dung theo danh sach variant.
- Auth: Internal/System (khuyen nghi protect bang service auth/network)
- Request body: AvailabilityCheckRequest
  - items[] { variantId, quantity }
- Response data: AvailabilityCheckResponse
- Code: 200, 400

### 2) reserveInventory
- Method name: reserveInventory
- Endpoint: POST /api/inventory/system/reserve
- Chuc nang: Giu cho ton kho khi don pending.
- Auth: Internal/System
- Request body: SystemInventoryRequest
  - referenceCode, note, items[]
- Response data: SystemInventoryResponse
- Code: 200, 400

### 3) commitInventory
- Method name: commitInventory
- Endpoint: POST /api/inventory/system/commit
- Chuc nang: Tru kho sau khi thanh toan thanh cong.
- Auth: Internal/System
- Request body: SystemInventoryRequest
- Response data: SystemInventoryResponse
- Code: 200, 400

### 4) rollbackInventory
- Method name: rollbackInventory
- Endpoint: POST /api/inventory/system/rollback
- Chuc nang: Tra reserved ve available khi huy don/timeout.
- Auth: Internal/System
- Request body: SystemInventoryRequest
- Response data: SystemInventoryResponse
- Code: 200, 400

### 5) stockIn
- Method name: stockIn
- Endpoint: POST /api/inventory/admin/stock-in
- Chuc nang: Nhap kho.
- Auth: ADMIN
- Request body: StockInRequest
  - variantId, quantity, unitCost, note
- Response data: InventorySnapshotResponse
- Code: 200, 400, 401, 403

### 6) stockOut
- Method name: stockOut
- Endpoint: POST /api/inventory/admin/stock-out
- Chuc nang: Xuat kho.
- Auth: ADMIN
- Request body: StockOutRequest
- Response data: InventorySnapshotResponse
- Code: 200, 400, 401, 403

### 7) stocktakeAdjustment
- Method name: stocktakeAdjustment
- Endpoint: POST /api/inventory/admin/stocktake-adjustment
- Chuc nang: Dieu chinh ton theo kiem ke.
- Auth: ADMIN
- Request body: StocktakeAdjustmentRequest
- Response data: InventorySnapshotResponse
- Code: 200, 400, 401, 403

### 8) getLedger
- Method name: getLedger
- Endpoint: GET /api/inventory/admin/ledger/{variantId}
- Chuc nang: Lich su bien dong ton kho.
- Auth: ADMIN
- Query: page, size
- Response data: InventoryLedgerResponse
- Code: 200, 401, 403, 404

### 9) getLowStockAlerts
- Method name: getLowStockAlerts
- Endpoint: GET /api/inventory/admin/low-stock
- Chuc nang: Canh bao ton thap.
- Auth: ADMIN
- Query: threshold
- Response data: List<InventorySnapshotResponse>
- Code: 200, 401, 403

---

## B. SHIPPING MODULE
Base path: /api/shipping

### 1) getProvinces
- Method name: getProvinces
- Endpoint: GET /api/shipping/provinces
- Chuc nang: Lay danh muc tinh/thanh GHN.
- Auth: Bearer User
- Response data: List<ProvinceResponse>
- Code: 200, 401

### 2) getDistricts
- Method name: getDistricts
- Endpoint: GET /api/shipping/provinces/{provinceId}/districts
- Chuc nang: Lay danh muc quan/huyen theo tinh.
- Auth: Bearer User
- Response data: List<DistrictResponse>
- Code: 200, 401, 404

### 3) getWards
- Method name: getWards
- Endpoint: GET /api/shipping/districts/{districtId}/wards
- Chuc nang: Lay danh muc phuong/xa theo quan.
- Auth: Bearer User
- Response data: List<WardResponse>
- Code: 200, 401, 404

### 4) getShippingDetail
- Method name: getShippingDetail
- Endpoint: GET /api/shipping/orders/{shippingCode}
- Chuc nang: Lay trang thai van don GHN.
- Auth: Bearer User
- Response data: ShippingOrderResponse
- Code: 200, 401, 404

### 5) receiveWebhook
- Method name: receiveWebhook
- Endpoint: POST /api/shipping/webhook/ghn
- Chuc nang: Nhan callback GHN va day event cap nhat trang thai don.
- Auth: Public hoac X-Webhook-Token (neu cau hinh)
- Request header:
  - X-Webhook-Token (optional, required khi server co callback-auth-token)
- Request body: ShippingWebhookRequest
  - OrderCode, ClientOrderCode, Status, ShopID, ...
- Response data: null
- Code:
  - 200: webhook accepted
  - 401: webhook token invalid

---

## C. DASHBOARD MODULE
Base path: /api/admin/dashboard
Auth: ADMIN

### 1) getRevenue
- Method name: getRevenue
- Endpoint: GET /api/admin/dashboard/revenue
- Chuc nang: Bao cao doanh thu theo chu ky.
- Query:
  - startDate (yyyy-MM-dd)
  - endDate (yyyy-MM-dd)
  - groupBy (DAY|MONTH|YEAR)
- Response data: List<RevenueReportResponse>
  - period, totalRevenue, totalOrders
- Code: 200, 401, 403

### 2) getRevenueByBrand
- Method name: getRevenueByBrand
- Endpoint: GET /api/admin/dashboard/revenue-by-brand
- Chuc nang: Doanh thu theo thuong hieu.
- Query: startDate, endDate
- Response data: List<BrandRevenueResponse>
  - brandName, totalRevenue, itemsSold
- Code: 200, 401, 403

### 3) getTopSelling
- Method name: getTopSelling
- Endpoint: GET /api/admin/dashboard/top-selling
- Chuc nang: Top san pham ban chay.
- Query: startDate, endDate, limit
- Response data: List<TopSellingResponse>
  - productId, productName, slug, thumbnailUrl, totalQuantitySold, totalRevenue
- Code: 200, 401, 403

### 4) getInventoryValue
- Method name: getInventoryValue
- Endpoint: GET /api/admin/dashboard/inventory-value
- Chuc nang: Tong gia tri ton kho.
- Response data: InventoryValueResponse
  - totalStockQuantity, estimatedValue
- Code: 200, 401, 403
