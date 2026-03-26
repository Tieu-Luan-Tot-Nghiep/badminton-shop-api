package com.badminton.shop.modules.chat.controller;

import com.badminton.shop.common.dto.ApiResponse;
import com.badminton.shop.modules.chat.dto.ChatMessageResponse;
import com.badminton.shop.modules.chat.dto.ChatReadReceiptResponse;
import com.badminton.shop.modules.chat.dto.ChatReadRequest;
import com.badminton.shop.modules.chat.dto.ChatRoomResponse;
import com.badminton.shop.modules.chat.dto.ChatUnreadCountResponse;
import com.badminton.shop.modules.chat.dto.ChatUploadResponse;
import com.badminton.shop.modules.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @GetMapping("/rooms/me")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getOrCreateMyRoom(Principal principal) {
        ChatRoomResponse response = chatService.getOrCreateMyRoom(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Chat room fetched successfully.", response));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getRoomMessages(
            Principal principal,
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ChatMessageResponse> response = chatService.getRoomMessages(principal.getName(), roomId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Chat message history fetched successfully.", response));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<ChatUnreadCountResponse>> getMyUnreadCount(Principal principal) {
        ChatUnreadCountResponse response = chatService.getMyUnreadCount(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched successfully.", response));
    }

    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<ChatReadReceiptResponse>> markRoomAsRead(
            Principal principal,
            @PathVariable String roomId
    ) {
        ChatReadReceiptResponse response = chatService.markRoomAsRead(principal.getName(), roomId);
        return ResponseEntity.ok(ApiResponse.success("Room marked as read.", response));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<List<ChatUploadResponse>>> uploadAttachment(
            Principal principal,
            @RequestParam("files") List<MultipartFile> files
    ) {
        List<ChatUploadResponse> response = chatService.uploadAttachments(files);
        return ResponseEntity.ok(ApiResponse.success("Attachment uploaded successfully.", response));
    }

    @GetMapping("/admin/inbox")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ChatRoomResponse>>> getAdminInbox(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ChatRoomResponse> response = chatService.getAdminInbox(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Admin inbox fetched successfully.", response));
    }
}
