package com.neuronation.context;

public class TestContext {
    private static final ThreadLocal<TestContext> INSTANCE = new ThreadLocal<>();

    private String authToken;
    private String userId;
    private int variantId;
    private String language;
    private String email;
    private String password;
    private String appType;
    private String platform;

    public static void set(TestContext ctx) { INSTANCE.set(ctx); }
    public static TestContext get() { return INSTANCE.get(); }
    public static void remove() { INSTANCE.remove(); }

    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getVariantId() { return variantId; }
    public void setVariantId(int variantId) { this.variantId = variantId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAppType() { return appType; }
    public void setAppType(String appType) { this.appType = appType; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}
