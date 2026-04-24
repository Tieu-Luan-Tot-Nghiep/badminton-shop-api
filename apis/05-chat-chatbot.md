# Chat + Chatbot Module

## A. CHAT MODULE (REST)
Base path: /api/chat

### 1) getOrCreateMyRoom
- Method name: getOrCreateMyRoom
- Endpoint: GET /api/chat/rooms/me
- Chuc nang: Lay room chat cua customer, neu chua co thi tao.
- Auth: Bearer User
- Response data: ChatRoomResponse
- Code: 200, 401

### 2) getRoomMessages
- Method name: getRoomMessages
- Endpoint: GET /api/chat/rooms/{roomId}/messages
- Chuc nang: Lay lich su tin nhan co phan trang.
- Auth: Bearer User (customer chi room cua minh, admin co the xem tat ca)
- Query: page, size
- Response data: Page<ChatMessageResponse>
- Code: 200, 401, 403, 404

### 3) getMyUnreadCount
- Method name: getMyUnreadCount
- Endpoint: GET /api/chat/unread-count
- Chuc nang: Lay so unread cua user.
- Auth: Bearer User
- Response data: ChatUnreadCountResponse
- Code: 200, 401

### 4) markRoomAsRead
- Method name: markRoomAsRead
- Endpoint: POST /api/chat/rooms/{roomId}/read
- Chuc nang: Danh dau da doc room.
- Auth: Bearer User
- Response data: ChatReadReceiptResponse
- Code: 200, 401, 403, 404

### 5) uploadAttachment
- Method name: uploadAttachment
- Endpoint: POST /api/chat/upload
- Chuc nang: Upload nhieu file dinh kem de gui qua STOMP.
- Auth: Bearer User
- Request: multipart/form-data
  - files[]
- Rule:
  - toi da 10 file/request
  - tong dung luong toi da 5MB/request
- Response data: List<ChatUploadResponse>
- Code: 200, 400, 401

### 6) getAdminInbox
- Method name: getAdminInbox
- Endpoint: GET /api/chat/admin/inbox
- Chuc nang: Inbox room cho admin.
- Auth: ADMIN
- Query: page, size
- Response data: Page<ChatRoomResponse>
- Code: 200, 401, 403

---

## B. CHAT MODULE (WebSocket / STOMP)

### Cau hinh
- Handshake endpoint: /ws-chat
- App destination prefix: /app
- User destination prefix: /user
- STOMP CONNECT header: Authorization: Bearer <access_token>

### 1) sendMessage
- Method name: sendMessage
- Destination: /app/chat.send
- Chuc nang: Gui tin nhan trong room.
- Auth: Bearer User
- Request payload: ChatSendMessageRequest
  - roomId
  - messageType (TEXT|IMAGE|FILE)
  - content
  - fileUrl
  - fileName
- Server push:
  - /user/queue/chat.sent (ack gui)
  - /topic/chat.room.{roomId} (stream room)
- Code/Error:
  - Loi auth: disconnect/forbidden
  - Loi validate payload: reject message

### 2) markRead
- Method name: markRead
- Destination: /app/chat.read
- Chuc nang: Danh dau da doc qua socket.
- Auth: Bearer User
- Request payload: ChatReadRequest { roomId }
- Server push:
  - /topic/chat.room.{roomId}.read

---

## C. CHATBOT MODULE
Base path: /api/chatbot

### 1) ask
- Method name: ask
- Endpoint: POST /api/chatbot/ask
- Chuc nang: Hoi dap chatbot.
- Auth: Bearer User (neu anonymous se 401)
- Request body:
  - question: string
- Response data: ChatbotAskResponse
- Code:
  - 200: tra loi thanh cong
  - 401: chua dang nhap
  - 400: cau hoi khong hop le

### 2) closeSession
- Method name: closeSession
- Endpoint: POST /api/chatbot/close-session
- Chuc nang: Dong phien chatbot cua user.
- Auth: Bearer User
- Response data: ChatbotCloseSessionResponse
- Code: 200, 401

### 3) sessionState
- Method name: sessionState
- Endpoint: GET /api/chatbot/session-state
- Chuc nang: Kiem tra trang thai phien chatbot.
- Auth: Bearer User
- Response data: ChatbotSessionStateResponse
- Code: 200, 401
