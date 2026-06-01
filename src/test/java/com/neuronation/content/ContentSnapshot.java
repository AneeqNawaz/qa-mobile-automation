package com.neuronation.content;

import java.util.LinkedHashMap;
import java.util.Map;

public class ContentSnapshot {
    private final String screenName;
    private final Map<String, String> content;

    public ContentSnapshot(String screenName, Map<String, String> content) {
        this.screenName = screenName;
        this.content = new LinkedHashMap<>(content);
    }

    public String getScreenName() { return screenName; }
    public Map<String, String> getContent() { return content; }
}
