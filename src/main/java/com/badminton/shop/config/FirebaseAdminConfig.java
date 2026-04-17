package com.badminton.shop.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseAdminConfig {

    @Value("${app.firebase.config-json:}")
    private String firebaseConfigJson;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (InputStream serviceAccountStream = openCredentialStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .build();

            return FirebaseApp.initializeApp(options);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot initialize Firebase Admin SDK from provided configuration.", ex);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    private InputStream openCredentialStream() throws IOException {
        String jsonFromEnv = firstNonBlank(
                firebaseConfigJson,
                System.getenv("FIREBASE_CONFIG_JSON")
        );
        if (jsonFromEnv != null) {
            String normalizedJson = normalizeJsonFromEnv(jsonFromEnv);
            return new ByteArrayInputStream(normalizedJson.getBytes(StandardCharsets.UTF_8));
        }

        throw new IllegalStateException("Firebase credentials are missing. Configure FIREBASE_CONFIG_JSON.");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeJsonFromEnv(String rawJson) {
        String normalized = rawJson.trim();

        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        return normalized
                .replace("\\\"", "\"")
                .replace("\\n", "\n");
    }
}
