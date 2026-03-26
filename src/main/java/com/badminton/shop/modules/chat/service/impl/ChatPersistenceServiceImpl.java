package com.badminton.shop.modules.chat.service.impl;

import com.badminton.shop.modules.auth.entity.Role;
import com.badminton.shop.modules.chat.dto.ChatMessagePersistEvent;
import com.badminton.shop.modules.chat.dto.ChatMessageResponse;
import com.badminton.shop.modules.chat.dto.ChatRoomResponse;
import com.badminton.shop.modules.chat.dto.ChatUnreadCountResponse;
import com.badminton.shop.modules.chat.entity.ChatMessageDocument;
import com.badminton.shop.modules.chat.entity.ChatRoomDocument;
import com.badminton.shop.modules.chat.repository.ChatMessageRepository;
import com.badminton.shop.modules.chat.repository.ChatRoomRepository;
import com.badminton.shop.modules.chat.service.ChatPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatPersistenceServiceImpl implements ChatPersistenceService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void persistMessage(ChatMessagePersistEvent event) {
        ChatRoomDocument room = chatRoomRepository.findById(event.getRoomId()).orElse(null);
        if (room == null) {
            log.warn("[chat] Room not found for event: {}", event.getRoomId());
            return;
        }

        ChatMessageDocument message = ChatMessageDocument.builder()
                .id(event.getMessageId())
                .roomId(event.getRoomId())
                .senderId(event.getSenderId())
                .senderEmail(event.getSenderEmail())
                .senderName(event.getSenderName())
                .senderRole(event.getSenderRole())
                .messageType(event.getMessageType())
                .content(event.getContent())
                .fileUrl(event.getFileUrl())
                .fileName(event.getFileName())
                .sentAt(event.getSentAt() == null ? LocalDateTime.now() : event.getSentAt())
                .build();

        chatMessageRepository.save(message);

        room.setLastMessageAt(message.getSentAt());
        room.setLastMessagePreview(buildPreview(message));
        room.setUpdatedAt(LocalDateTime.now());

        if (event.getSenderRole() == Role.CUSTOMER) {
            room.setAdminUnreadCount(safeInt(room.getAdminUnreadCount()) + 1);
        } else {
            room.setCustomerUnreadCount(safeInt(room.getCustomerUnreadCount()) + 1);
        }

        chatRoomRepository.save(room);

        ChatMessageResponse messageResponse = toMessageResponse(message);
        ChatRoomResponse roomResponse = toRoomResponse(room);

        messagingTemplate.convertAndSend("/topic/chat.room." + room.getId(), messageResponse);
        messagingTemplate.convertAndSend("/topic/chat.admin.inbox", roomResponse);

        messagingTemplate.convertAndSendToUser(
                room.getCustomerEmail(),
                "/queue/chat.unread",
                ChatUnreadCountResponse.builder()
                        .roomId(room.getId())
                        .unreadCount(safeInt(room.getCustomerUnreadCount()))
                        .build()
        );

        int adminTotalUnread = chatRoomRepository.findAll().stream()
                .mapToInt(r -> safeInt(r.getAdminUnreadCount()))
                .sum();
        messagingTemplate.convertAndSend("/topic/chat.admin.unread", ChatUnreadCountResponse.builder()
                .roomId(null)
                .unreadCount(adminTotalUnread)
                .build());
    }

    private String buildPreview(ChatMessageDocument message) {
        if (message.getMessageType() == null) {
            return "";
        }

        return switch (message.getMessageType()) {
            case TEXT -> message.getContent() == null ? "" : message.getContent();
            case IMAGE -> "[Image] " + (message.getFileName() == null ? "" : message.getFileName());
            case FILE -> "[File] " + (message.getFileName() == null ? "" : message.getFileName());
        };
    }

    private ChatMessageResponse toMessageResponse(ChatMessageDocument message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .senderEmail(message.getSenderEmail())
                .senderName(message.getSenderName())
                .senderRole(message.getSenderRole())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .sentAt(message.getSentAt())
                .readByCustomerAt(message.getReadByCustomerAt())
                .readByAdminAt(message.getReadByAdminAt())
                .build();
    }

    private ChatRoomResponse toRoomResponse(ChatRoomDocument room) {
        return ChatRoomResponse.builder()
                .roomId(room.getId())
                .customerId(room.getCustomerId())
                .customerEmail(room.getCustomerEmail())
                .customerName(room.getCustomerName())
                .lastMessagePreview(room.getLastMessagePreview())
                .lastMessageAt(room.getLastMessageAt())
                .adminUnreadCount(safeInt(room.getAdminUnreadCount()))
                .customerUnreadCount(safeInt(room.getCustomerUnreadCount()))
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
