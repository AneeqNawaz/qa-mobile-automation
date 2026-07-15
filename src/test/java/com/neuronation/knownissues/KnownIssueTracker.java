package com.neuronation.knownissues;

import com.neuronation.config.Platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Collects which known issues actually fired (expected-fail) during a run, and flushes the
 * run summary to target/known-issues-report.json at JVM exit for the Jenkins/Slack step.
 * Recording never affects build status — that is the {@link KnownIssueAction} FAIL path's job.
 */
public final class KnownIssueTracker {

    static final String REPORT_PATH = "target/known-issues-report.json";

    private static final Set<String> ENCOUNTERED = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean HOOK_INSTALLED = new AtomicBoolean(false);

    private KnownIssueTracker() {}

    /** Note that a known issue fired this run (id only; deduplicated). */
    public static void record(KnownIssue ki) {
        ENCOUNTERED.add(ki.getId());
    }

    public static Set<String> encountered() {
        return Set.copyOf(ENCOUNTERED);
    }

    static void reset() {
        ENCOUNTERED.clear();
    }

    /** Register the once-per-JVM shutdown hook that writes the run summary. Called from the test
     *  base (not from {@link #record}) so unit tests don't spawn hooks or write files. */
    public static void enableReportOnExit() {
        if (HOOK_INSTALLED.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(KnownIssueTracker::writeReport, "known-issue-report"));
        }
    }

    static void writeReport() {
        try {
            Platform platform = currentPlatform();
            KnownIssueRegistry registry = KnownIssueRegistry.load();
            var active = registry.all().stream().filter(ki -> ki.matchesPlatform(platform)).toList();
            KnownIssueReport report = KnownIssueReport.build(active, encountered(), LocalDate.now());
            Path path = Paths.get(REPORT_PATH);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.writeString(path, report.toJson());
        } catch (Exception ignored) {
            // Reporting must never fail the run.
        }
    }

    private static Platform currentPlatform() {
        String p = System.getProperty("platform");
        try {
            return p == null ? null : Platform.fromString(p);
        } catch (Exception e) {
            return null;
        }
    }
}
