package com.neuronation.knownissues;

import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;

/**
 * The decision matrix — the part that must be bulletproof, because it is what keeps the mechanism
 * honest (a quarantined assertion that starts passing must turn the build red, not stay hidden).
 */
public class KnownIssueEvaluatorTest {

    private static final Optional<KnownIssue> ACTIVE = Optional.of(new KnownIssue());
    private static final Optional<KnownIssue> NONE = Optional.empty();

    @Test
    public void noKnownIssue_passing_isNormalPass() {
        assertEquals(KnownIssueEvaluator.decide(true, NONE), KnownIssueDecision.NORMAL_PASS);
    }

    @Test
    public void noKnownIssue_failing_isRegression() {
        assertEquals(KnownIssueEvaluator.decide(false, NONE), KnownIssueDecision.REGRESSION);
    }

    @Test
    public void activeKnownIssue_failing_isKnownFail() {
        assertEquals(KnownIssueEvaluator.decide(false, ACTIVE), KnownIssueDecision.KNOWN_FAIL);
    }

    @Test
    public void activeKnownIssue_passing_isUnexpectedPass() {
        assertEquals(KnownIssueEvaluator.decide(true, ACTIVE), KnownIssueDecision.UNEXPECTED_PASS);
    }
}
