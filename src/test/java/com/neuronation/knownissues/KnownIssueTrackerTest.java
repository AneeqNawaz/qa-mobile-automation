package com.neuronation.knownissues;

import com.neuronation.config.Platform;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.*;

public class KnownIssueTrackerTest {

    private static KnownIssue issue(String id, String key) {
        return KnownIssueRegistry
                .fromJson("{\"" + id + "\":{\"jira\":\"https://x/browse/" + key + "\",\"platform\":\"ios\"}}")
                .active(id, Platform.IOS).orElseThrow();
    }

    @Test
    public void recordsEncounteredIds() {
        KnownIssueTracker.reset();
        KnownIssueTracker.record(issue("alpha", "MIBA-9"));
        KnownIssueTracker.record(issue("alpha", "MIBA-9")); // idempotent
        KnownIssueTracker.record(issue("beta", "MIBA-8"));
        assertTrue(KnownIssueTracker.encountered().contains("alpha"));
        assertTrue(KnownIssueTracker.encountered().contains("beta"));
        assertEquals(KnownIssueTracker.encountered().size(), 2, "duplicate records collapse to one id");
    }

    @Test
    public void resetClearsState() {
        KnownIssueTracker.record(issue("gamma", "MIBA-7"));
        KnownIssueTracker.reset();
        assertTrue(KnownIssueTracker.encountered().isEmpty());
    }

    /** Exercises the actual flush path against the shipped registry: a fired iOS known issue must
     *  land in target/known-issues-report.json with the right key and encountered count. */
    @Test
    public void writesReportFileForCurrentPlatform() throws Exception {
        String prev = System.getProperty("platform");
        System.setProperty("platform", "ios");
        try {
            KnownIssueTracker.reset();
            KnownIssue ki = KnownIssueRegistry.load().active("consent-date-format", Platform.IOS).orElseThrow();
            KnownIssueTracker.record(ki);
            KnownIssueTracker.writeReport();

            Path p = Paths.get(KnownIssueTracker.REPORT_PATH);
            assertTrue(Files.exists(p), "report file should be written to " + p);
            String json = Files.readString(p);
            assertTrue(json.contains("MIBA-4277"), json);
            assertTrue(json.replaceAll("\\s", "").contains("\"encounteredCount\":1"), json);
            assertTrue(json.replaceAll("\\s", "").contains("\"encountered\":true"), json);
        } finally {
            KnownIssueTracker.reset();
            if (prev == null) System.clearProperty("platform"); else System.setProperty("platform", prev);
        }
    }
}
