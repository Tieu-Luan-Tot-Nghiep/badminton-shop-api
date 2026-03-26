package com.badminton.shop.modules.chatbot.repository;

import com.badminton.shop.modules.chatbot.document.ChatHistoryDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ChatHistoryRepository extends ElasticsearchRepository<ChatHistoryDocument, String> {

    List<ChatHistoryDocument> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
