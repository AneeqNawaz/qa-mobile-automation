package com.neuronation.api;

import com.neuronation.config.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class ApiClient {

    public static RequestSpecification baseRequest() {
        String baseUrl = ConfigManager.getInstance().getString("api.base.url");
        return RestAssured.given()
                .baseUri(baseUrl)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    public static RequestSpecification authenticatedRequest(String accessToken) {
        return baseRequest()
                .header("Authorization", "Bearer " + accessToken);
    }
}
