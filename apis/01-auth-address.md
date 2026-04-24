# Auth + Address Module

## A. AUTH MODULE
Base path: /api/auth

### 1) register
- Method name: register
- Endpoint: POST /api/auth/register
- Chuc nang: Dang ky tai khoan moi.
- Auth: Public
- Request body:
  - username: string
  - email: string
  - password: string
  - phoneNumber: string
- Response data: AuthResponse (token, username, role)
- Code:
  - 200: Register successful.
  - 400: Validation fail / email da ton tai.
  - 500: Loi he thong.
- Error can xu ly UI:
  - Field validation
  - Duplicate email/username

### 2) login
- Method name: login
- Endpoint: POST /api/auth/login
- Chuc nang: Dang nhap va lay access/refresh token.
- Auth: Public
- Request body:
  - username
  - password
- Response data: AuthResponse (token, refreshToken, username, role)
- Code: 200, 400, 401, 500
- Error:
  - Sai thong tin dang nhap
  - Tai khoan chua verify

### 3) refresh
- Method name: refresh
- Endpoint: POST /api/auth/refresh
- Chuc nang: Lam moi access token.
- Auth: Public (dua vao refreshToken)
- Request body:
  - refreshToken: string
- Response data: AuthResponse (token, refreshToken)
- Code: 200, 400, 401

### 4) logout
- Method name: logout
- Endpoint: POST /api/auth/logout
- Chuc nang: Vo hieu refresh token.
- Auth: Public (dua vao refreshToken)
- Request body:
  - refreshToken
- Response data: null
- Code: 200, 400

### 5) verifyEmail
- Method name: verifyEmail
- Endpoint: GET /api/auth/verify-email
- Chuc nang: Xac thuc email.
- Auth: Public
- Request query:
  - token
  - email
- Response data: null
- Code: 200, 400

### 6) resendVerification
- Method name: resendVerification
- Endpoint: POST /api/auth/resend-verification
- Chuc nang: Gui lai email verify.
- Auth: Public
- Request body:
  - email
- Response data: null
- Code: 200, 400

### 7) forgotPassword
- Method name: forgotPassword
- Endpoint: POST /api/auth/forgot-password
- Chuc nang: Gui link reset password.
- Auth: Public
- Request body:
  - email
- Response data: null
- Code: 200, 400

### 8) resetPassword
- Method name: resetPassword
- Endpoint: POST /api/auth/reset-password
- Chuc nang: Dat lai mat khau bang token.
- Auth: Public
- Request body:
  - token
  - newPassword
- Response data: null
- Code: 200, 400

### 9) changePassword
- Method name: changePassword
- Endpoint: POST /api/auth/change-password
- Chuc nang: Doi mat khau khi da dang nhap.
- Auth: Bearer User
- Request body:
  - oldPassword
  - newPassword
- Response data: null
- Code: 200, 400, 401

### 10) getCurrentUser
- Method name: getCurrentUser
- Endpoint: GET /api/auth/me
- Chuc nang: Lay profile user hien tai.
- Auth: Bearer User
- Request: khong body
- Response data: UserProfileResponse
  - id, fullName, email, birthDate, avatar, role
- Code: 200, 401

### 11) updateProfile
- Method name: updateProfile
- Endpoint: PUT /api/auth/profile
- Chuc nang: Cap nhat thong tin profile.
- Auth: Bearer User
- Request body:
  - fullName
  - phoneNumber
  - birthDate
- Response data: null
- Code: 200, 400, 401

### 12) updateAvatar
- Method name: updateAvatar
- Endpoint: POST /api/auth/profile/avatar
- Chuc nang: Upload avatar user.
- Auth: Bearer User
- Request: multipart/form-data
  - file
- Response data: null
- Code: 200, 400, 401

### 13) deleteAccount
- Method name: deleteAccount
- Endpoint: DELETE /api/auth/account
- Chuc nang: Vo hieu hoa tai khoan.
- Auth: Bearer User
- Response data: null
- Code: 200, 401

---

## B. ADDRESS MODULE
Base path: /api/addresses

### 1) createAddress
- Method name: createAddress
- Endpoint: POST /api/addresses
- Chuc nang: Tao dia chi moi cho user.
- Auth: Bearer User
- Request body:
  - receiverName
  - phoneNumber
  - province
  - district
  - ward
  - specificAddress
  - isDefault
- Response data: AddressResponse
  - id, receiverName, phoneNumber, province, district, ward, specificAddress, ghnProvinceId, ghnDistrictId, ghnWardCode, isDefault
- Code:
  - 201: Tao thanh cong
  - 400: Mapping GHN that bai / validation fail
  - 401

### 2) updateAddress
- Method name: updateAddress
- Endpoint: PUT /api/addresses/{id}
- Chuc nang: Cap nhat dia chi.
- Auth: Bearer User
- Request body: giong createAddress
- Response data: AddressResponse
- Code: 200, 400, 401, 404

### 3) deleteAddress
- Method name: deleteAddress
- Endpoint: DELETE /api/addresses/{id}
- Chuc nang: Xoa dia chi.
- Auth: Bearer User
- Response data: null
- Code: 200, 401, 404

### 4) getAllAddresses
- Method name: getAllAddresses
- Endpoint: GET /api/addresses
- Chuc nang: Lay danh sach dia chi cua user.
- Auth: Bearer User
- Response data: List<AddressResponse>
- Code: 200, 401

### 5) getAddressById
- Method name: getAddressById
- Endpoint: GET /api/addresses/{id}
- Chuc nang: Lay chi tiet 1 dia chi.
- Auth: Bearer User
- Response data: AddressResponse
- Code: 200, 401, 404

### 6) setDefaultAddress
- Method name: setDefaultAddress
- Endpoint: PATCH /api/addresses/{id}/default
- Chuc nang: Dat dia chi mac dinh.
- Auth: Bearer User
- Response data: null
- Code: 200, 401, 404
