package com.neuronation.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.neuronation.config.AppType;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Loads test data from per-feature JSON files under testdata/{app}/.
 *
 * File structure:
 *   testdata/med/registration.json     — email, password, name
 *   testdata/med/activation.json       — DiGA code
 *   testdata/med/doctor.json           — doctor info
 *   testdata/med/profile.json          — age group, expected values
 *   testdata/med/flows.json            — E2E flow configs
 *   testdata/med/screen-content.json   — expected screen text
 *   testdata/med/diga-code-validation.json    — invalid code test data
 *   testdata/med/password-validation.json     — invalid password test data
 *   testdata/med/email-validation.json        — invalid email test data
 */
public class TestDataLoader {
    private static final Gson GSON = new Gson();

    // ──────────────────────────────────────────────
    // Per-feature data loaders
    // ──────────────────────────────────────────────

    public static <T> T loadRegistrationData(AppType app, String lang, Class<T> clazz) {
        return loadFile(app, "registration.json", clazz);
    }

    public static <T> T loadActivationData(AppType app, String lang, Class<T> clazz) {
        JsonObject obj = loadFileAsJson(app, "activation.json");
        resolveEnvVars(obj);
        return GSON.fromJson(obj, clazz);
    }

    public static <T> T loadDoctorData(AppType app, String lang, Class<T> clazz) {
        return loadFile(app, "doctor.json", clazz);
    }

    public static <T> T loadProfileData(AppType app, String lang, Class<T> clazz) {
        return loadFile(app, "profile.json", clazz);
    }

    public static <T> T loadFlowConfig(AppType app, String lang, String flowName, Class<T> clazz) {
        JsonObject flows = loadFileAsJson(app, "flows.json");
        return GSON.fromJson(flows.get(flowName), clazz);
    }

    public static <T> T loadScreenContent(AppType app, String screenName, Class<T> clazz) {
        JsonObject content = loadFileAsJson(app, "screen-content.json");
        return GSON.fromJson(content.get(screenName), clazz);
    }

    // ──────────────────────────────────────────────
    // Validation test data loaders
    // ──────────────────────────────────────────────

    public static JsonObject loadDigaCodeValidation(AppType app) {
        return loadFileAsJson(app, "diga-code-validation.json");
    }

    public static JsonObject loadPasswordValidation(AppType app) {
        return loadFileAsJson(app, "password-validation.json");
    }

    public static JsonObject loadEmailValidation(AppType app) {
        return loadFileAsJson(app, "email-validation.json");
    }

    // ──────────────────────────────────────────────
    // Generic loaders
    // ──────────────────────────────────────────────

    /**
     * Load a JSON file from testdata/{app}/{filename} and deserialize to clazz.
     */
    public static <T> T loadFile(AppType app, String filename, Class<T> clazz) {
        return GSON.fromJson(new InputStreamReader(getStream(app, filename)), clazz);
    }

    /**
     * Load a JSON file as a raw JsonObject for flexible access.
     */
    public static JsonObject loadFileAsJson(AppType app, String filename) {
        return GSON.fromJson(new InputStreamReader(getStream(app, filename)), JsonObject.class);
    }

    private static InputStream getStream(AppType app, String filename) {
        String path = String.format("testdata/%s/%s", app.name().toLowerCase(), filename);
        InputStream is = TestDataLoader.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new RuntimeException("Test data file not found: " + path);
        }
        return is;
    }

    /**
     * Resolves ${ENV_VAR:default} patterns in JSON string values.
     */
    private static void resolveEnvVars(JsonObject obj) {
        if (obj == null) return;
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                String value = entry.getValue().getAsString();
                if (value.startsWith("${") && value.endsWith("}")) {
                    String inner = value.substring(2, value.length() - 1);
                    int colonIdx = inner.indexOf(':');
                    String envVar = colonIdx != -1 ? inner.substring(0, colonIdx) : inner;
                    String defaultVal = colonIdx != -1 ? inner.substring(colonIdx + 1) : "";
                    String envValue = System.getenv(envVar);
                    obj.addProperty(entry.getKey(), envValue != null ? envValue : defaultVal);
                }
            }
        }
    }
}
