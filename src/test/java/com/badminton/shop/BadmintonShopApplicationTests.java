package com.badminton.shop;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.badminton.shop.modules.search.repository.ProductSearchRepository;
import com.badminton.shop.modules.search.repository.SearchQueryLogRepository;
import com.badminton.shop.modules.chatbot.repository.ChatHistoryRepository;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import com.badminton.shop.modules.chat.repository.ChatMessageRepository;
import com.badminton.shop.modules.chat.repository.ChatRoomRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration,"
        + "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
})
@ActiveProfiles("test")
class BadmintonShopApplicationTests {

    @MockBean
    private ProductSearchRepository productSearchRepository;

    @MockBean
    private SearchQueryLogRepository searchQueryLogRepository;

    @MockBean
    private ChatHistoryRepository chatHistoryRepository;

    @MockBean
    private ChatMessageRepository chatMessageRepository;

    @MockBean
    private ChatRoomRepository chatRoomRepository;

    @TestConfiguration
    static class S3TestConfig {
        @Bean
        S3Template s3Template() {
            return Mockito.mock(S3Template.class);
        }

        @Bean
        FirebaseApp firebaseApp() {
            return Mockito.mock(FirebaseApp.class);
        }

        @Bean
        FirebaseAuth firebaseAuth() {
            return Mockito.mock(FirebaseAuth.class);
        }

        @Bean
        ElasticsearchOperations elasticsearchOperations() {
            return Mockito.mock(ElasticsearchOperations.class);
        }

        @Bean
        MongoTemplate mongoTemplate() {
            return Mockito.mock(MongoTemplate.class);
        }
    }

    @Test
    void contextLoads() {
    }

}
