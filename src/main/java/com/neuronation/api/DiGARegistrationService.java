package com.neuronation.api;

import com.neuronation.config.ConfigManager;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * DiGA 7-step API registration flow.
 * Mirrors the Postman collection "DiGA Code Redemption — E2E Tests".
 *
 * Steps:
 * 1. Validate coupon code → couponToken
 * 2. Register user (byte-encoded email/password) → activationToken + validationToken
 * 3. Verify email (GET with validationToken) → email confirmed
 * 4. Activate user → accessToken + userUniqueId + refreshToken
 * 5. Login → accessToken (refreshed)
 * 6. Session validate → sessionToken
 * 7. Get roles → verify role assignment
 */
public class DiGARegistrationService {
    private static final Logger log = LoggerFactory.getLogger(DiGARegistrationService.class);
    private final String apiBaseUrl;

    // Tokens collected during the flow
    private String couponToken;
    private String activationToken;
    private String validationToken;
    private String accessToken;
    private String refreshToken;
    private String userUniqueId;
    private String sessionToken;

    public DiGARegistrationService() {
        this.apiBaseUrl = ConfigManager.getInstance().getString("api.base.url");
    }

    // ──────────────────────────────────────────────
    // Step 1: Validate Coupon
    // ──────────────────────────────────────────────

    @Step("API Step 1: Validate coupon code → get couponToken")
    public String validateCoupon(String couponCode) {
        log.info("Step 1: Validating coupon code: {}", couponCode);

        Response response = RestAssured.given()
                .baseUri(apiBaseUrl)
                .header("Content-Type", "application/json")
                .body(Map.of("couponCode", couponCode))
                .post("/api/v2/coupons/validate");

        response.then().statusCode(201);

        couponToken = response.jsonPath().getString("couponToken");
        log.info("Step 1 complete — couponToken obtained");
        return couponToken;
    }

    // ──────────────────────────────────────────────
    // Step 2: Register User
    // ──────────────────────────────────────────────

    @Step("API Step 2: Register user {email} → get activationToken + validationToken")
    public void registerUser(String email, String password) {
        log.info("Step 2: Registering user: {}", email);

        // Email and password must be byte-encoded as JSON arrays
        byte[] emailBytes = email.getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);

        String emailEncoded = byteArrayToJsonArray(emailBytes);
        String passwordEncoded = byteArrayToJsonArray(passwordBytes);

        String body = String.format(
                "{\"mail\": %s, \"password\": %s, \"dpConsent\": false, \"drConsent\": false, "
                + "\"language\": \"en\", \"deviceLanguage\": \"en\", \"country\": \"US\", "
                + "\"appVersion\": 2001008, \"attribution\": null, \"couponToken\": \"%s\", "
                + "\"osName\": 2, \"osVersion\": \"14 (API-Level: 34)\"}",
                emailEncoded, passwordEncoded, couponToken);

        Response response = RestAssured.given()
                .baseUri(apiBaseUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + couponToken)
                .body(body)
                .post("/api/v2/users/register");

        int status = response.statusCode();
        if (status != 200 && status != 201) {
            log.error("Step 2 failed with status {}: {}", status, response.body().asString());
            throw new RuntimeException("Registration failed with status " + status);
        }

