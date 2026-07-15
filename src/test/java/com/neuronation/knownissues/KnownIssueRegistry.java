package com.neuronation.knownissues;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.neuronation.config.Platform;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads and queries the known-issue registry (testdata/med/known-issues.json). The single source
 * of truth for which assertions are currently quarantined; toggling a known issue is a one-line
 * edit to that file, never a code change. A missing file simply means "no known issues".
 */
public final class KnownIssueRegistry {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, KnownIssue>>() {}.getType();
    static final String PATH = "testdata/med/known-issues.json";

    private final Map<String, KnownIssue> issues;

    private KnownIssueRegistry(Map<String, KnownIssue> issues) {
        this.issues = issues;
    }

    public static KnownIssueRegistry fromJson(String json) {
        Map<String, KnownIssue> parsed = GSON.fromJson(json, MAP_TYPE);
        Map<String, KnownIssue> map = new LinkedHashMap<>();
        if (parsed != null) {
            for (Map.Entry<String, KnownIssue> e : parsed.entrySet()) {
                KnownIssue ki = e.getValue();
                ki.setId(e.getKey());   // Gson doesn't map the key onto the value
                map.put(e.getKey(), ki);
            }
        }
        return new KnownIssueRegistry(map);
    }

    public static KnownIssueRegistry load() {
        try (InputStream is = KnownIssueRegistry.class.getClassLoader().getResourceAsStream(PATH)) {
            if (is == null) return new KnownIssueRegistry(new LinkedHashMap<>()); // no file → no known issues
            return fromJson(new String(is.readAllBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load known-issue registry from " + PATH, e);
        }
    }

    /** The known issue registered under {@code id}, but only if its platform scope matches the
     *  current run platform. Empty means "assert normally" (no active quarantine). */
    public Optional<KnownIssue> active(String id, Platform current) {
        KnownIssue ki = issues.get(id);
        if (ki != null && ki.matchesPlatform(current)) return Optional.of(ki);
        return Optional.empty();
    }

    /** Every registered entry, for the run summary / aging report. */
    public Collection<KnownIssue> all() {
        return issues.values();
    }
}
