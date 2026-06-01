package com.neuronation.config;

public enum Platform {
    ANDROID,
    IOS;

    public static Platform fromString(String value) {
        return valueOf(value.toUpperCase());
    }
}
