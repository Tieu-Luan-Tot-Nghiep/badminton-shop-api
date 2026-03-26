package com.badminton.shop.modules.chat.service.impl;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.exception.ResourceNotFoundException;
import com.badminton.shop.modules.auth.entity.Role;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.chat.dto.ChatMessagePersistEvent;
import com.badminton.shop.modules.chat.dto.ChatMessageResponse;
import com.badminton.shop.modules.chat.dto.ChatReadReceiptResponse;
import com.badminton.shop.modules.chat.dto.ChatRoomResponse;
import com.badminton.shop.modules.chat.dto.ChatSendMessageRequest;
import com.badminton.shop.modules.chat.dto.ChatUnreadCountResponse;
import com.badminton.shop.modules.chat.dto.ChatUploadResponse;
import com.badminton.shop.modules.chat.entity.ChatMessageDocument;
import com.badminton.shop.modules.chat.entity.ChatMessageType;
import com.badminton.shop.modules.chat.entity.ChatRoomDocument;
import com.badminton.shop.modules.chat.repository.ChatMessageRepository;
import com.badminton.shop.modules.chat.repository.ChatRoomRepository;
import com.badminton.shop.modules.chat.service.ChatService;
import com.badminton.shop.utils.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private static final long MAX_UPLOAD_REQUEST_SIZE_BYTES = 5L * 1024 * 1024;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MongoTemplate mongoTemplate;
    private final S3Service s3Service;

    @Override
    public ChatRoomResponse getOrCreateMyRoom(String principalEmail) {
        User user = findUserByPrincipal(principalEmail);
        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("Only customers can auto-create personal chat room.");
        }

        ChatRoomDocument room = chatRoomRepository.findByCustomerId(user.getId())
                .orElseGet(() -> chatRoomRepository.save(ChatRoomDocument.builder()
                        .customerId(user.getId())
                        .customerEmail(user.getEmail())
                        .customerName(resolveDisplayName(user))
                        .adminUnreadCount(0)
                        .customerUnreadCount(0)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));

        return toRoomResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getRoomMessages(String principalEmail, String roomId, int page, int size) {
        User actor = findUserByPrincipal(principalEmail);
        ChatRoomDocument room = getRoomOrThrow(roomId);
        validateRoomAccess(actor, room);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "sentAt"));

        return chatMessageRepository.findByRoomId(roomId, pageable).map(this::toMessageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatRoomResponse> getAdminInbox(String principalEmail, int page, int size) {
        User actor = findUserByPrincipal(principalEmail);
        if (actor.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Only admin can access admin inbox.");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        return chatRoomRepository.findAllByOrderByLastMessageAtDesc(pageable).map(this::toRoomResponse);
    }

    @Override
    public ChatMessageResponse sendMessage(String principalEmail, ChatSendMessageRequest request) {
        User actor = findUserByPrincipal(principalEmail);
        ChatRoomDocument room = getRoomOrThrow(request.getRoomId());
        validateRoomAccess(actor, room);
        validateMessagePayload(request);

        ChatMessagePersistEvent event = ChatMessagePersistEvent.builder()
                .messageId(UUID.randomUUID().toString())
                .roomId(room.getId())
                .senderId(actor.getId())
                .senderEmail(actor.getEmail())
                .senderName(resolveDisplayName(actor))
                .senderRole(actor.getRole())
                .messageType(request.getMessageType())
                .content(request.getContent())
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .sentAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.CHAT_MESSAGE_PERSIST_ROUTING_KEY,
                event
        );

        return toMessageResponse(event);
    }

    @Override
    public ChatReadReceiptResponse markRoomAsRead(String principalEmail, String roomId) {
        User actor = findUserByPrincipal(principalEmail);
        ChatRoomDocument room = getRoomOrThrow(roomId);
        validateRoomAccess(actor, room);

        LocalDateTime readAt = LocalDateTime.now();

        Query query = Query.query(Criteria.where("roomId").is(roomId));
        if (actor.getRole() == Role.CUSTOMER) {
            query.addCriteria(Criteria.where("senderRole").is(Role.ADMIN));
            query.addCriteria(Criteria.where("readByCustomerAt").is(null));
            mongoTemplate.updateMulti(query, new Update().set("readByCustomerAt", readAt), ChatMessageDocument.class);

            room.setCustomerUnreadCount(0);
        } else {
            query.addCriteria(Criteria.where("senderRole").is(Role.CUSTOMER));
            query.addCriteria(Criteria.where("readByAdminAt").is(null));
            mongoTemplate.updateMulti(query, new Update().set("readByAdminAt", readAt), ChatMessageDocument.class);

            room.setAdminUnreadCount(0);
        }

        room.setUpdatedAt(readAt);
        chatRoomRepository.save(room);

        return ChatReadReceiptResponse.builder()
                .roomId(roomId)
                .readerRole(actor.getRole())
                .readAt(readAt)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatUnreadCountResponse getMyUnreadCount(String principalEmail) {
        User actor = findUserByPrincipal(principalEmail);
        if (actor.getRole() == Role.CUSTOMER) {
            ChatRoomDocument room = chatRoomRepository.findByCustomerId(actor.getId()).orElse(null);
            return ChatUnreadCountResponse.builder()
                    .roomId(room != null ? room.getId() : null)
                    .unreadCount(room != null ? safeInt(room.getCustomerUnreadCount()) : 0)
                    .build();
        }

        int unread = chatRoomRepository.findAll().stream()
                .mapToInt(room -> safeInt(room.getAdminUnreadCount()))
                .sum();

        return ChatUnreadCountResponse.builder()
                .roomId(null)
                .unreadCount(unread)
                .build();
    }

    @Override
    public List<ChatUploadResponse> uploadAttachments(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one attachment file is required.");
        }
        if (files.size() > 10) {
            throw new IllegalArgumentException("You can upload up to 10 files per request.");
        }

        long totalSize = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .mapToLong(MultipartFile::getSize)
                .sum();
        if (totalSize > MAX_UPLOAD_REQUEST_SIZE_BYTES) {
            throw new IllegalArgumentException("Total upload size must not exceed 5MB per request.");
        }

        return files.stream()
                .map(file -> {
                    if (file == null || file.isEmpty()) {
                        throw new IllegalArgumentException("Attachment file must not be empty.");
                    }

                    String customFileName = "chat-" + UUID.randomUUID();
                    String fileUrl = s3Service.uploadFile("chat", customFileName, file);

                    return ChatUploadResponse.builder()
                            .fileUrl(fileUrl)
                            .fileName(file.getOriginalFilename())
                            .contentType(file.getContentType())
                            .build();
                })
                .toList();
    }

    private void validateMessagePayload(ChatSendMessageRequest request) {
        if (request.getMessageType() == ChatMessageType.TEXT) {
            if (request.getContent() == null || request.getContent().isBlank()) {
                throw new IllegalArgumentException("Text message must contain content.");
            }
            return;
        }

        if (request.getFileUrl() == null || request.getFileUrl().isBlank()) {
            throw new IllegalArgumentException("Image/File message must contain fileUrl.");
        }
    }

    private User findUserByPrincipal(String principalEmail) {
        return userRepository.findByEmail(principalEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + principalEmail));
    }

    private ChatRoomDocument getRoomOrThrow(String roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found: " + roomId));
    }

    private void validateRoomAccess(User actor, ChatRoomDocument room) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }

        if (!actor.getId().equals(room.getCustomerId())) {
            throw new IllegalArgumentException("You are not allowed to access this chat room.");
        }
    }

    private String resolveDisplayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
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

    private ChatMessageResponse toMessageResponse(ChatMessagePersistEvent event) {
        return ChatMessageResponse.builder()
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
                .sentAt(event.getSentAt())
                .build();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
