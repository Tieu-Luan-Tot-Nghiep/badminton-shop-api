# Tài liệu Chức năng Semantic Search

Hệ thống sử dụng **Semantic Search** (Tìm kiếm ngữ nghĩa) để cải thiện trải nghiệm người dùng, cho phép tìm kiếm dựa trên ý nghĩa của câu lệnh thay vì chỉ khớp từ khóa (keyword matching).

## 1. Công nghệ sử dụng
- **Model**: `all-MiniLM-L6-v2` (Sentence-Transformers) thông qua thư viện **DJL (Deep Java Library)**.
- **Dimentions**: 384 chiều.
- **Engine**: PyTorch.
- **Vector Database**: Elasticsearch (Dense Vector field).
- **Similarity**: Cosine Similarity.

## 2. Luồng thực thi (Execution Flow)

### A. Luồng Đánh chỉ mục (Indexing Flow)
Khi một sản phẩm được thêm mới hoặc cập nhật:
1. `ProductServiceImpl` phát sự kiện đồng bộ.
2. `ProductSearchServiceImpl` nhận dữ liệu sản phẩm.
3. Hệ thống tổng hợp các trường văn bản (`name`, `brand`, `category`, `description`) thành một chuỗi đại diện (context).
4. `EmbeddingService` sử dụng mô hình AI để chuyển chuỗi văn bản này thành một **Vector 384 chiều**.
5. Vector này được lưu vào trường `my_vector` trong Elasticsearch.

### B. Luồng Tìm kiếm (Search Flow)
Khi người dùng gọi API `/api/search/products/semantic?query=...`:
1. **Vector hóa Query**: Chuỗi tìm kiếm của người dùng (ví dụ: "vợt cho người mới chơi") được gửi tới `EmbeddingService` để chuyển thành Vector (384 dims).
2. **K-Nearest Neighbors (KNN)**: Hệ thống sử dụng thuật toán KNN của Elasticsearch để tìm ra các sản phẩm có Vector "gần" với Vector của người dùng nhất dựa trên khoảng cách Cosine.
3. **Filtering**: Kết quả KNN được kết hợp với các bộ lọc cơ bản (ví dụ: `isDeleted = false`, `isActive = true`).
4. **Ranking**: Elasticsearch trả về các kết quả có độ tương đồng cao nhất.

## 3. Ưu điểm so với Keyword Search
- Hiểu được ngữ cảnh (Ví dụ: Tìm "vợt linh hoạt" có thể ra các mẫu vợt 4U, thân dẻo dù trong tên ko có chữ "linh hoạt").
- Xử lý được đồng nghĩa và đa nghĩa tốt hơn.
- Không bị phụ thuộc cứng nhắc vào lỗi chính tả.

---
*Cập nhật ngày: 19/03/2026*

