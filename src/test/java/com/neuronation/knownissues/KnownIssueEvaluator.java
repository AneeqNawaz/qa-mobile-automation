package com.neuronation.knownissues;

import java.util.Optional;

/**
 * Pure decision core of the known-issue quarantine. Given whether an assertion passed and whether
 * a known issue is active for the current platform, decides how the result should be treated.
 * No I/O, no framework coupling — this is the honesty guarantee, so it is unit-tested in isolation.
 */
public final class KnownIssueEvaluator {

    private KnownIssueEvaluator() {}

    public static KnownIssueDecision decide(boolean passed, Optional<KnownIssue> active) {
        if (active.isEmpty()) {
            return passed ? KnownIssueDecision.NORMAL_PASS : KnownIssueDecision.REGRESSION;
        }
        return passed ? KnownIssueDecision.UNEXPECTED_PASS : KnownIssueDecision.KNOWN_FAIL;
    }
}
