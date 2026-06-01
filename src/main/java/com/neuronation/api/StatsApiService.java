package com.neuronation.api;

import com.neuronation.api.model.StatsEntry;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StatsApiService {
    private static final Logger log = LoggerFactory.getLogger(StatsApiService.class);

    @Step("GET /api/v2/users/{userId}/stats — resolve onboarding variant")
    public int getOnboardingVariantId(String accessToken, String userId) {
        log.info("Resolving onboarding variant for user: {}", userId);

        Response response = ApiClient.authenticatedRequest(accessToken)
                .get("/api/v2/users/" + userId + "/stats");

        int status = response.statusCode();
        log.info("Stats API response status: {}", status);

        if (status != 200) {
            String body = response.body().asString();
            log.warn("Stats API non-200 response: {} — {}", status, body.substring(0, Math.min(200, body.length())));
            throw new RuntimeException("Stats API returned " + status);
        }

        // Response might be a direct array or nested under a key
        List<StatsEntry> stats;
        try {
            stats = Arrays.asList(response.as(StatsEntry[].class));
        } catch (Exception e) {
            try {
                stats = response.jsonPath().getList("$", StatsEntry.class);
            } catch (Exception e2) {
                String body = response.body().asString();
                log.warn("Could not parse stats response: {}", body.substring(0, Math.min(300, body.length())));
                stats = Collections.emptyList();
            }
        }

        log.info("Found {} stat entries", stats.size());
        final List<StatsEntry> finalStats = stats;

        return finalStats.stream()
                .filter(s -> s.getStatsType() == 4
                        && s.getStatsId() == 0
                        && s.getStatsComponent() == 0)
                .findFirst()
                .map(s -> {
                    log.info("Onboarding stat found: valueInt={}", s.getValueInt());
                    return s.getValueInt();
                })
                .orElseThrow(() -> {
                    log.warn("Onboarding stat (type=4,id=0,component=0) not found among {} entries", finalStats.size());
                    return new RuntimeException("Onboarding stat not found");
                });
    }
}
