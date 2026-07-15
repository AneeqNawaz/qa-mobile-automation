package com.neuronation.knownissues;

/**
 * Outcome of evaluating one assertion against the known-issue registry.
 *
 * <ul>
 *   <li>{@link #NORMAL_PASS} — no active known issue and the assertion passed. Nothing to record.</li>
 *   <li>{@link #REGRESSION} — no active known issue and the assertion failed. Must fail the build.</li>
 *   <li>{@link #KNOWN_FAIL} — an active known issue and the assertion failed. Expected; must NOT
 *       fail the build (report only).</li>
 *   <li>{@link #UNEXPECTED_PASS} — an active known issue but the assertion passed: the bug appears
 *       fixed. Must fail the build so the entry gets removed from the registry.</li>
 * </ul>
 */
public enum KnownIssueDecision {
    NORMAL_PASS,
    REGRESSION,
    KNOWN_FAIL,
    UNEXPECTED_PASS
}
