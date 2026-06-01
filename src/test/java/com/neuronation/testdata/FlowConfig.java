package com.neuronation.testdata;

/**
 * Configuration for a single E2E flow. Each flow represents a unique
 * combination of choices throughout the onboarding journey.
 * Loaded from testdata/med-en.json → flows.{flowName}
 */
public class FlowConfig {
    private String description;
    private String authMethod;          // "password", "passkey", "nopassword"
    private String doctorInfo;          // "skip" or "fill"
    private boolean newsletterConsent;
    private String trainingComplexity;  // "activate" or "deactivate"
    private String specialNeeds;        // "standard", "colorVision", "arithmetic", "both"
    private String trainingTime;        // "morning", "noon", "evening", "night"
    private String trainingDays;        // "default" or comma-separated days
    private String notificationPermission; // "allow" or "deny"
    private boolean neuroBooster;
    private boolean promise;
    private boolean dataRetainConsent;
    private boolean dataProcessingConsent;

    public String getDescription() { return description; }
    public String getAuthMethod() { return authMethod; }
    public String getDoctorInfo() { return doctorInfo; }
    public boolean isNewsletterConsent() { return newsletterConsent; }
    public String getTrainingComplexity() { return trainingComplexity; }
    public String getSpecialNeeds() { return specialNeeds; }
    public String getTrainingTime() { return trainingTime; }
    public String getTrainingDays() { return trainingDays; }
    public String getNotificationPermission() { return notificationPermission; }
    public boolean isNeuroBooster() { return neuroBooster; }
    public boolean isPromise() { return promise; }
    public boolean isDataRetainConsent() { return dataRetainConsent; }
    public boolean isDataProcessingConsent() { return dataProcessingConsent; }
}
