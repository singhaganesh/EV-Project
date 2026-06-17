package com.ganesh.EV_Project.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Single place that initializes the Firebase Admin SDK for the whole app.
 *
 * Both FCM push ({@code FcmService}) and phone-auth login
 * ({@code FirebaseAuthService}) rely on the default {@link FirebaseApp} being
 * initialized here. Initialization is idempotent and non-fatal: if no
 * service-account credentials are configured (or init fails), the app still
 * boots — the dependent services simply report themselves as disabled.
 *
 * Configure the credentials path via {@code fcm.credentials-path}; if blank it
 * falls back to {@code GOOGLE_APPLICATION_CREDENTIALS}.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${fcm.credentials-path:}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return; // already initialized
        }
        try {
            GoogleCredentials credentials;
            if (credentialsPath != null && !credentialsPath.isBlank()) {
                try (InputStream in = new FileInputStream(credentialsPath)) {
                    credentials = GoogleCredentials.fromStream(in);
                }
            } else {
                // Falls back to GOOGLE_APPLICATION_CREDENTIALS if present; otherwise throws.
                credentials = GoogleCredentials.getApplicationDefault();
            }

            FirebaseApp.initializeApp(FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build());
            log.info("Firebase Admin initialized — FCM push and phone-auth login enabled");
        } catch (Exception e) {
            log.warn("Firebase Admin NOT initialized ({}). FCM push and phone-auth login are disabled. "
                    + "Set fcm.credentials-path to a service-account JSON to enable.", e.getMessage());
        }
    }
}
