package com.badminton.shop.modules.chat.service;

import com.badminton.shop.modules.chat.dto.ChatMessageResponse;
import com.badminton.shop.modules.chat.dto.ChatReadReceiptResponse;
import com.badminton.shop.modules.chat.dto.ChatRoomResponse;
import com.badminton.shop.modules.chat.dto.ChatSendMessageRequest;
import com.badminton.shop.modules.chat.dto.ChatUnreadCountResponse;
import com.badminton.shop.modules.chat.dto.ChatUploadResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatService {

    ChatRoomResponse getOrCreateMyRoom(String principalEmail);

    Page<ChatMessageResponse> getRoomMessages(String principalEmail, String roomId, int page, int size);

    Page<ChatRoomResponse> getAdminInbox(String principalEmail, int page, int size);

    ChatMessageResponse sendMessage(String principalEmail, ChatSendMessageRequest request);

    ChatReadReceiptResponse markRoomAsRead(String principalEmail, String roomId);

    ChatUnreadCountResponse getMyUnreadCount(String principalEmail);

    List<ChatUploadResponse> uploadAttachments(List<MultipartFile> files);
}
