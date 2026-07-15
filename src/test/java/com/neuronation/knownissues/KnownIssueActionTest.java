package com.neuronation.knownissues;

import com.neuronation.config.Platform;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.*;

/**
 * Maps a decision to a concrete action (do nothing / fail the build / record a known issue) and
 * builds the human-facing messages. Pure so the helper wiring in the test base stays trivial glue.
 */
public class KnownIssueActionTest {

    private static KnownIssue iosIssue() {
        return KnownIssueRegistry
                .fromJson("{\"consent-date-format\":{" +
                        "\"jira\":\"https://neuronation.atlassian.net/browse/MIBA-4277\"," +
                        "\"platform\":\"ios\"}}")
                .active("consent-date-format", Platform.IOS)
                .orElseThrow();
    }

    @Test
    public void normalPass_isNoop() {
        KnownIssueAction a = KnownIssueAction.resolve(
                true, Optional.empty(), "x", "msg", null, Platform.ANDROID);
        assertEquals(a.type, KnownIssueAction.Type.NONE);
    }

    @Test
    public void regression_failsWithMessageAndDetail() {
        KnownIssueAction a = KnownIssueAction.resolve(
                false, Optional.empty(), "x", "Reminder should be 09:00", "was 10:00", Platform.ANDROID);
        assertEquals(a.type, KnownIssueAction.Type.FAIL);
        assertTrue(a.failMessage.contains("Reminder should be 09:00"), a.failMessage);
        assertTrue(a.failMessage.contains("was 10:00"), a.failMessage);
    }

    @Test
    public void knownFail_recordsTheIssue_doesNotFail() {
        KnownIssue ki = iosIssue();
        KnownIssueAction a = KnownIssueAction.resolve(
                false, Optional.of(ki), "consent-date-format", "msg", "detail", Platform.IOS);
        assertEquals(a.type, KnownIssueAction.Type.RECORD);
        assertSame(a.recorded, ki);
    }

    @Test
    public void unexpectedPass_nonStrict_isReportOnly() {
        KnownIssue ki = KnownIssueRegistry
                .fromJson("{\"c\":{\"jira\":\"https://x/browse/MIBA-4277\",\"platform\":\"ios\",\"strict\":false}}")
                .active("c", Platform.IOS).orElseThrow();
        KnownIssueAction a = KnownIssueAction.resolve(true, Optional.of(ki), "c", "msg", null, Platform.IOS);
        assertEquals(a.type, KnownIssueAction.Type.RECORD, "non-strict unexpected pass must NOT fail the build");
    }

    @Test
    public void knownFail_nonStrict_stillRecords() {
        KnownIssue ki = KnownIssueRegistry
                .fromJson("{\"c\":{\"jira\":\"https://x/browse/MIBA-4277\",\"platform\":\"ios\",\"strict\":false}}")
                .active("c", Platform.IOS).orElseThrow();
        KnownIssueAction a = KnownIssueAction.resolve(false, Optional.of(ki), "c", "msg", "detail", Platform.IOS);
        assertEquals(a.type, KnownIssueAction.Type.RECORD);
    }

    @Test
    public void unexpectedPass_failsAndTellsYouToRemoveTheEntry() {
        KnownIssueAction a = KnownIssueAction.resolve(
                true, Optional.of(iosIssue()), "consent-date-format", "msg", null, Platform.IOS);
        assertEquals(a.type, KnownIssueAction.Type.FAIL);
        assertTrue(a.failMessage.contains("UNEXPECTED PASS"), a.failMessage);
        assertTrue(a.failMessage.contains("MIBA-4277"), a.failMessage);
        assertTrue(a.failMessage.contains("consent-date-format"), a.failMessage);
        assertTrue(a.failMessage.contains("known-issues.json"), a.failMessage);
    }
}
