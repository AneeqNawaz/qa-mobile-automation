package com.neuronation.testdata;

/**
 * TestNG group constants for feature-based test organization.
 * Use these as @Test(groups = {...}) values for selective execution.
 *
 * Run by feature:  mvn test -Dgroups=registration
 * Run by app:      mvn test -Dgroups=med
 * Run by priority: mvn test -Dgroups=smoke
 * Combine:         mvn test -Dgroups="med,registration"
 */
public final class Features {

    // --- App groups ---
    public static final String MED = "med";
    public static final String BT = "bt";

    // --- Feature groups ---
    public static final String REGISTRATION = "registration";
    public static final String LOGIN = "login";
    public static final String ONBOARDING = "onboarding";
    public static final String DASHBOARD = "dashboard";
    public static final String PROFILE = "profile";
    public static final String PAYWALL = "paywall";
    public static final String GAME = "game";
    public static final String EDGE_CASES = "edge-cases";

    // --- Priority groups ---
    public static final String SMOKE = "smoke";
    public static final String SANITY = "sanity";
    public static final String REGRESSION = "regression";
    public static final String CRITICAL = "critical";

    // --- Test type groups ---
    public static final String CONTENT = "content";
    public static final String NEGATIVE = "negative";
    public static final String NAVIGATION = "navigation";
    public static final String FAQ = "faq";
    public static final String EXTRAS = "extras";

    private Features() {}
}
