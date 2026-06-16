# Functional Specification: Custom OTP Login & Registration Hardening

This document outlines the business rules, security constraints, and user experience requirements for reforming the mobile app's OTP login and registration flow. Use this specification to guide implementation.

---

## 1. OTP Delivery & Broker Configuration

### A. Chosen SMS Gateway: Firebase Authentication (Phone Auth)
* **Requirement:** The application must strictly use **Firebase Authentication (Phone Auth)** as the message broker for dispatching and validating SMS OTPs.
* **Reasoning:** Leverages Google's infrastructure to handle SMS delivery, auto-retrieval, and basic phone verification at zero cost (under 10,000 monthly verifications) without requiring a separate third-party SMS API subscription (like Twilio or MSG91).
* **Delivery:** The mobile client will trigger SMS verification directly via the Firebase SDK.

---

## 2. Authentication Architecture & Token Exchange

### A. Firebase and Backend Integration
To maintain a custom database and local security parameters, the authentication flow must follow a token exchange architecture:
1. **Request SMS:** Mobile client prompts Firebase SDK to send an SMS verification code to the phone number.
2. **Verify Code:** User enters the code (or it is auto-filled), and the Firebase SDK validates it.
3. **Retrieve ID Token:** Upon successful validation, the Firebase SDK returns a cryptographically signed **Firebase ID Token** (JWT).
4. **Backend Handshake:** The mobile client sends this **Firebase ID Token** to our Spring Boot backend (via `POST /api/auth/firebase-login`).
5. **Backend Verification:** The backend verifies the token's validity using the **Firebase Admin SDK**.
6. **Local Token Issue:** Once verified, the backend checks if the user exists (creates a stub account if new) and issues our local, secure access/refresh JWT tokens.

---

## 3. Android App UX Requirements

### A. Remove Debug UI Elements
* **Requirement:** Completely remove any debug UI components or text cards that display the generated OTP code on the login screen.

### B. Segmented OTP Input Fields
* **Requirement:** Do not use a single, generic text input field for entering the OTP.
* **UX Guideline:** Implement a segmented 6-digit input view composed of 6 separate, aligned boxes. As the user enters a digit, the cursor focus must automatically shift to the next box. If the user presses backspace, the cursor must delete and jump back to the previous box.

### C. Resend Countdown Timer
* **Requirement:** Prevent immediate or consecutive taps on the "Resend OTP" button.
* **UX Guideline:** Implement a **30-second to 60-second countdown timer** immediately after the OTP is sent. The "Resend OTP" button must be disabled and display the ticking timer (e.g., *"Resend in 24s"*). Enable the button only when the timer reaches zero.

### D. Mobile Input Validation
* **Requirement:** Add pre-submission validation on the mobile number field.
* **UX Guideline:** Add a country code prefix selector (defaulting to the target region, e.g. `+91`) and use strict regex format validation (e.g., checking for exactly 10 digits starting with valid mobile digits) before enabling the "Send OTP" button.

### E. SMS Auto-Verification (SmsRetriever API / Instant Verification)
* **Requirement:** Leverage Firebase's built-in support for instant verification and SMS auto-retrieval.
* **UX Guideline:** When the SMS containing the OTP arrives, the app must automatically detect the SMS, extract the 6-digit code, and automatically submit it without requiring the driver to manually copy it or open their messaging app.
