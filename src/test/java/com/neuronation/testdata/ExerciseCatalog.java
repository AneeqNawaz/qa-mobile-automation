package com.neuronation.testdata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/**
 * Canonical catalog of the 23 MED training exercises and the subset locked
 * for each special-needs option. Loaded from testdata/med/exercises.json.
 *
 * Names use the app's exact spelling (e.g. "Mathrobatics", "Colour Craze").
 * English only for now; a localized variant is added when German suites run.
 */
public class ExerciseCatalog {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String PATH = "testdata/med/exercises.json";

    private List<String> all;
    private Map<String, List<String>> locked;

    public static ExerciseCatalog load() {
        InputStream is = ExerciseCatalog.class.getClassLoader().getResourceAsStream(PATH);
        if (is == null) throw new RuntimeException("Test data file not found: " + PATH);
        return GSON.fromJson(new InputStreamReader(is), ExerciseCatalog.class);
    }

    public List<String> all() {
        return all;
    }

    public List<String> lockedFor(String specialNeeds) {
        List<String> result = locked.get(specialNeeds);
        if (result == null) throw new IllegalArgumentException("Unknown specialNeeds: " + specialNeeds);
        return result;
    }

    public int expectedAvailable(String specialNeeds) {
        return all.size() - lockedFor(specialNeeds).size();
    }

    public String expectedCountLabel(String specialNeeds) {
        return expectedAvailable(specialNeeds) + "/" + all.size();
    }
}
