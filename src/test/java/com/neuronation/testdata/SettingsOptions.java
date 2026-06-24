package com.neuronation.testdata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/**
 * Expected options for the expandable Settings rows (Comparison Group, Training Adaptation,
 * Language) and the Training Priorities domains. Loaded from testdata/med/settings-options.json
 * so the labels are data-driven, not hardcoded in tests.
 */
public class SettingsOptions {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String PATH = "testdata/med/settings-options.json";

    private Map<String, List<String>> expandableRowOptions;
    private List<String> trainingPriorityDomains;

    public static SettingsOptions load() {
        InputStream is = SettingsOptions.class.getClassLoader().getResourceAsStream(PATH);
        if (is == null) throw new RuntimeException("Test data file not found: " + PATH);
        return GSON.fromJson(new InputStreamReader(is), SettingsOptions.class);
    }

    /** Expected options for an expandable row title (e.g. "Comparison Group"). */
    public List<String> optionsFor(String rowTitle) {
        List<String> opts = expandableRowOptions.get(rowTitle);
        if (opts == null) throw new IllegalArgumentException("No options configured for row: " + rowTitle);
        return opts;
    }

    public List<String> trainingPriorityDomains() {
        return trainingPriorityDomains;
    }
}
