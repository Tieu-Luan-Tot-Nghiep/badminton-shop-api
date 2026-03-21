package com.badminton.shop.modules.messaging.repository;

import com.badminton.shop.modules.messaging.entity.ChatMessageDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessageDocument, String> {

    Page<ChatMessageDocument> findByRoomId(String roomId, Pageable pageable);
}
