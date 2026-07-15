package com.neuronation.knownissues;

import com.neuronation.config.Platform;

import java.util.Optional;

/**
 * The concrete action to take for one evaluated assertion, plus any human-facing message. Built by
 * {@link #resolve}; the test base simply applies it (softAssert.fail / record). Keeping message
 * construction and routing here (rather than in the test base) makes them unit-testable.
 */
public final class KnownIssueAction {

    public enum Type { NONE, FAIL, RECORD }

    public final Type type;
    public final String failMessage;   // set when type == FAIL
    public final KnownIssue recorded;  // set when type == RECORD

    private KnownIssueAction(Type type, String failMessage, KnownIssue recorded) {
        this.type = type;
        this.failMessage = failMessage;
        this.recorded = recorded;
    }

    public static KnownIssueAction resolve(boolean passed, Optional<KnownIssue> active,
                                           String id, String message, String failDetail,
                                           Platform platform) {
        switch (KnownIssueEvaluator.decide(passed, active)) {
            case NORMAL_PASS:
                return new KnownIssueAction(Type.NONE, null, null);
            case REGRESSION:
                String detail = (failDetail == null || failDetail.isBlank()) ? "" : " — " + failDetail;
                return new KnownIssueAction(Type.FAIL, message + detail, null);
            case KNOWN_FAIL:
                return new KnownIssueAction(Type.RECORD, null, active.get());
            case UNEXPECTED_PASS:
                KnownIssue ki = active.get();
                // Non-strict entries (inherently inconsistent bugs) are report-only: an unexpected
                // pass does NOT fail the build, it is just recorded.
                if (!ki.isStrict()) {
                    return new KnownIssueAction(Type.RECORD, null, ki);
                }
                String platformName = platform == null ? "?" : platform.name().toLowerCase();
                String fail = "UNEXPECTED PASS: " + ki.jiraKey() + " ('" + id + "') now passes on "
                        + platformName + " — the bug appears fixed. Remove this entry from "
                        + "known-issues.json to re-enable the assertion. (" + message + ")";
                return new KnownIssueAction(Type.FAIL, fail, null);
            default:
                throw new IllegalStateException("Unhandled decision");
        }
    }
}
