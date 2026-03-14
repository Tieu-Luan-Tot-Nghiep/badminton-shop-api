# 🏸 Badminton Shop API

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-green?style=flat-square&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-green?style=flat-square&logo=springsecurity)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue?style=flat-square&logo=mysql)
![Maven](https://img.shields.io/badge/Maven-3.x-red?style=flat-square&logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

**RESTful API cho hệ thống cửa hàng cầu lông trực tuyến**

</div>

---

## 📖 Giới Thiệu Dự Án

**Badminton Shop API** là một backend REST API được xây dựng bằng **Spring Boot 3**, phục vụ cho hệ thống cửa hàng thương mại điện tử chuyên về cầu lông. Hệ thống cung cấp đầy đủ các tính năng:

- 🛍️ **Quản lý sản phẩm**: Vợt cầu lông, giầy, quần áo, túi, cầu lông, phụ kiện
- 📂 **Quản lý danh mục** sản phẩm
- 👤 **Xác thực & phân quyền** người dùng (JWT)
- 🛒 **Giỏ hàng** thời gian thực
- 📦 **Quản lý đơn hàng** với quy trình trạng thái đầy đủ
- 💳 **Thanh toán** đa phương thức (COD, MoMo, VNPay, ZaloPay, chuyển khoản)
- 🔐 **Phân quyền** Admin / Customer

---

## 🛠️ Tech Stack

| Thành phần | Công nghệ |
|---|---|
| **Ngôn ngữ** | Java 17 |
| **Framework** | Spring Boot 3.2.3 |
| **Bảo mật** | Spring Security 6 + JWT (jjwt 0.12.5) |
| **Persistence** | Spring Data JPA + Hibernate 6 |
| **Database** | MySQL 8.x (H2 cho môi trường test) |
| **Build Tool** | Apache Maven 3 |
| **Tài liệu API** | SpringDoc OpenAPI 3 (Swagger UI) |
| **Logging** | SLF4J + Logback |
| **Validation** | Jakarta Bean Validation |
| **Lombok** | Giảm boilerplate code |
| **Monitoring** | Spring Boot Actuator |

---

## 📁 Cấu Trúc Dự Án

Dự án áp dụng kiến trúc **Controller → Service → Repository** theo chuẩn công nghiệp:

```
src/
├── main/
│   ├── java/com/badminton/shop/
│   │   ├── BadmintonShopApplication.java       # Entry point
│   │   │
│   │   ├── config/                             # Cấu hình ứng dụng
│   │   │   ├── SecurityConfig.java             # Spring Security & JWT filter
│   │   │   └── OpenApiConfig.java              # Swagger/OpenAPI config
│   │   │
│   │   ├── controller/                         # Lớp API (HTTP endpoints)
│   │   │   ├── AuthController.java             # Đăng ký, đăng nhập
│   │   │   ├── ProductController.java          # CRUD sản phẩm
│   │   │   ├── CategoryController.java         # CRUD danh mục
│   │   │   ├── OrderController.java            # Quản lý đơn hàng
│   │   │   ├── CartController.java             # Giỏ hàng
│   │   │   ├── PaymentController.java          # Thanh toán
│   │   │   └── UserController.java             # Quản lý người dùng
│   │   │
│   │   ├── service/                            # Lớp nghiệp vụ (Business Logic)
│   │   │   ├── AuthService.java
│   │   │   ├── ProductService.java
│   │   │   ├── CategoryService.java
│   │   │   ├── OrderService.java
│   │   │   ├── CartService.java
│   │   │   ├── PaymentService.java
│   │   │   ├── UserService.java
│   │   │   └── impl/                           # Triển khai service
│   │   │       ├── AuthServiceImpl.java
│   │   │       ├── ProductServiceImpl.java
│   │   │       ├── CategoryServiceImpl.java
│   │   │       ├── OrderServiceImpl.java       # Logging chi tiết
│   │   │       ├── CartServiceImpl.java
│   │   │       ├── PaymentServiceImpl.java     # Logging chi tiết
│   │   │       └── UserServiceImpl.java
│   │   │
│   │   ├── repository/                         # Lớp truy cập dữ liệu (JPA)
│   │   │   ├── UserRepository.java
│   │   │   ├── ProductRepository.java          # Tìm kiếm full-text
│   │   │   ├── CategoryRepository.java
│   │   │   ├── OrderRepository.java
│   │   │   ├── PaymentRepository.java
│   │   │   ├── CartRepository.java             # Fetch join tối ưu
│   │   │   └── CartItemRepository.java
│   │   │
│   │   ├── entity/                             # JPA Entities
│   │   │   ├── User.java
│   │   │   ├── Product.java
│   │   │   ├── Category.java
│   │   │   ├── Order.java
│   │   │   ├── OrderItem.java
│   │   │   ├── Cart.java
│   │   │   ├── CartItem.java
│   │   │   └── Payment.java
│   │   │
│   │   ├── dto/                                # Data Transfer Objects
│   │   │   ├── request/                        # Request DTOs (với validation)
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── ProductRequest.java
│   │   │   │   ├── CategoryRequest.java
│   │   │   │   ├── OrderRequest.java
│   │   │   │   ├── OrderItemRequest.java
│   │   │   │   ├── CartItemRequest.java
│   │   │   │   ├── UpdateProfileRequest.java
│   │   │   │   └── ChangePasswordRequest.java
│   │   │   └── response/                       # Response DTOs
│   │   │       ├── ApiResponse.java            # Wrapper response chuẩn
│   │   │       ├── PageResponse.java           # Pagination wrapper
│   │   │       ├── AuthResponse.java
│   │   │       ├── UserResponse.java
│   │   │       ├── ProductResponse.java
│   │   │       ├── CategoryResponse.java
│   │   │       ├── OrderResponse.java
│   │   │       ├── OrderItemResponse.java
│   │   │       ├── PaymentResponse.java
│   │   │       ├── CartResponse.java
│   │   │       └── CartItemResponse.java
│   │   │
│   │   ├── enums/                              # Enumerations
│   │   │   ├── UserRole.java                   # ROLE_ADMIN, ROLE_CUSTOMER
│   │   │   ├── OrderStatus.java                # PENDING → DELIVERED
│   │   │   ├── PaymentStatus.java              # PENDING, COMPLETED, FAILED...
│   │   │   └── PaymentMethod.java              # COD, MOMO, VNPAY...
│   │   │
│   │   ├── exception/                          # Xử lý ngoại lệ
│   │   │   ├── GlobalExceptionHandler.java     # @RestControllerAdvice
│   │   │   ├── ResourceNotFoundException.java
│   │   │   ├── BadRequestException.java
│   │   │   ├── DuplicateResourceException.java
│   │   │   ├── AccessDeniedException.java
│   │   │   └── BusinessException.java
│   │   │
│   │   ├── security/                           # Bảo mật JWT
│   │   │   ├── JwtTokenProvider.java           # Tạo & xác thực JWT
│   │   │   ├── JwtAuthenticationFilter.java    # Filter request
│   │   │   └── UserDetailsServiceImpl.java     # Load user từ DB
│   │   │
│   │   └── util/
│   │       └── AppUtils.java                   # Tiện ích (slug, orderCode...)
│   │
│   └── resources/
│       └── application.yml                     # Cấu hình ứng dụng
│
└── test/
    ├── java/com/badminton/shop/
    │   ├── BadmintonShopApplicationTests.java  # Context load test
    │   ├── service/
    │   │   └── CategoryServiceTest.java        # Unit test service
    │   └── util/
    │       └── AppUtilsTest.java               # Unit test tiện ích
    └── resources/
        └── application-test.yml                # Config cho môi trường test
```

---

## 🚀 Hướng Dẫn Cài Đặt

### Yêu Cầu

- **JDK 17+**
- **Maven 3.8+**
- **MySQL 8.x**

### Bước 1: Clone dự án

```bash
git clone https://github.com/Tieu-Luan-Tot-Nghiep/badminton-shop-api.git
cd badminton-shop-api
```

### Bước 2: Tạo database

```sql
CREATE DATABASE badminton_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Bước 3: Cấu hình môi trường

Tạo file `.env` hoặc thiết lập biến môi trường:

```bash
DB_HOST=localhost
DB_PORT=3306
DB_NAME=badminton_shop
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret_base64_encoded_min_32_chars
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
```

Hoặc chỉnh sửa trực tiếp `src/main/resources/application.yml`.

### Bước 4: Build và chạy

```bash
# Chạy tests
mvn test

# Build project
mvn clean package -DskipTests

# Chạy ứng dụng
java -jar target/badminton-shop-api-1.0.0.jar
```

Hoặc với Maven wrapper:

```bash
mvn spring-boot:run
```

### Bước 5: Truy cập

| URL | Mô tả |
|-----|-------|
| `http://localhost:8080/api` | Base URL |
| `http://localhost:8080/api/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/api/v3/api-docs` | OpenAPI JSON |
| `http://localhost:8080/api/actuator/health` | Health check |

---

## 🗺️ Sơ Đồ API

### Authentication

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `POST` | `/api/auth/register` | Đăng ký tài khoản | Public |
| `POST` | `/api/auth/login` | Đăng nhập | Public |
| `POST` | `/api/auth/refresh` | Làm mới token | Public |

### Products (Sản phẩm)

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `GET` | `/api/products` | Danh sách sản phẩm (phân trang) | Public |
| `GET` | `/api/products/{id}` | Chi tiết sản phẩm | Public |
| `GET` | `/api/products/slug/{slug}` | Tìm theo slug | Public |
| `GET` | `/api/products/category/{categoryId}` | Sản phẩm theo danh mục | Public |
| `GET` | `/api/products/search?keyword=...` | Tìm kiếm sản phẩm | Public |
| `POST` | `/api/products` | Tạo sản phẩm mới | Admin |
| `PUT` | `/api/products/{id}` | Cập nhật sản phẩm | Admin |
| `DELETE` | `/api/products/{id}` | Xóa sản phẩm | Admin |

### Categories (Danh mục)

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `GET` | `/api/categories` | Danh sách danh mục | Public |
| `GET` | `/api/categories/{id}` | Chi tiết danh mục | Public |
| `GET` | `/api/categories/slug/{slug}` | Tìm theo slug | Public |
| `POST` | `/api/categories` | Tạo danh mục | Admin |
| `PUT` | `/api/categories/{id}` | Cập nhật danh mục | Admin |
| `DELETE` | `/api/categories/{id}` | Xóa danh mục | Admin |

### Orders (Đơn hàng)

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `POST` | `/api/orders` | Tạo đơn hàng | Customer |
| `GET` | `/api/orders/my-orders` | Đơn hàng của tôi | Customer |
| `GET` | `/api/orders/{id}` | Chi tiết đơn hàng | Customer |
| `GET` | `/api/orders/code/{orderCode}` | Tìm theo mã đơn | Customer |
| `PATCH` | `/api/orders/{id}/cancel` | Hủy đơn hàng | Customer |
| `GET` | `/api/orders/admin` | Tất cả đơn hàng | Admin |
| `PATCH` | `/api/orders/admin/{id}/status` | Cập nhật trạng thái | Admin |

### Cart (Giỏ hàng)

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `GET` | `/api/cart` | Lấy giỏ hàng | Customer |
| `POST` | `/api/cart/items` | Thêm vào giỏ | Customer |
| `PUT` | `/api/cart/items/{productId}` | Cập nhật số lượng | Customer |
| `DELETE` | `/api/cart/items/{productId}` | Xóa sản phẩm | Customer |
| `DELETE` | `/api/cart` | Xóa toàn bộ giỏ | Customer |

### Payments (Thanh toán)

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `GET` | `/api/payments/order/{orderId}` | Thông tin thanh toán | Customer |
| `POST` | `/api/payments/order/{orderId}/process` | Xử lý thanh toán | Customer |
| `POST` | `/api/payments/admin/order/{orderId}/confirm` | Xác nhận thanh toán | Admin |

### Users (Người dùng)

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `GET` | `/api/users/me` | Thông tin cá nhân | Customer |
| `PUT` | `/api/users/me` | Cập nhật hồ sơ | Customer |
| `PATCH` | `/api/users/me/password` | Đổi mật khẩu | Customer |
| `GET` | `/api/users/admin` | Danh sách users | Admin |
| `GET` | `/api/users/admin/{id}` | Chi tiết user | Admin |
| `PATCH` | `/api/users/admin/{id}/disable` | Vô hiệu hóa tài khoản | Admin |

---

## 🔄 Quy Trình Trạng Thái Đơn Hàng

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
   ↓           ↓
CANCELLED    CANCELLED
                                              ↓
                                           REFUNDED
```

---

## 💳 Phương Thức Thanh Toán

| Mã | Tên |
|----|-----|
| `COD` | Thanh toán khi nhận hàng |
| `BANK_TRANSFER` | Chuyển khoản ngân hàng |
| `MOMO` | Ví điện tử MoMo |
| `VNPAY` | VNPay |
| `ZALOPAY` | ZaloPay |
| `CREDIT_CARD` | Thẻ tín dụng |

---

## 🔐 Bảo Mật

- Xác thực bằng **JWT (JSON Web Token)**
- Mật khẩu được mã hóa bằng **BCrypt**
- Phân quyền dựa trên **Role-Based Access Control (RBAC)**
- **Stateless session** (không lưu session server-side)
- **Global Exception Handler** xử lý tập trung tất cả lỗi

---

## 📊 Tối Ưu Hiệu Suất

- **Lazy Loading** cho tất cả quan hệ @OneToMany và @ManyToOne
- **Database indexes** trên các cột thường xuyên query (email, slug, status, etc.)
- **Fetch Join** trong CartRepository để tránh N+1 problem
- **Pagination** cho tất cả danh sách có thể lớn
- **Transactional(readOnly = true)** cho các query chỉ đọc
- **batch_fetch_size** cấu hình để tối ưu batch loading

---

## 🧪 Chạy Tests

```bash
# Chạy tất cả tests
mvn test

# Chạy test với báo cáo chi tiết
mvn test -Dsurefire.failIfNoSpecifiedTests=false

# Chạy một test cụ thể
mvn test -Dtest=CategoryServiceTest
```

---

## 📝 Logging

Hệ thống ghi log tại `logs/badminton-shop.log`:

- **Order flow**: Ghi log đầy đủ khi tạo đơn hàng, cập nhật trạng thái, hủy đơn
- **Payment flow**: Ghi log khi xử lý và xác nhận thanh toán
- **Auth flow**: Ghi log khi đăng ký và đăng nhập
- **Error**: Ghi log chi tiết cho tất cả lỗi hệ thống

---

## 📄 License

Dự án này được phát hành dưới giấy phép [MIT](LICENSE).