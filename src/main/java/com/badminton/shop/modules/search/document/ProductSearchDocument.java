package com.badminton.shop.modules.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// THÊM: createIndex = false để chặn Spring tự tạo index với cấu hình shards/replicas
@Document(indexName = "products")
public class ProductSearchDocument {

    @Id
    private Long id;

    // Phân tích văn bản tiếng Việt/Anh chuẩn
    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(type = FieldType.Text)
    private String shortDescription;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;

    @Field(type = FieldType.Double)
    private Double basePrice;

    @Field(type = FieldType.Keyword)
    private String brandName;

    @Field(type = FieldType.Keyword)
    private String brandSlug;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String categorySlug;

    @Field(type = FieldType.Boolean)
    private Boolean isActive;

    @Field(type = FieldType.Boolean)
    private Boolean isDeleted;

    @Field(name = "my_vector", type = FieldType.Dense_Vector, dims = 384, index = true, similarity = "cosine")
    private float[] myVector;

    @Field(name = "clip_image_vector", type = FieldType.Dense_Vector, dims = 512, index = true, similarity = "cosine")
    private float[] clipImageVector;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime updatedAt;
}