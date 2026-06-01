package com.neuronation.content;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class SnapshotManager {
    private static final Logger log = LoggerFactory.getLogger(SnapshotManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private final String basePath;

    public SnapshotManager(String basePath) {
        this.basePath = basePath;
    }

    public void record(String screenName, String app, String platform,
                        String lang, String variantId, Map<String, String> content) {
        Path filePath = buildPath(screenName, app, platform, lang, variantId);
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, GSON.toJson(content));
            log.info("Recorded snapshot: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to record snapshot: " + filePath, e);
        }
    }

    public ContentSnapshot load(String screenName, String app, String platform,
                                 String lang, String variantId) {
        Path filePath = buildPath(screenName, app, platform, lang, variantId);
        if (!Files.exists(filePath)) {
            log.warn("No snapshot found at: {}", filePath);
            return null;
        }
        try {
            String json = Files.readString(filePath);
            Map<String, String> content = GSON.fromJson(json, MAP_TYPE);
            return new ContentSnapshot(screenName, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load snapshot: " + filePath, e);
        }
    }

    public boolean exists(String screenName, String app, String platform,
                           String lang, String variantId) {
        return Files.exists(buildPath(screenName, app, platform, lang, variantId));
    }

    private Path buildPath(String screenName, String app, String platform,
                            String lang, String variantId) {
        return Paths.get(basePath, app, platform, lang,
                "variant_" + variantId, screenName + ".json");
    }
}
