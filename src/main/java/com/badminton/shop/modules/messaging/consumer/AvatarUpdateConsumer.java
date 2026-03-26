package com.badminton.shop.modules.chat.consumer;

import com.badminton.shop.config.RabbitMQConfig;
import com.badminton.shop.modules.auth.entity.User;
import com.badminton.shop.modules.auth.repository.UserRepository;
import com.badminton.shop.modules.messaging.dto.AvatarUpdateMessage;
import com.badminton.shop.utils.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarUpdateConsumer {

    private final UserRepository userRepository;
    private final S3Service s3Service;

    @RabbitListener(queues = RabbitMQConfig.AVATAR_UPDATE_QUEUE)
    public void consumeAvatarUpdate(AvatarUpdateMessage message) {
        log.info("Nhận message cập nhật avatar cho email: {}", message.getEmail());
        
        try {
            User user = userRepository.findByEmail(message.getEmail())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + message.getEmail()));

            String oldAvatarUrl = message.getOldAvatarUrl();
            user.setAvatar(message.getAvatarUrl());
            userRepository.save(user);
            
            log.info("Cập nhật avatar thành công cho người dùng: {}", message.getEmail());

            // Xóa ảnh cũ trên S3 nếu có
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                log.info("Bắt đầu xóa ảnh cũ trên S3: {}", oldAvatarUrl);
                s3Service.deleteFile(oldAvatarUrl);
            }
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật avatar hoặc dọn dẹp ảnh cũ cho {}: {}", message.getEmail(), e.getMessage());
        }
    }
}
