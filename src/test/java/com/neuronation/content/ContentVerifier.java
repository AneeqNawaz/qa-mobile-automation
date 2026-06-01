package com.neuronation.content;

import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContentVerifier {
    private static final Logger log = LoggerFactory.getLogger(ContentVerifier.class);

    public List<String> verify(ContentSnapshot current, ContentSnapshot baseline) {
        List<String> mismatches = new ArrayList<>();
        Map<String, String> currentContent = current.getContent();
        Map<String, String> baselineContent = baseline.getContent();

        for (Map.Entry<String, String> entry : currentContent.entrySet()) {
            String key = entry.getKey();
            String actualValue = entry.getValue();
            String expectedValue = baselineContent.get(key);

            if (expectedValue == null) {
                mismatches.add(String.format("NEW key '%s' = '%s' (not in baseline)", key, actualValue));
            } else if (!actualValue.equals(expectedValue)) {
                mismatches.add(String.format("MISMATCH key '%s': expected='%s', actual='%s'",
                        key, expectedValue, actualValue));
            }
        }

        for (String key : baselineContent.keySet()) {
            if (!currentContent.containsKey(key)) {
                mismatches.add(String.format("MISSING key '%s' (in baseline but not on screen)", key));
            }
        }

        if (!mismatches.isEmpty()) {
            String report = String.join("\n", mismatches);
            log.warn("Content mismatches on {}:\n{}", current.getScreenName(), report);
            try {
                Allure.addAttachment(
                        "Content Mismatches - " + current.getScreenName(),
                        "text/plain", report);
            } catch (Exception ignored) {
                // Allure may not be initialized in unit tests
            }
        }

        return mismatches;
    }
}
