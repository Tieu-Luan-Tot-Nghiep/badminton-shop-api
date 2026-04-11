package com.badminton.shop.modules.admin.controller;

import com.badminton.shop.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatController {

    // Đây là mock controller theo yêu cầu để UI Admin có thể tích hợp.
    // Các logic này sẽ cần ChatService tĩnh hoặc tích hợp websocket sau.

    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<List<Object>>> getInbox() {
        return ResponseEntity.ok(ApiResponse.success("Get inbox successful", Collections.emptyList()));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<Object>>> getRoomMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Get messages successful", Collections.emptyList()));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<Object>> sendMessage(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(ApiResponse.success("Send message successful", Map.of("status", "sent")));
    }

    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<Object>> markRoomAsRead(@PathVariable String roomId) {
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @GetMapping("/unread-summary")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getUnreadSummary() {
        return ResponseEntity.ok(ApiResponse.success("Get unread summary successful", Map.of("totalUnread", 0)));
    }
}
