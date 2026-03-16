# Tài liệu Hệ thống: Bảo mật và Cấu hình Cloud

Tài liệu này tổng hợp các tính năng bảo mật và cấu hình dịch vụ đám mây (Redis, RabbitMQ, Neon Postgres) đã triển khai trong project BadmintonShop.

## 1. Quản lý Cấu hình (Configuration Management)
Để đảm bảo an toàn, các thông tin nhạy cảm được quản lý thông qua biến môi trường.
- **File mẫu**: [application-template.properties](file:///d:/TLTN/BE/BadmintonShop/CloudProperties/application-template.properties)
- **Danh sách biến**: [variable-enviroment.txt](file:///d:/TLTN/BE/BadmintonShop/CloudProperties/variable-enviroment.txt)

---

## 2. Hệ thống Giới hạn Request (Rate Limiting)
Sử dụng Redis để theo dõi và giới hạn tần suất gửi request từ client.

### Đăng ký (Registration)
- **Quy tắc**:
    - Mỗi 30 giây chỉ được gửi 1 lần (Cooldown).
    - Tối đa 5 lần thử trong 1 phút trên mỗi địa chỉ IP.
- **Mục đích**: Ngăn chặn spam đăng ký và bảo vệ dịch vụ gửi Email.

### Đăng nhập (Login)
- **Quy tắc**: Tối đa 5 lần đăng nhập mỗi phút.
- **Mục đích**: Hạn chế việc dùng bot tự động thử mật khẩu.

---

## 3. Cơ chế Khóa tài khoản (Account Lockout)
Bảo vệ tài khoản người dùng khỏi các cuộc tấn công Brute-force.
- **Cơ chế**: Nếu nhập sai mật khẩu **5 lần liên tiếp**, hệ thống sẽ khóa định danh đó.
- **Đối tượng khóa**: 
    - Khóa theo **Username**: Ngăn chặn việc thử nhiều mật khẩu cho 1 tài khoản.
    - Khóa theo **Địa chỉ IP**: Ngăn chặn 1 hacker thử nhiều tài khoản khác nhau trên 1 máy.
- **Thời gian khóa**: 15 phút. Sau thời gian này, Redis sẽ tự động xóa key và người dùng có thể thử lại.

---

## 4. Xử lý Bất đồng bộ (Messaging)
Sử dụng RabbitMQ để xử lý các tác vụ tốn thời gian.
- **Luồng xử lý**: 
    1. Người dùng bấm Đăng ký.
    2. API tạo mã token, lưu vào Redis và đẩy tin nhắn vào RabbitMQ.
    3. API phản hồi "Thành công" ngay lập tức cho người dùng.
    4. `EmailConsumer` lấy tin nhắn từ hàng đợi và thực hiện gửi Email xác thực.
- **Lợi ích**: Giảm thời gian phản hồi của API và đảm bảo Email luôn được gửi kể cả khi Server SMTP gặp sự cố tạm thời (nhờ khả năng lưu trữ của Queue).

---

## 5. Caching & Storage (Redis)
Redis được sử dụng cho 3 mục đích chính:
1. Lưu trữ mã xác thực Email (TTL 10 phút).
2. Lưu trữ dữ liệu Rate Limit (TTL biến động).
3. Lưu trữ trạng thái khóa tài khoản và bộ đếm lỗi đăng nhập.

---

## 6. Cơ chế Token Làm mới (Refresh Token)
Đảm bảo trải nghiệm người dùng liền mạch và tăng cường bảo mật.

### Luồng hoạt động:
1. **Đăng nhập/Đăng ký**: Hệ thống trả về cả `accessToken` (1 giờ) và `refreshToken` (7 ngày).
2. **Lưu trữ**: `refreshToken` được lưu trong Redis với key `refresh:{username}`.
3. **Lấy Token mới**: FE gọi API `/api/auth/refresh` kèm theo `refreshToken`. Hệ thống kiểm tra trong Redis, nếu khớp sẽ cấp cặp token mới (Token rotation).
4. **Đăng xuất**: Gọi API `/api/auth/logout` sẽ xóa `refreshToken` tương ứng trong Redis, làm cho token đó không còn giá trị sử dụng.

### Lợi ích:
- **Bảo mật**: Access Token có thời gian sống ngắn. Nếu bị lộ, hacker chỉ có tối đa 1 giờ để lợi dụng.
- **Kiểm soát**: Có thể thu hồi quyền truy cập của người dùng ngay lập tức bằng cách xóa token trong Redis (ví dụ khi phát hiện dấu hiệu hack).

---
*Tài liệu được cập nhật ngày: 15/03/2026*
