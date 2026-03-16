package com.badminton.shop.utils.s3;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Template s3Template;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String uploadFile(String folder, String customFileName, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = folder + "/" + customFileName + extension;
        try (InputStream inputStream = file.getInputStream()) {
            s3Template.upload(bucketName, fileName, inputStream);
            // Construct the S3 URL. 
            // Note: For public access, the bucket needs appropriate policies.
            // Format usually: https://bucket-name.s3.region.amazonaws.com/filename
            // Or use s3Template.downloadUrl if short-lived or specific config.
            // Since we want a permanent link for avatar:
            return String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);
        } catch (Exception e) {
            log.error("Failed to upload file to S3. Bucket: {}, File: {}. Error: {}", bucketName, fileName, e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tải ảnh lên hệ thống lưu trữ: " + e.getMessage());
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        try {
            // Extract key from URL: https://bucket-name.s3.amazonaws.com/key
            String key = fileUrl.substring(fileUrl.lastIndexOf(".amazonaws.com/") + 15);
            log.info("Deleting file from S3: {}", key);
            s3Template.deleteObject(bucketName, key);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", fileUrl, e);
        }
    }
}
