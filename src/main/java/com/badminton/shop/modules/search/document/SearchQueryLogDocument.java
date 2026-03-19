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
@Document(indexName = "search_query_logs")
public class SearchQueryLogDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String keyword;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime searchedAt;
}
