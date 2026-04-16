package com.badminton.shop.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FirebaseAdminConfig {

    @Value("${app.firebase.service-account-path}")
    private String serviceAccountPath;

    @Value("${app.firebase.project-id:}")
    private String projectId;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        Path credentialPath = resolveCredentialPath(serviceAccountPath);
        try (InputStream serviceAccountStream = Files.newInputStream(credentialPath)) {
            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream));

            if (projectId != null && !projectId.isBlank()) {
                optionsBuilder.setProjectId(projectId.trim());
            }

            return FirebaseApp.initializeApp(optionsBuilder.build());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot initialize Firebase Admin SDK from: " + credentialPath, ex);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    private Path resolveCredentialPath(String configuredPath) {
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            throw new IllegalStateException("Property app.firebase.service-account-path is required.");
        }

        Path path = Paths.get(configuredPath.trim());
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }

        if (!Files.exists(path)) {
            throw new IllegalStateException("Firebase service account file not found: " + path);
        }

        return path;
    }
}
