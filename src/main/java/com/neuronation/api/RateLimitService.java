package com.neuronation.api;

import com.neuronation.config.ConfigManager;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clears the BSI backend rate-limit restriction so a fresh registration can run.
 *
 * The BSI API blocks by SOURCE IP after frequent hits (e.g. several DiGA code
 * activations in quick succession). {@code DELETE /api/v2/restrict?apikey=…} lifts
 * the block. Called before each flow so every registration starts unthrottled.
 *
 * IMPORTANT (network/IP): the restriction is keyed PER SOURCE IP (confirmed with
 * backend — it is NOT a global clear). This works on the LOCAL EMULATOR because the
 * app and this call egress through the same host IP. It does NOT work on BrowserStack
 * CI: the device egresses from BrowserStack IPs (different from, and varying vs, the
 * CI machine), so a clear from CI lifts the wrong IP. This is a LOCAL-only mechanism.
 *
 * NOTE: the DELETE returns HTTP 404 by design — that is the expected/intentional
 * response and still performs the clear, so we do not treat it as a failure.
 */
public class RateLimitService {
    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    /** Best-effort clear of the BSI rate-limit restriction. Never throws. */
    @Step("Clear BSI rate-limit restriction (DELETE /api/v2/restrict)")
    public static void clearRestriction() {
        ConfigManager cfg = ConfigManager.getInstance();
        String baseUrl = cfg.getString("api.base.url");
        String apiKey = cfg.getString("bsi.restrict.apikey");
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("bsi.restrict.apikey not configured — skipping rate-limit clear");
            return;
        }
        try {
            Response response = RestAssured.given()
                    .baseUri(baseUrl)
                    .queryParam("apikey", apiKey)
                    .delete("/api/v2/restrict");
            // 404 is the intended response for this endpoint — the clear still happens.
            log.info("Rate-limit restriction cleared — DELETE /api/v2/restrict → {} (404 expected)",
                    response.getStatusCode());
        } catch (Exception e) {
            log.warn("Could not clear rate-limit restriction (continuing): {}", e.getMessage());
        }
    }
}
