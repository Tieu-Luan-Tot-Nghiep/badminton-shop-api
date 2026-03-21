package com.badminton.shop.modules.messaging.repository;

import com.badminton.shop.modules.messaging.entity.ChatRoomDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoomDocument, String> {

    Optional<ChatRoomDocument> findByCustomerId(Long customerId);

    Page<ChatRoomDocument> findAllByOrderByLastMessageAtDesc(Pageable pageable);
}
