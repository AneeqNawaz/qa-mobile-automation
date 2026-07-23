package com.neuronation.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neuronation.testdata.NeuroBoosterCatalog;
import com.neuronation.testdata.NeuroBoosterLabels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Loads and filters the per-condition NeuroBooster catalog + the shared labels
 * file. Mirrors {@link com.neuronation.testdata.ExerciseCatalog}'s classloader/Gson
 * pattern. English loads from {@code content/}, other languages from
 * {@code content-<lang>/}.
 */
public class CatalogProvider {
    private static final Logger log = LoggerFactory.getLogger(CatalogProvider.class);
    private static final Gson GSON = new GsonBuilder().create();

    // ---- parse (unit-testable, no I/O) ----
    public static NeuroBoosterCatalog parse(String json) {
        return GSON.fromJson(json, NeuroBoosterCatalog.class);
    }

    // ---- load from resources ----
    /** condition = "parkinson" | "mci"; language = "en", "de", ... */
    public static NeuroBoosterCatalog load(String language, String condition) {
        String file = extrasDir(language) + "catalog-neurobooster-" + condition.toLowerCase() + ".json";
        return GSON.fromJson(new InputStreamReader(open(file)), NeuroBoosterCatalog.class);
    }

    public static NeuroBoosterLabels labels(String language) {
        String file = extrasDir(language) + "neurobooster-labels.json";
        return GSON.fromJson(new InputStreamReader(open(file)), NeuroBoosterLabels.class);
    }

    // ---- filters ----
    public static List<NeuroBoosterCatalog.Tile> tiles(NeuroBoosterCatalog c, Predicate<NeuroBoosterCatalog.Tile> p) {
        List<NeuroBoosterCatalog.Tile> out = new ArrayList<>();
        if (c == null || c.tiles == null) return out;
        for (NeuroBoosterCatalog.Tile t : c.tiles) if (p.test(t)) out.add(t);
        return out;
    }

    public static List<NeuroBoosterCatalog.Tile> smokeTiles(NeuroBoosterCatalog c) {
        return tiles(c, t -> t.smoke);
    }

    public static List<NeuroBoosterCatalog.Tile> byType(NeuroBoosterCatalog c, String type) {
        return tiles(c, t -> type.equals(t.type));
    }

    /** First smoke tile of a type (representative), or null if none. */
    public static NeuroBoosterCatalog.Tile firstSmokeOfType(NeuroBoosterCatalog c, String type) {
        for (NeuroBoosterCatalog.Tile t : c.tiles) if (t.smoke && type.equals(t.type)) return t;
        return null;
    }

    // ---- internals ----
    private static String extrasDir(String language) {
        String dir = "en".equals(language) ? "content" : "content-" + language;
        return "testdata/med/" + dir + "/extras/";
    }

    private static InputStream open(String file) {
        InputStream is = CatalogProvider.class.getClassLoader().getResourceAsStream(file);
        if (is == null) throw new RuntimeException("Catalog resource not found: " + file);
        log.info("Loaded catalog resource: {}", file);
        return is;
    }
}
