package com.ganesh.EV_Project.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Low-level Firebase Cloud Messaging sender (CV-11).
 *
 * Initializes the Firebase Admin SDK from a service-account JSON. If no
 * credentials are configured (or init fails), the service runs in a disabled
 * state and {@link #sendToToken} becomes a no-op — so the backend boots and all
 * endpoints work even before the service-account key is provided.
 */
@Service
@Slf4j
public class FcmService {

    public enum Result { SENT, INVALID_TOKEN, FAILED, DISABLED }

    @Value("${fcm.credentials-path:}")
    private String credentialsPath;

    private boolean enabled = false;

    @PostConstruct
    void init() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                enabled = true;
                return;
            }

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
            enabled = true;
            log.info("Firebase Admin initialized — FCM push enabled");
        } catch (Exception e) {
            enabled = false;
            log.warn("FCM disabled: could not initialize Firebase Admin ({}). "
                    + "Set fcm.credentials-path to a service-account JSON to enable push.", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sends a data-only message so the Android client always builds the
     * notification itself (foreground and background) and can act on the
     * deep link. Returns a {@link Result} so the caller can prune dead tokens.
     */
    public Result sendToToken(String token, String type, String title, String body, String deepLink) {
        if (!enabled) return Result.DISABLED;
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .putData("type", nz(type))
                    .putData("title", nz(title))
                    .putData("body", nz(body))
                    .putData("deepLink", nz(deepLink))
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .build();
            FirebaseMessaging.getInstance().send(message);
            return Result.SENT;
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                log.info("Pruning invalid FCM token: {}", code);
                return Result.INVALID_TOKEN;
            }
            log.warn("FCM send failed ({}): {}", code, e.getMessage());
            return Result.FAILED;
        } catch (Exception e) {
            log.warn("FCM send error: {}", e.getMessage());
            return Result.FAILED;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
