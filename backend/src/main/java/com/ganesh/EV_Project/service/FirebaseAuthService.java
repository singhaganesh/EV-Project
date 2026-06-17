package com.ganesh.EV_Project.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Verifies Firebase ID tokens produced by the mobile client's Phone Auth flow.
 *
 * The client signs the user in with Firebase (SMS OTP handled by Google), then
 * sends the resulting ID token to {@code POST /api/auth/firebase-login}. This
 * service verifies the token's signature/expiry with the Firebase Admin SDK and
 * extracts the verified phone number, which the controller exchanges for our own
 * local JWTs. Relies on the default app initialized by {@code FirebaseConfig}.
 */
@Service
@Slf4j
public class FirebaseAuthService {

    public boolean isEnabled() {
        return !FirebaseApp.getApps().isEmpty();
    }

    /**
     * Verifies the given Firebase ID token and returns the verified E.164 phone
     * number (e.g. {@code +919876543210}), or {@code null} if the token carries
     * no phone-number claim.
     *
     * @throws FirebaseAuthException if the token is invalid, expired or revoked
     * @throws IllegalStateException if Firebase Admin is not initialized
     */
    public String verifyAndGetPhoneNumber(String idToken) throws FirebaseAuthException {
        if (!isEnabled()) {
            throw new IllegalStateException("Firebase Admin is not initialized");
        }
        FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
        Object phone = decoded.getClaims().get("phone_number");
        return phone == null ? null : phone.toString();
    }
}
