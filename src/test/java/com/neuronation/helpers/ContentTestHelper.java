package com.neuronation.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.asserts.SoftAssert;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Data-driven content verification helper.
 * Loads expected text from per-screen JSON files and provides assertion methods.
 *
 * File structure:
 *   testdata/med/content/registration/launch.json
 *   testdata/med/content/onboarding/doctorInfo.json
 *   testdata/med/content/common/dashboard.json
 *
 * Multi-language: "en" loads from content/, "de" loads from content-de/
 * Add a language = copy the content/ folder, translate values, same test code.
 *
 * Usage:
 *   var expected = ContentTestHelper.load("en", "registration", "appSelection");
 *   ContentTestHelper.assertText(actual, expected.get("headline"), "headline", softAssert);
 */
public class ContentTestHelper {
    private static final Logger log = LoggerFactory.getLogger(ContentTestHelper.class);
    private static final Gson gson = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();

    /**
     * Load expected content for a specific screen.
     *
     * @param language "en", "de", etc.
     * @param folder   "registration", "onboarding", or "common"
     * @param screen   screen key matching the JSON filename (e.g., "appSelection", "doctorInfo")
     * @return Map of field name → expected value
     */
    public static Map<String, Object> load(String language, String folder, String screen) {
        String contentDir = "en".equals(language) ? "content" : "content-" + language;
        String file = "testdata/med/" + contentDir + "/" + folder + "/" + screen + ".json";
        InputStream is = ContentTestHelper.class.getClassLoader().getResourceAsStream(file);
        if (is == null) throw new RuntimeException("Content file not found: " + file);
        log.info("Loaded content: {}", file);
        return gson.fromJson(new InputStreamReader(is), MAP_TYPE);
    }

    /** Shortcut for registration screens. */
    public static Map<String, Object> loadRegistration(String language, String screen) {
        return load(language, "registration", screen);
    }

    /** Shortcut for onboarding screens. */
    public static Map<String, Object> loadOnboarding(String language, String screen) {
        return load(language, "onboarding", screen);
    }

    /** Shortcut for common screens (dashboard, profile). */
    public static Map<String, Object> loadCommon(String language, String screen) {
        return load(language, "common", screen);
    }

    /** Assert exact text match. */
    public static void assertText(String actual, Object expected, String field, SoftAssert sa) {
        String exp = String.valueOf(expected);
        sa.assertEquals(actual, exp, field + ": expected '" + exp + "' got '" + actual + "'");
        log.info("[{}] '{}' == '{}'", field, actual, exp);
    }

    /** Assert actual contains expected substring. */
    public static void assertContains(String actual, String substring, String field, SoftAssert sa) {
        sa.assertTrue(actual != null && actual.contains(substring),
                field + " should contain '" + substring + "' but got '" + actual + "'");
    }

    /** Assert actual contains ALL substrings from a list. */
    @SuppressWarnings("unchecked")
    public static void assertContainsAll(String actual, Object expectedList, String field, SoftAssert sa) {
        if (expectedList instanceof List) {
            for (Object item : (List<Object>) expectedList) {
                assertContains(actual, String.valueOf(item), field, sa);
            }
        }
    }

    /** Assert element text is visible (not null, not empty). */
    public static void assertVisible(String actual, String field, SoftAssert sa) {
        sa.assertNotNull(actual, field + " should not be null");
        sa.assertFalse(actual == null || actual.isEmpty(), field + " should not be empty");
    }
}
