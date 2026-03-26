package com.badminton.shop.modules.chatbot.document;

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
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "chat_history")
public class ChatHistoryDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Keyword)
    private List<String> recommendedProducts;

    @Field(name = "memory_vector", type = FieldType.Dense_Vector, dims = 384, index = true, similarity = "cosine")
    private float[] memoryVector;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime createdAt;
}
