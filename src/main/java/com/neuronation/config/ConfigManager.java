package com.neuronation.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Thread-safe configuration manager.
 * Supports parallel test execution — each thread gets its own config
 * based on the config.profile parameter passed from TestNG suite XML.
 *
 * Usage:
 *   ConfigManager.initForThread("med-ios");  // in setUp
 *   ConfigManager.getInstance();              // anywhere — returns thread's config
 *   ConfigManager.removeThread();             // in tearDown
 */
public class ConfigManager {
    private static final ThreadLocal<ConfigManager> threadInstance = new ThreadLocal<>();
    private static ConfigManager globalInstance;
    private final Properties properties;

    private ConfigManager(String profile) {
        properties = new Properties();
        loadProperties("config.properties");

        if (profile != null && !profile.isEmpty()) {
            loadProperties("config-" + profile + ".properties");
        } else {
            // Check system property
            String sysProfile = System.getProperty("config.profile");
            if (sysProfile != null && !sysProfile.isEmpty()) {
                loadProperties("config-" + sysProfile + ".properties");
            } else {
                // Derive from app.type + platform
                String appType = resolveProperty("app.type");
                String platform = resolveProperty("platform");
                if (appType != null && platform != null) {
                    String overlay = String.format("config-%s-%s.properties",
                            appType.toLowerCase(), platform.toLowerCase());
                    loadProperties(overlay);
                }
            }
        }
    }

    private void loadProperties(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + filename, e);
        }
    }

    private String resolveProperty(String key) {
        String sysVal = System.getProperty(key);
        if (sysVal != null && !sysVal.isEmpty()) {
            return sysVal;
        }
        return properties.getProperty(key);
    }

    /**
     * Initialize config for the current thread with a specific profile.
     * Call this in BaseTest.setUp() before anything else.
     */
    public static void initForThread(String profile) {
        threadInstance.set(new ConfigManager(profile));
    }

    /**
     * Remove thread-local config. Call in BaseTest.tearDown().
     */
    public static void removeThread() {
        threadInstance.remove();
    }

    /**
     * Get the config instance for the current thread.
     * Falls back to global singleton if no thread-local instance.
     */
    public static ConfigManager getInstance() {
        ConfigManager local = threadInstance.get();
        if (local != null) return local;

        // Fallback: global singleton (single-threaded mode)
        if (globalInstance == null) {
            synchronized (ConfigManager.class) {
                if (globalInstance == null) {
                    globalInstance = new ConfigManager(null);
                }
            }
        }
        return globalInstance;
    }

    public static synchronized void reset() {
        globalInstance = null;
        threadInstance.remove();
    }

    public String getString(String key) {
        String value = resolveProperty(key);
        if (value != null && value.startsWith("${") && value.endsWith("}")) {
            String envVar = value.substring(2, value.length() - 1);
            String envValue = System.getenv(envVar);
            return envValue != null ? envValue : value;
        }
        return value;
    }

    public int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

    public AppType getAppType() {
        return AppType.fromString(getString("app.type"));
    }

    public Platform getPlatform() {
        return Platform.fromString(getString("platform"));
    }
}
