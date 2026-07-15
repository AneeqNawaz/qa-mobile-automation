package com.neuronation.tests.med.profile;

import com.neuronation.base.BaseTest;
import com.neuronation.config.AppType;
import com.neuronation.config.ConfigManager;
import com.neuronation.config.Platform;
import com.neuronation.knownissues.KnownIssue;
import com.neuronation.knownissues.KnownIssueAction;
import com.neuronation.knownissues.KnownIssueRegistry;
import com.neuronation.knownissues.KnownIssueTracker;
import com.neuronation.pages.med.profile.ConsentHistoryScreen;
import com.neuronation.pages.med.profile.TrainingReminderScreen;
import com.neuronation.testdata.ExerciseCatalog;
import com.neuronation.testdata.FlowConfig;
import com.neuronation.testdata.ProfileData;
import com.neuronation.testdata.SettingsOptions;
import com.neuronation.utils.TestDataLoader;
import io.qameta.allure.Allure;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared verification logic for the MED Settings / Privacy / Consent screens, reused by the
 * full E2E happy path (MedFullE2EHappyPathTest) and the standalone read-only checks
 * (MedSettingsVerificationTest). Every step is soft-asserted via {@link #step} so one broken
 * step never halts the rest. No @Test methods here — abstract so TestNG doesn't run it directly.
 */
public abstract class MedSettingsVerifierBase extends BaseTest {

    protected final SettingsOptions settingsOptions = SettingsOptions.load();
    protected final KnownIssueRegistry knownIssues = KnownIssueRegistry.load();

    protected static final long CONSENT_TIME_TOLERANCE_MIN = 3;
    protected static final DateTimeFormatter CONSENT_TS_FORMAT =
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.US);
    // ICU uses U+202F before AM/PM — allow unicode spaces.
    protected static final Pattern CONSENT_TS_PATTERN = Pattern.compile(
            "(\\d{1,2}/\\d{1,2}/\\d{4}[\\s\\u00A0\\u202F]+\\d{1,2}:\\d{2}:\\d{2}[\\s\\u00A0\\u202F]+[AP]M)");
    // iOS renders the consent timestamp as dd.MM.yyyy HH:mm:ss (24h, no AM/PM).
    protected static final DateTimeFormatter CONSENT_TS_FORMAT_IOS =
            DateTimeFormatter.ofPattern("d.M.yyyy H:mm:ss", Locale.US);
    protected static final Pattern CONSENT_TS_PATTERN_IOS = Pattern.compile(
            "(\\d{1,2}\\.\\d{1,2}\\.\\d{4}[\\s\\u00A0\\u202F]+\\d{1,2}:\\d{2}:\\d{2})");

    /** Run one verification step. A thrown exception (navigation/read failure) is recorded as a
     *  soft failure so the run continues to the next step instead of halting on a single issue. */
    protected void step(String label, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("[step] '{}' threw, continuing: {}", label, e.toString());
            softAssert.fail("Step '" + label + "' failed: " + e);
        }
    }

    /**
     * Soft-assert {@code actual == expected}, but quarantine the failure if a known issue is
     * registered for {@code id} on the current platform (see testdata/med/known-issues.json).
     * A quarantined failure is reported (Allure link to the ticket) but does NOT fail the build;
     * if the assertion instead PASSES while quarantined, the build fails with an "unexpected pass"
     * so the stale entry gets removed. With no active entry this behaves like a normal soft assert.
     */
    protected void knownIssue(String id, Object actual, Object expected, String message) {
        applyKnownIssue(id, Objects.equals(actual, expected), message,
                "expected <" + expected + "> but was <" + actual + ">");
    }

    /** assertTrue variant of {@link #knownIssue(String, Object, Object, String)}. */
    protected void knownIssue(String id, boolean condition, String message) {
        applyKnownIssue(id, condition, message, null);
    }

    private void applyKnownIssue(String id, boolean passed, String message, String failDetail) {
        Platform platform = ConfigManager.getInstance().getPlatform();
        Optional<KnownIssue> active = knownIssues.active(id, platform);
        KnownIssueAction action = KnownIssueAction.resolve(passed, active, id, message, failDetail, platform);
        switch (action.type) {
            case NONE:
                break;
            case FAIL:
                softAssert.fail(action.failMessage);
                break;
            case RECORD:
                KnownIssue ki = action.recorded;
                log.warn("[known-issue] {} ({}) expected-fail on {}: {}",
                        ki.jiraKey(), id, platform.name().toLowerCase(), message);
                Allure.link(ki.jiraKey(), ki.getJira());
                Allure.step("🟡 Known issue " + ki.jiraKey() + " (expected fail): " + message);
                KnownIssueTracker.record(ki);
                KnownIssueTracker.enableReportOnExit();
                break;
        }
    }

    /**
     * Steps 0–10: onboarding Schedule Review per-day times, then every Settings row in
     * on-screen order, then Privacy Settings and Consent History. Assumes completeFullFlow has
     * already run (so the dashboard is showing and medFlow.getFlowConfig() is populated). Does
     * NOT log out — the caller decides what happens next.
     */
    protected void verifyOnboardingSettingsPrivacyConsent(String flowName, LocalDateTime flowStart) {
        verifyOnboardingSettingsPrivacyConsent(flowName, flowStart, true);
    }

    /**
     * @param includeOnboardingOnly when {@code false} (a logged-in-only run, no onboarding this
     *   session) skip the onboarding Schedule-Review check (step 0) and the consent-timestamp
     *   freshness check — the account was created earlier so "now" isn't a valid reference.
     *   Settings / Privacy / Consent VALUES are still verified. The "Open Settings" step starts
     *   from the Dashboard, so the app must already be logged in.
     */
    protected void verifyOnboardingSettingsPrivacyConsent(String flowName, LocalDateTime flowStart,
                                                          boolean includeOnboardingOnly) {
        ExerciseCatalog catalog = ExerciseCatalog.load();
        FlowConfig flow = medFlow.getFlowConfig();
        ProfileData profileData = TestDataLoader.loadProfileData(
                AppType.MED, context.getLanguage(), ProfileData.class);
        String needs = flow.getSpecialNeeds();

        // 0. Onboarding Schedule Review per-day times (captured during onboarding) — onboarding only.
        if (includeOnboardingOnly) {
            step("0. Onboarding Schedule Review per-day times", () -> {
                Map<String, String> sched = medFlow.getScheduleReviewTimes();
                String expTime = slotToReminderTime(flow.getTrainingTime());
                log.info("[{}] 0. Onboarding Schedule Review expected={} actual={}", flowName, expTime, sched);
                softAssert.assertFalse(sched.isEmpty(),
                        "Schedule Review should have shown per-day times during onboarding");
                for (Map.Entry<String, String> e : sched.entrySet()) {
                    softAssert.assertEquals(e.getValue(), expTime,
                            "Onboarding Schedule Review " + e.getKey() + " should be " + expTime);
                }
            });
        }

        step("Open Settings", () -> {
            screens.dashboard().tapProfileTab();
            screens.profile().waitForScreen();
            screens.profile().tapSettings();
            screens.settings().waitForScreen();
        });

        step("1. Special needs", () -> {
            boolean expectCv = "colorVision".equals(needs) || "both".equals(needs);
            boolean expectAr = "arithmetic".equals(needs) || "both".equals(needs);
            boolean cv = screens.settings().isColorVisionEnabled();
            boolean ar = screens.settings().isArithmeticEnabled();
            log.info("[{}] 1. Special needs — ColorVision exp={} act={} | Arithmetic exp={} act={}",
                    flowName, expectCv, cv, expectAr, ar);
            softAssert.assertEquals(cv, expectCv, "Color Vision switch should match specialNeeds=" + needs);
            softAssert.assertEquals(ar, expectAr, "Arithmetic switch should match specialNeeds=" + needs);
            screens.settings().collapseSpecialNeeds();
        });

        step("2. Available Exercises — count, locked set, count↔checkbox consistency", () -> {
            String expCount = catalog.expectedCountLabel(needs);
            String actCount = screens.settings().getAvailableExercises();
            log.info("[{}] 2. Available Exercises count exp={} act={}", flowName, expCount, actCount);
            softAssert.assertEquals(actCount, expCount, "Available Exercises count should match specialNeeds=" + needs);

            // Both platforms: expand, read each exercise's name + toggle (iOS pairs each game text to
            // its same-row Switch by Y once truly expanded — verified on device), assert the unchecked
            // set is exactly the locked set and the X/Y count is internally consistent, then collapse.
            Map<String, Boolean> states = screens.settings().getExerciseStates(catalog.all());
            Set<String> actLocked = new java.util.LinkedHashSet<>();
            long checkedCount = 0;
            for (Map.Entry<String, Boolean> e : states.entrySet()) {
                if (Boolean.TRUE.equals(e.getValue())) checkedCount++;
                else actLocked.add(e.getKey());
            }
            Set<String> expLocked = new java.util.LinkedHashSet<>(catalog.lockedFor(needs));
            log.info("[{}] 2. locked exp={} act={} | checked={}/{}", flowName, expLocked, actLocked, checkedCount, states.size());
            softAssert.assertEquals(actLocked, expLocked,
                    "Unchecked exercises should be exactly the locked set (only those games, no others)");

            String[] parts = actCount.split("/");
            if (parts.length == 2 && parts[0].matches("\\d+") && parts[1].matches("\\d+")) {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                softAssert.assertEquals(y, catalog.all().size(),
                        "Count denominator should be the total number of exercises (" + catalog.all().size() + ")");
                softAssert.assertEquals((long) x, checkedCount,
                        "Count numerator (" + x + ") should equal the number of checked exercises (" + checkedCount + ")");
            } else {
                softAssert.fail("Available Exercises count is not in X/Y format: " + actCount);
            }
            screens.settings().collapseAvailableExercises();
        });

        step("3. Comparison Group (age) — open, verify options, collapse", () ->
                verifyExpandableRow(flowName, "Comparison Group", profileData.getAgeGroup()));

        step("4. Training Priorities — open, (popup if shown) Understood → domains, collapse", () -> {
            String act = screens.settings().getTrainingPriorities();
            log.info("[{}] 4. Training Priorities subtitle exp=Recommended act={}", flowName, act);
            softAssert.assertEquals(act, "Recommended",
                    "Training Priorities should default to 'Recommended' for a new account");
            try {
                screens.settings().openTrainingPriorities();
                if (screens.settings().isAttentionPopupShown()) {
                    log.info("[{}] 4. Attention popup msg='{}'", flowName, screens.settings().getAttentionMessage());
                    screens.settings().tapUnderstood();
                }
                List<String> domains = screens.settings().getTrainingPriorityDomains();
                log.info("[{}] 4. Training Priorities domains {}", flowName, domains);
                softAssert.assertTrue(domains.containsAll(settingsOptions.trainingPriorityDomains()),
                        "Training Priorities should show domains " + settingsOptions.trainingPriorityDomains()
                                + ", got " + domains);
            } finally {
                screens.settings().collapseTrainingPriorities();
            }
        });

        step("5. Language — open, verify options, collapse", () ->
                verifyExpandableRow(flowName, "Language", expectedLanguageLabel(context.getLanguage())));

        step("6. Training Adaptation — open, verify options, collapse", () -> {
            String exp = "deactivate".equals(flow.getTrainingComplexity()) ? "Don't ask me" : "Ask me";
            verifyExpandableRow(flowName, "Training Adaptation", exp);
        });

        step("7. Training Reminder", () -> {
            screens.settings().tapTrainingReminder();
            screens.trainingReminder().waitForScreen();
            Map<String, String> times = screens.trainingReminder().getAllReminderTimes();
            boolean personalisedOn = screens.trainingReminder().isPersonalisedTimesOn();
            boolean notificationsAllowed = !"deny".equals(flow.getNotificationPermission());
            String expTime = slotToReminderTime(flow.getTrainingTime());
            log.info("[{}] 7. Training Reminder slot={} notificationsAllowed={} expected={} actual={} personalisedOn={}",
                    flowName, flow.getTrainingTime(), notificationsAllowed, expTime, times, personalisedOn);
            if (notificationsAllowed) {
                // Permission granted → reminder ON, every day shows the slot time. Correct on both
                // platforms (verified iOS build #66) — asserted normally.
                for (String day : TrainingReminderScreen.DAYS) {
                    softAssert.assertEquals(times.get(day), expTime,
                            day + " reminder should be " + expTime + " for slot " + flow.getTrainingTime());
                }
                softAssert.assertTrue(personalisedOn,
                        "'Personalised training times' should be ON when notifications are allowed");
            } else if (!medFlow.wasNotificationDenyApplied()) {
                // Deny was requested but the OS never prompted this run — the permission was already
                // decided on this install (common on a persistent local device; fresh installs and
                // BrowserStack DO prompt, where the deny path below runs). We cannot force a deny
                // here, so DON'T assert the denied state (that would be a false failure). Log the
                // actual reminder state for visibility and move on.
                log.warn("[{}] 7. Notification deny could NOT be applied (OS did not prompt — permission "
                        + "already decided on this install). Skipping deny-state assertions. "
                        + "Actual: personalisedOn={} times={}", flowName, personalisedOn, times);
            } else {
                // Permission DENIED → the app turns the reminder OFF and shows no per-day times
                // (only the dropdown arrow). This is specific to denied permission: manually
                // toggling off with permission granted keeps the times visible.
                softAssert.assertFalse(personalisedOn,
                        "'Personalised training times' should be OFF when notification permission was denied");
                // Day rows are still present but show no time (only the arrow) → all values blank.
                boolean anyTimeShown = times.values().stream()
                        .anyMatch(v -> v != null && !v.trim().isEmpty());
                // Known issue on iOS (MIBA-4280): when notifications are denied, iOS leaves stale
                // per-day times lingering instead of clearing them (Android clears them, so this
                // asserts normally there). Verified in build #66 (iOS showed 22:59 on all 7 days).
                // When MIBA-4280 is fixed: delete the known-issues.json entry, then uncomment the
                // original below and remove the knownIssue() call.
                // softAssert.assertFalse(anyTimeShown,
                //         "No per-day reminder times should show when notification permission was denied, but found: " + times);
                knownIssue("reminder-permission-config", !anyTimeShown,
                        "No per-day reminder times should show when notification permission was denied, but found: " + times);
            }
            screens.trainingReminder().tapBack();
        });

        step("8. NeuroBooster", () -> {
            log.info("[{}] 8. NeuroBooster exp={}", flowName, flow.isNeuroBooster());
            boolean notifDenied = "deny".equals(flow.getNotificationPermission());
            // MIBA-4281 only manifests when the user agreed to NB on its screen but then cancelled
            // the OS notification prompt (i.e. the notification-denied flow): iOS forces the toggle
            // OFF while Android keeps the user's choice. Verified build #66 (iOS flow3 read value=0
            // for neuroBooster:true). In every notifications-allowed flow the toggle is correct on
            // both platforms, so only quarantine the denied case; assert normally otherwise.
            // When MIBA-4281 is fixed: delete the known-issues.json entry, then replace the whole
            // if/else below with the single original assertEquals.
            if (notifDenied) {
                // original: softAssert.assertEquals(isNeuroBoosterEnabled(), flow.isNeuroBooster(), msg);
                knownIssue("neurobooster-cancel-toggle",
                        screens.settings().isNeuroBoosterEnabled(), flow.isNeuroBooster(),
                        "NeuroBooster switch should match flow.neuroBooster");
            } else {
                softAssert.assertEquals(screens.settings().isNeuroBoosterEnabled(), flow.isNeuroBooster(),
                        "NeuroBooster switch should match flow.neuroBooster");
            }
        });

        step("9. Privacy Settings", () -> {
            screens.profile().waitForScreen();
            screens.profile().tapPrivacySettings();
            screens.privacySettings().waitForScreen();
            softAssert.assertTrue(screens.privacySettings().allTogglesPresent(),
                    "Privacy Settings should show all 4 toggles");
            boolean nl = screens.privacySettings().isNewsletterEnabled();
            log.info("[{}] 9. Privacy newsletter toggle exp={} act={}", flowName, flow.isNewsletterConsent(), nl);
            softAssert.assertEquals(nl, flow.isNewsletterConsent(),
                    "Newsletter toggle (4th) should match flow.newsletterConsent");
            screens.privacySettings().tapBack();
        });

        step("10. Consent History", () -> {
            screens.profile().waitForScreen();
            screens.profile().tapConsentHistory();
            screens.consentHistory().waitForScreen();
            screens.consentHistory().waitForEntriesLoaded();
            LocalDateTime ref = includeOnboardingOnly ? flowStart : null; // null → skip freshness check
            verifyConsent(flowName, "Newsletter", ConsentHistoryScreen.NEWSLETTER, flow.isNewsletterConsent(), ref);
            verifyConsent(flowName, "Data retention", ConsentHistoryScreen.DATA_RETENTION, flow.isDataRetainConsent(), ref);
            verifyConsent(flowName, "Data processing", ConsentHistoryScreen.DATA_PROCESSING, flow.isDataProcessingConsent(), ref);
            screens.consentHistory().tapBack();
        });
    }

    /** Verify an expandable single-select Settings row: subtitle shows {@code expectedSelected},
     *  and the expanded list contains every configured option (plus the selected value). The
     *  selection highlight is visual-only, so selection is verified via the subtitle. */
    protected void verifyExpandableRow(String flowName, String rowTitle, String expectedSelected) {
        if (!"android".equals(context.getPlatform())) {
            // iOS: clean expand → verify the selected flag (per flow) → collapse. One value read,
            // two taps — no per-label option queries or scroll-sweep (those are slow/unreliable on
            // iOS, where the full option list isn't dependably exposed). The selected value IS the flag.
            screens.settings().tapSetting(rowTitle);               // expand
            String selected = screens.settings().getSettingValue(rowTitle);
            log.info("[{}] {} selected exp={} act={}", flowName, rowTitle, expectedSelected, selected);
            softAssert.assertEquals(selected, expectedSelected,
                    rowTitle + " selected value should be '" + expectedSelected + "'");
            screens.settings().tapSetting(rowTitle);               // collapse
            return;
        }

        String subtitle = screens.settings().getSettingValue(rowTitle);
        log.info("[{}] {} subtitle exp={} act={}", flowName, rowTitle, expectedSelected, subtitle);
        softAssert.assertEquals(subtitle, expectedSelected,
                rowTitle + " selected value should be '" + expectedSelected + "'");

        List<String> expected = settingsOptions.optionsFor(rowTitle);
        try {
            List<String> present = screens.settings().expandRowAndReadOptions(rowTitle, expected);
            log.info("[{}] {} options exp={} act={}", flowName, rowTitle, expected, present);
            for (String exp : expected) {
                softAssert.assertTrue(present.contains(exp),
                        rowTitle + " expanded list should contain '" + exp + "', got " + present);
            }
            softAssert.assertTrue(present.contains(expectedSelected),
                    rowTitle + " expanded list should include the selected '" + expectedSelected + "'");
        } finally {
            screens.settings().collapseRow(rowTitle);
        }
    }

    /** Assert one Consent History entry shows the expected consent/dissent state and a timestamp
     *  within ±{@value #CONSENT_TIME_TOLERANCE_MIN} min of {@code reference}. */
    protected void verifyConsent(String flowName, String label, String key,
                                 boolean expectedConsent, LocalDateTime reference) {
        String content = screens.consentHistory().getEntryContent(key);
        boolean actualConsent = content.contains("Consent") && !content.contains("Dissent");
        LocalDateTime ts = parseConsentTimestamp(content);

        Long diffFromStart = (ts == null || reference == null) ? null : Math.abs(Duration.between(ts, reference).toMinutes());
        Long diffFromNow = ts == null ? null : Math.abs(Duration.between(ts, LocalDateTime.now()).toMinutes());
        log.info("[{}] Consent {} expected={} actual={} ts={} diffFromFlowStart={}min diffFromNow={}min",
                flowName, label, expectedConsent, actualConsent, ts, diffFromStart, diffFromNow);

        softAssert.assertEquals(actualConsent, expectedConsent,
                label + " consent should be " + (expectedConsent ? "Consent" : "Dissent"));

        // Known issue on iOS (MIBA-4277, strict:false → report-only): the consent timestamp format
        // is hardcoded per platform / locale-dependent and, on iOS, inconsistent across flows
        // (build #66 saw day-first, 24h, lowercase pm, and even an accidental Android-format match).
        // Expected canonical format is M/d/yyyy h:mm:ss a (12h, AM/PM) — CONSENT_TS_PATTERN. A match
        // implies a parseable timestamp, so this single check also covers "timestamp present".
        // Because iOS is inconsistent, the entry is strict:false: never fails the build on iOS
        // (whether it passes or fails), asserts normally on Android. When MIBA-4277 is fixed: delete
        // the known-issues.json entry, then uncomment the original assertNotNull and remove this call.
        // softAssert.assertNotNull(ts, label + " consent entry should have a parseable date/time, got: " + content);
        boolean formatMatchesLocale = content != null && CONSENT_TS_PATTERN.matcher(content).find();
        knownIssue("consent-date-format", formatMatchesLocale,
                label + " consent timestamp should be present and in the device-locale format "
                        + "'M/d/yyyy h:mm:ss a', got: " + content);
        // reference == null → logged-in-only run: account predates this session, skip freshness.
        if (ts != null && reference != null) {
            long diffMin = Math.abs(Duration.between(ts, reference).toMinutes());
            softAssert.assertTrue(diffMin <= CONSENT_TIME_TOLERANCE_MIN,
                    label + " consent time " + ts + " should be within "
                            + CONSENT_TIME_TOLERANCE_MIN + " min of reference " + reference
                            + " (diff=" + diffMin + "min)");
        }
    }

    protected static LocalDateTime parseConsentTimestamp(String content) {
        if (content == null) return null;
        // Android: M/d/yyyy h:mm:ss a
        Matcher m = CONSENT_TS_PATTERN.matcher(content);
        if (m.find()) {
            String n = m.group(1).replaceAll("[\\u00A0\\u202F\\u2007\\u2009]", " ")
                    .replaceAll("\\s+", " ").trim();
            try { return LocalDateTime.parse(n, CONSENT_TS_FORMAT); } catch (Exception ignored) {}
        }
        // iOS: dd.MM.yyyy HH:mm:ss
        Matcher mi = CONSENT_TS_PATTERN_IOS.matcher(content);
        if (mi.find()) {
            String n = mi.group(1).replaceAll("[\\u00A0\\u202F\\u2007\\u2009]", " ")
                    .replaceAll("\\s+", " ").trim();
            try { return LocalDateTime.parse(n, CONSENT_TS_FORMAT_IOS); } catch (Exception ignored) {}
        }
        return null;
    }

    /** Training-time slot → expected reminder time (same for all 7 days). */
    protected static String slotToReminderTime(String slot) {
        if (slot == null) return null;
        switch (slot) {
            case "morning": return "09:00";
            case "noon":    return "14:00";
            case "evening": return "18:00";
            case "night":   return "21:00";
            default:        throw new IllegalArgumentException("Unknown training time slot: " + slot);
        }
    }

    protected static String expectedLanguageLabel(String lang) {
        return "de".equalsIgnoreCase(lang) ? "Deutsch" : "English (United Kingdom)";
    }

    protected void openSettings() {
        screens.dashboard().tapProfileTab();
        screens.profile().waitForScreen();
        screens.profile().tapSettings();
        screens.settings().waitForScreen();
    }

    /** Best-effort logout so the next run starts clean — never fails the test. */
    protected void logoutQuietly() {
        try {
            screens.settings().tapBack();
            screens.profile().waitForScreen();
            screens.profile().tapLogout();
            screens.launch().waitForScreen();
        } catch (Exception e) {
            log.warn("logout failed (next run may need a manual reset): {}", e.getMessage());
        }
    }
}
