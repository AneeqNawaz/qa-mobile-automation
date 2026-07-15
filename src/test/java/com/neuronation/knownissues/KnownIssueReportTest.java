package com.neuronation.knownissues;

import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * The run-summary builder: turns the active registry entries + the ids that actually fired this
 * run into a machine-readable report (consumed by the Jenkins/Slack summary and the aging report).
 */
public class KnownIssueReportTest {

    private List<KnownIssue> entries() {
        KnownIssueRegistry reg = KnownIssueRegistry.fromJson(
                "{" +
                "\"encountered-one\":{\"jira\":\"https://x/browse/MIBA-1\",\"platform\":\"ios\"}," +
                "\"quiet-one\":{\"jira\":\"https://x/browse/MIBA-2\",\"platform\":\"ios\"}," +
                "\"aging-one\":{\"jira\":\"https://x/browse/MIBA-3\",\"platform\":\"ios\"," +
                "  \"opened\":\"2026-01-01\",\"review\":\"2026-02-01\"}" +
                "}");
        return List.copyOf(reg.all());
    }

    @Test
    public void flagsEncounteredEntries() {
        KnownIssueReport r = KnownIssueReport.build(entries(), Set.of("encountered-one"), LocalDate.of(2026, 3, 1));
        assertEquals(r.encounteredCount(), 1);
        assertTrue(r.encounteredKeys().contains("MIBA-1"));
        assertFalse(r.encounteredKeys().contains("MIBA-2"), "an un-fired entry is not 'encountered'");
    }

    @Test
    public void flagsAgingEntries() {
        KnownIssueReport r = KnownIssueReport.build(entries(), Set.of(), LocalDate.of(2026, 3, 1));
        assertEquals(r.agingKeys(), List.of("MIBA-3"), "only the entry past its review date is aging");
    }

    @Test
    public void listsEveryActiveEntry() {
        KnownIssueReport r = KnownIssueReport.build(entries(), Set.of(), LocalDate.of(2026, 1, 1));
        assertEquals(r.total(), 3, "report should list every active known issue, fired or not");
    }

    @Test
    public void jsonRoundTripsKeys() {
        KnownIssueReport r = KnownIssueReport.build(entries(), Set.of("encountered-one"), LocalDate.of(2026, 3, 1));
        String json = r.toJson();
        assertTrue(json.contains("MIBA-1"), json);
        assertTrue(json.contains("encounteredCount"), json);
    }
}
