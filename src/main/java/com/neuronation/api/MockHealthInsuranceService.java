package com.neuronation.api;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Mock Health Insurance Management API — generates DiGA activation codes.
 * Base URL: https://hi.bsi.nn-services.de
 * Requires VPN connection to BSI environment.
 */
public class MockHealthInsuranceService {
    private static final Logger log = LoggerFactory.getLogger(MockHealthInsuranceService.class);
    private static final String BASE_URL = "https://hi.bsi.nn-services.de";

    @Step("Generate fresh {digaType} activation code via Mock HI API")
    public String generateCode(String digaType) {
        log.info("Generating {} activation code", digaType);

        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .header("Content-Type", "application/json")
                .body(Map.of("diga", digaType))
                .post("/management/codes");

        response.then().statusCode(201);

        String code = response.jsonPath().getString("code");
        String responseType = response.jsonPath().getString("response_type");
        String digaId = response.jsonPath().getString("diga_id");

        log.info("Generated {} code: {} (diga_id: {}, type: {})", digaType, code, digaId, responseType);
        return code;
    }

    @Step("Generate fresh MCI activation code")
    public String generateMciCode() {
        return generateCode("mci");
    }

    @Step("Generate fresh Parkinson activation code")
    public String generateParkinsonCode() {
        return generateCode("parkinson");
    }
}