        activationToken = response.jsonPath().getString("activationToken");
        validationToken = response.jsonPath().getString("validationToken");
        log.info("Step 2 complete — activationToken + validationToken obtained");
    }

    // ──────────────────────────────────────────────
    // Step 3: Verify Email
    // ──────────────────────────────────────────────

    @Step("API Step 3: Verify email via validationToken (simulates clicking email link)")
    public void verifyEmail() {
        log.info("Step 3: Verifying email with validationToken");

        Response response = RestAssured.given()
                .baseUri(apiBaseUrl)
                .get("/api/v2/users/validate/" + validationToken);

        response.then().statusCode(200);
        log.info("Step 3 complete — email verified");
    }

    // ──────────────────────────────────────────────
    // Step 4: Activate User
    // ──────────────────────────────────────────────

    @Step("API Step 4: Activate user → get accessToken + userUniqueId")
    public void activateUser() {
        log.info("Step 4: Activating user");

        Response response = RestAssured.given()
                .baseUri(apiBaseUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + activationToken)
                .post("/api/v2/users/activate");

        response.then().statusCode(201);

        accessToken = response.jsonPath().getString("result.accessToken");
        userUniqueId = response.jsonPath().getString("result.userUniqueId");
        refreshToken = response.jsonPath().getString("result.refreshToken");
        log.info("Step 4 complete — user activated, userUniqueId: {}", userUniqueId);
    }

    // ──────────────────────────────────────────────
    // Step 5: Login
    // ──────────────────────────────────────────────

    @Step("API Step 5: Login with {email}")
    public void login(String email, String password) {
        log.info("Step 5: Logging in as: {}", email);

        Response response = RestAssured.given()
                .baseUri(apiBaseUrl)
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", "1.2.3.4")
                .body(Map.of("mail", email, "password", password))
                .post("/api/v2/login");

        response.then().statusCode(200);

        accessToken = response.jsonPath().getString("result.accessToken");
        userUniqueId = response.jsonPath().getString("result.userUniqueId");
        refreshToken = response.jsonPath().getString("result.refreshToken");
        log.info("Step 5 complete — logged in");
    }

    // ──────────────────────────────────────────────
    // Step 6: Session Validate
    // ──────────────────────────────────────────────

    @Step("API Step 6: Validate session → get sessionToken")
    public void validateSession() {
        log.info("Step 6: Validating session");

        Response response = RestAssured.given()
                .baseUri(apiBaseUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .post("/api/v2/validate");

        response.then().statusCode(200);

        sessionToken = response.jsonPath().getString("result.sessionToken");
        log.info("Step 6 complete — sessionToken obtained");
    }

    // ──────────────────────────────────────────────
    // Step 7: Get Roles
    // ──────────────────────────────────────────────

    @Step("API Step 7: Verify user has role {expectedRoleId}")
    public boolean hasRole(int expectedRoleId) {
        log.info("Step 7: Checking roles for user {}", userUniqueId);

        Response response = RestAssured.given()
                .baseUri(apiBaseUrl)
                .header("Authorization", "Bearer " + accessToken)
                .get("/api/v2/users/" + userUniqueId + "/roles");

        response.then().statusCode(200);

        var roles = response.jsonPath().getList("roles.id", Integer.class);
        boolean hasRole = roles != null && roles.contains(expectedRoleId);
        log.info("Step 7 complete — roles: {}, has role {}: {}", roles, expectedRoleId, hasRole);
        return hasRole;
    }

    // ──────────────────────────────────────────────
    // Full flows
    // ──────────────────────────────────────────────

    /**
     * Full 7-step MCI registration: validate → register → verify email →
     * activate → login → session → check role 12.
     */
    @Step("Full MCI registration flow for {email}")
    public void fullMciRegistration(String couponCode, String email, String password) {
        validateCoupon(couponCode);
        registerUser(email, password);
        verifyEmail();
        activateUser();
        login(email, password);
        validateSession();
    }

    /**
     * Verify email only — for use when app handles registration
     * and we just need to confirm the email via API.
     * Requires couponCode to validate, then register with same email to get validationToken.
     */
    @Step("API: Verify email for app-registered user {email}")
    public void verifyEmailForAppUser(String couponCode, String email, String password) {
        validateCoupon(couponCode);
        registerUser(email, password);
        verifyEmail();
    }

    // ──────────────────────────────────────────────
    // Getters
    // ──────────────────────────────────────────────

    public String getCouponToken() { return couponToken; }
    public String getActivationToken() { return activationToken; }
    public String getValidationToken() { return validationToken; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getUserUniqueId() { return userUniqueId; }
    public String getSessionToken() { return sessionToken; }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private String byteArrayToJsonArray(byte[] bytes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(",");
            // Unsigned byte value
            sb.append(bytes[i] & 0xFF);
        }
        sb.append("]");
        return sb.toString();
    }
}
