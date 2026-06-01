package com.neuronation.api.model;

public class RegistrationResponse {
    private String accessToken;
    private String userId;

    public RegistrationResponse() {}

    public RegistrationResponse(String accessToken, String userId) {
        this.accessToken = accessToken;
        this.userId = userId;
    }

    public String getAccessToken() { return accessToken; }
    public String getUserId() { return userId; }
}
