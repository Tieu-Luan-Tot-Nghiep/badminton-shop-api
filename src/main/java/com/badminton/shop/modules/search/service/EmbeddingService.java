package com.badminton.shop.modules.search.service;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.translator.ImageFeatureExtractor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Slf4j
@Service
public class EmbeddingService {

    private static final int TEXT_VECTOR_DIMS = 384;
    private static final int CLIP_VECTOR_DIMS = 512;

    private ZooModel<String, float[]> model;
    private ZooModel<Image, byte[]> clipImageModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.search.clip.model-path:}")
    private String clipModelPath;

    @Value("${app.search.clip.service-url:}")
    private String clipServiceUrl;

    @PostConstruct
    public void init() {
        try {
            // Sử dụng mô hình Sentence-Transformer phổ biến (384 dims)
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
                    .optEngine("PyTorch")
                    .optProgress(new ProgressBar())
                    .build();

            this.model = criteria.loadModel();
            log.info("Successfully loaded Embedding Model: all-MiniLM-L6-v2");
        } catch (Exception e) {
            log.error("Failed to load Embedding Model", e);
        }

        if (clipModelPath != null && !clipModelPath.isBlank()) {
            try {
                Criteria<Image, byte[]> clipCriteria = Criteria.builder()
                        .setTypes(Image.class, byte[].class)
                        .optModelPath(Paths.get(clipModelPath))
                        .optTranslator(ImageFeatureExtractor.builder().build())
                        .optEngine("PyTorch")
                        .optProgress(new ProgressBar())
                        .build();

                this.clipImageModel = clipCriteria.loadModel();
                log.info("Successfully loaded CLIP image model from path: {}", clipModelPath);
                return;
            } catch (Exception e) {
                log.error("Failed to load CLIP image model from local path: {}", clipModelPath, e);
            }
        }

        if (clipServiceUrl != null && !clipServiceUrl.isBlank()) {
            log.info("CLIP image embedding will use local service at: {}", clipServiceUrl);
            return;
        }

        log.warn("CLIP image embedding provider is not configured. Set app.search.clip.model-path or app.search.clip.service-url.");
    }

    public boolean isImageEmbeddingAvailable() {
        return clipImageModel != null || (clipServiceUrl != null && !clipServiceUrl.isBlank());
    }

    public float[] embed(String text) {
        if (text == null || text.isBlank()) return new float[TEXT_VECTOR_DIMS];
        try (Predictor<String, float[]> predictor = model.newPredictor()) {
            return predictor.predict(text);
        } catch (Exception e) {
            log.error("Error during embedding generation", e);
            return new float[TEXT_VECTOR_DIMS];
        }
    }

    public float[] embedImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return new float[CLIP_VECTOR_DIMS];
        }

        if (clipImageModel != null) {
            return embedImageWithLocalModel(imageBytes);
        }

        if (clipServiceUrl != null && !clipServiceUrl.isBlank()) {
            return embedImageWithLocalService(imageBytes);
        }

        return new float[CLIP_VECTOR_DIMS];
    }

    private float[] embedImageWithLocalModel(byte[] imageBytes) {
        try {
            try (InputStream inputStream = new ByteArrayInputStream(imageBytes);
                 Predictor<Image, byte[]> predictor = clipImageModel.newPredictor()) {
                Image image = ImageFactory.getInstance().fromInputStream(inputStream);
                byte[] rawOutput = predictor.predict(image);
                float[] vector = bytesToFloatArray(rawOutput);
                return resizeVector(vector, CLIP_VECTOR_DIMS);
            }
        } catch (Exception e) {
            log.error("Error during image embedding generation with local model", e);
            return new float[CLIP_VECTOR_DIMS];
        }
    }

    private float[] embedImageWithLocalService(byte[] imageBytes) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(clipServiceUrl).toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(20000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            connection.getOutputStream().write(imageBytes);

            int status = connection.getResponseCode();
            if (status >= 400) {
                throw new IllegalStateException("CLIP service returned HTTP " + status);
            }

            try (InputStream inputStream = connection.getInputStream()) {
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                JsonNode root = objectMapper.readTree(body);
                JsonNode vectorNode = root.isArray() ? root : root.get("vector");

                if (vectorNode == null || !vectorNode.isArray()) {
                    throw new IllegalStateException("CLIP service response does not contain vector array");
                }

                float[] vector = new float[vectorNode.size()];
                for (int i = 0; i < vectorNode.size(); i++) {
                    vector[i] = (float) vectorNode.get(i).asDouble();
                }
                return resizeVector(vector, CLIP_VECTOR_DIMS);
            }
        } catch (Exception e) {
            log.error("Error during image embedding generation with local CLIP service", e);
            return new float[CLIP_VECTOR_DIMS];
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public float[] embedImageFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return new float[CLIP_VECTOR_DIMS];
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(imageUrl).toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");

            try (InputStream inputStream = connection.getInputStream()) {
                byte[] imageBytes = inputStream.readAllBytes();
                return embedImage(imageBytes);
            }
        } catch (Exception e) {
            log.warn("Cannot generate CLIP embedding from thumbnail URL: {}", imageUrl, e);
            return new float[CLIP_VECTOR_DIMS];
        }
    }

    private float[] resizeVector(float[] source, int targetSize) {
        if (source == null) {
            return new float[targetSize];
        }
        if (source.length == targetSize) {
            return source;
        }

        float[] resized = new float[targetSize];
        int copyLength = Math.min(source.length, targetSize);
        System.arraycopy(source, 0, resized, 0, copyLength);
        return resized;
    }

    private float[] bytesToFloatArray(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new float[0];
        }
        int floatCount = bytes.length / Float.BYTES;
        float[] result = new float[floatCount];
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < floatCount; i++) {
            result[i] = buffer.getFloat();
        }
        return result;
    }

    @PreDestroy
    public void close() {
        if (model != null) {
            model.close();
        }
        if (clipImageModel != null) {
            clipImageModel.close();
        }
    }
}

