# Tài liệu Tích hợp Social Login & VNPay

Tài liệu này hướng dẫn cách sử dụng và cấu hình cho các chức năng đăng nhập qua mạng xã hội (Google, Facebook) và thanh toán qua VNPay.

## 1. Cấu hình Biến môi trường

Các thông tin nhạy cảm được lưu trong `CloudProperties/variable-enviroment.txt`:

```env
# === Social Login (Facebook & Google) ===
FACEBOOK_CLIENT_ID=...
FACEBOOK_CLIENT_SECRET=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...

# === Payment (VNPay) ===
VNPAY_TMN_CODE=...
VNPAY_HASH_SECRET=...
```

## 2. Luồng Đăng nhập Social (OAuth2)

Hệ thống sử dụng Spring Security OAuth2 Client để xử lý đăng nhập.

### Luồng hoạt động:
1. **Frontend**: Chuyển hướng người dùng đến:
   - Google: `http://localhost:8080/oauth2/authorization/google`
   - Facebook: `http://localhost:8080/oauth2/authorization/facebook`
2. **Backend**: 
   - Xử lý callback từ Provider.
   - `CustomOAuth2UserService` kiểm tra email. Nếu chưa có tài khoản, hệ thống sẽ tự động tạo mới với `AuthProvider` tương ứng.
   - Sau khi thành công, `OAuth2AuthenticationSuccessHandler` sẽ tạo cặp JWT (Access & Refresh Token).
3. **Frontend Callback**: Backend chuyển hướng về `http://localhost:3000/oauth2/redirect?token=...&refreshToken=...`.
   - Frontend cần lấy token từ URL và lưu vào LocalStorage/Cookie.

## 3. Cấu hình VNPay

Thông tin cấu hình VNPay trong `application.properties`:

- `vnpay.tmn-code`: Mã website tại hệ thống VNPay.
- `vnpay.hash-secret`: Chuỗi bí mật dùng để tạo mã hash (Checksum).
- `vnpay.url`: URL thanh toán của VNPay (Sandbox).
- `vnpay.return-url`: URL nhận kết quả thanh toán trả về từ VNPay.

> [!IMPORTANT]
> Địa chỉ `return-url` phải được cấu hình chính xác trên Portal của VNPay để nhận kết quả thanh toán.

---
*Cập nhật ngày: 15/03/2026*
