package com.neuronation.config;

public enum AppType {
    MED("nn.mobile.app.med", "com.neuronation.med.MainActivity"),
    BT("nn.mobile.app.bt", "com.neuronation.bt.MainActivity");

    private final String appPackage;
    private final String appActivity;

    AppType(String appPackage, String appActivity) {
        this.appPackage = appPackage;
        this.appActivity = appActivity;
    }

    public String getAppPackage() { return appPackage; }
    public String getAppActivity() { return appActivity; }

    public static AppType fromString(String value) {
        return valueOf(value.toUpperCase());
    }
}
