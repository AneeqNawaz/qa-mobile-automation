package com.neuronation.tests.med.profile;

import com.neuronation.base.BaseTest;
import com.neuronation.config.AppType;
import com.neuronation.pages.med.profile.TrainingReminderScreen;
import com.neuronation.testdata.ExerciseCatalog;
import com.neuronation.testdata.Features;
import com.neuronation.testdata.FlowConfig;
import com.neuronation.testdata.ProfileData;
import com.neuronation.utils.TestDataLoader;
import io.qameta.allure.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

/**
 * Verifies that every onboarding selection from each flow appears correctly in
 * Profile → Settings (special-needs switches, locked exercises + count, reminder
 * times, and the existing rows). One onboarding per flow via the data provider —
 * BaseTest creates a fresh driver in @BeforeMethod before each data row.
 */
@Epic("NeuroNation MED App")
@Feature("Settings Verification")
public class MedSettingsVerificationTest extends BaseTest {

    @DataProvider(name = "flows")
    public static Object[][] flows() {
        Object[][] all = {
                {"flow1_password_morning_skip"},      // standard,    23/23, morning 09:00
                {"flow2_password_evening_doctor"},    // both,        17/23, evening 18:00
                {"flow3_nopassword_noon_colorvision"},// colorVision, 20/23, noon    14:00
                {"flow4_password_night_arithmetic"},  // arithmetic,  20/23, night   21:00
        };
        // Optional single-flow filter: -Dflow=flow4_password_night_arithmetic
        String only = System.getProperty("flow");
        if (only != null && !only.isEmpty()) {
            return new Object[][] {{only}};
        }
        return all;
    }

    @Test(dataProvider = "flows",
          groups = {Features.MED, Features.PROFILE, Features.REGRESSION})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Onboarding selections are reflected in Settings")
    public void settingsReflectOnboarding(String flowName) {
        ExerciseCatalog catalog = ExerciseCatalog.load();

        medFlow.completeFullFlow(flowName);
        FlowConfig flow = medFlow.getFlowConfig();
        ProfileData profileData = TestDataLoader.loadProfileData(
                AppType.MED, context.getLanguage(), ProfileData.class);
        String needs = flow.getSpecialNeeds();

        // Navigate Profile → Settings
        screens.dashboard().tapProfileTab();
        screens.profile().waitForScreen();
        screens.profile().tapSettings();
        screens.settings().waitForScreen();

        // Order matters: the Special-needs and Available-Exercises accordions expand inline
        // and push the lower Settings rows off-screen. So read all plain rows + Training
        // Reminder FIRST (collapsed layout), do NeuroBooster (its Android back-nav lands on
        // Profile), then RE-ENTER Settings and expand the accordions LAST.

        // ── Plain text rows (collapsed layout) ──
        softAssert.assertEquals(screens.settings().getComparisonGroup(), profileData.getAgeGroup(),
                "Comparison Group should match profile.json ageGroup");
        softAssert.assertEquals(screens.settings().getLanguage(), expectedLanguageLabel(context.getLanguage()),
                "Language should match suite language");
        String expectedAdaptation = "deactivate".equals(flow.getTrainingComplexity()) ? "Don't ask me" : "Ask me";
        softAssert.assertEquals(screens.settings().getTrainingAdaptation(), expectedAdaptation,
                "Training Adaptation should match flow.trainingComplexity");

        // ── Available exercises count (reads the row subtitle, no expansion) ──
        String expectedCount = catalog.expectedCountLabel(needs);
        String actualCount = screens.settings().getAvailableExercises();
        log.info("[{}] AvailableExercises expected={} actual={}", flowName, expectedCount, actualCount);
        softAssert.assertEquals(actualCount, expectedCount,
                "Available Exercises count should match specialNeeds=" + needs);

        // ── Training Reminder per-day times (tap in, read, back to Settings) ──
        screens.settings().tapTrainingReminder();
        screens.trainingReminder().waitForScreen();
        String expectedTime = slotToReminderTime(flow.getTrainingTime());
        Map<String, String> times = screens.trainingReminder().getAllReminderTimes();
        log.info("[{}] slot={} expectedTime={} actual={}", flowName, flow.getTrainingTime(), expectedTime, times);
        for (String day : TrainingReminderScreen.DAYS) {
            softAssert.assertEquals(times.get(day), expectedTime,
                    day + " reminder should be " + expectedTime + " for slot " + flow.getTrainingTime());
        }
        softAssert.assertTrue(screens.trainingReminder().isPersonalisedTimesOn(),
                "'Personalised training times' should default ON");
        screens.trainingReminder().tapBack(); // → Settings

        // ── NeuroBooster (Android back-nav lands on Profile) ──
        softAssert.assertEquals(screens.settings().isNeuroBoosterEnabled(), flow.isNeuroBooster(),
                "NeuroBooster switch should match flow.neuroBooster");

        // ── Re-enter Settings, then expand accordions LAST ──
        screens.profile().waitForScreen();
        screens.profile().tapSettings();
        screens.settings().waitForScreen();

        // ── Special-needs switches ──
        boolean expectColorVision = "colorVision".equals(needs) || "both".equals(needs);
        boolean expectArithmetic  = "arithmetic".equals(needs)  || "both".equals(needs);
        boolean actualColorVision = screens.settings().isColorVisionEnabled();
        boolean actualArithmetic  = screens.settings().isArithmeticEnabled();
        log.info("[{}] ColorVision expected={} actual={}", flowName, expectColorVision, actualColorVision);
        log.info("[{}] Arithmetic  expected={} actual={}", flowName, expectArithmetic, actualArithmetic);
        softAssert.assertEquals(actualColorVision, expectColorVision,
                "Color Vision switch should match flow specialNeeds=" + needs);
        softAssert.assertEquals(actualArithmetic, expectArithmetic,
                "Arithmetic switch should match flow specialNeeds=" + needs);

        // ── Locked exercises set ──
        Set<String> expectedLocked = new java.util.LinkedHashSet<>(catalog.lockedFor(needs));
        Set<String> actualLocked = screens.settings().getLockedExercises(catalog.all());
        log.info("[{}] LockedExercises expected={} actual={}", flowName, expectedLocked, actualLocked);
        softAssert.assertEquals(actualLocked, expectedLocked,
                "Locked (unchecked) exercises should match specialNeeds=" + needs);

        // Return the app to the Launch screen so the next flow starts clean (every E2E flow
        // ends logged out). Best-effort — must not mask the verification result above.
        try {
            screens.settings().tapBack();          // Settings → Profile
            screens.profile().waitForScreen();
            screens.profile().tapLogout();
            screens.launch().waitForScreen();
            log.info("[{}] logged out — Launch screen visible", flowName);
        } catch (Exception e) {
            log.warn("[{}] post-flow logout failed (next flow may need a manual reset): {}",
                    flowName, e.getMessage());
        }

        softAssert.assertAll();
    }

    /** Training-time slot → expected reminder time (same for all 7 days). */
    private static String slotToReminderTime(String slot) {
        if (slot == null) return null;
        switch (slot) {
            case "morning": return "09:00";
            case "noon":    return "14:00";
            case "evening": return "18:00";
            case "night":   return "21:00";
            default:        throw new IllegalArgumentException("Unknown training time slot: " + slot);
        }
    }

    private static String expectedLanguageLabel(String lang) {
        return "de".equalsIgnoreCase(lang) ? "Deutsch" : "English (United Kingdom)";
    }
}
