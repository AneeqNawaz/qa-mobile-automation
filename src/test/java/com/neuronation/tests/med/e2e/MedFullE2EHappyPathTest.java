package com.neuronation.tests.med.e2e;

import com.neuronation.base.BaseTest;
import com.neuronation.config.AppType;
import com.neuronation.pages.med.profile.TrainingReminderScreen;
import com.neuronation.testdata.Features;
import com.neuronation.testdata.FlowConfig;
import com.neuronation.testdata.ProfileData;
import com.neuronation.utils.ScreenDumper;
import com.neuronation.utils.TestDataLoader;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.*;

/**
 * Full E2E happy path tests — each flow uses a different configuration
 * for auth method, training options, consents, etc.
 * All choices are driven by testdata/med-en.json → flows.{flowName}
 */
@Epic("NeuroNation MED App")
@Feature("Full E2E Happy Path")
public class MedFullE2EHappyPathTest extends BaseTest {

    @Test(
        description = "Flow 1: Password + Standard + Morning + Doctor Skip + Newsletter Agree",
        groups = {Features.MED, Features.REGISTRATION, Features.SMOKE, Features.CRITICAL}
    )
    @Severity(SeverityLevel.BLOCKER)
    @Story("MCI registration → password → standard morning → dashboard")
    @Description("Full E2E: generate MCI code → register via email with password → "
            + "standard training → morning schedule → doctor skip → newsletter agree → "
            + "NeuroBooster yes → Promise yes → Dashboard. Verify profile shows MCI 90-day validity.")
    public void testFlow1_PasswordMorningSkip() {
        // ── 1. Register + onboarding → Dashboard ──
        medFlow.completeFullFlow("flow1_password_morning_skip");
        FlowConfig flow = medFlow.getFlowConfig();
        ProfileData profileData = TestDataLoader.loadProfileData(
                AppType.MED, context.getLanguage(), ProfileData.class);

        assertTrue(screens.dashboard().isDashboardDisplayed(), "Dashboard should be visible");
        ScreenDumper.dumpCurrentScreen("flow1_dashboard");

        // ── 2. Profile → MCI validity ──
        screens.dashboard().tapProfileTab();
        screens.profile().waitForScreen();
        String validity = screens.profile().getAccountValidity();
        assertTrue(validity.contains("MCI"), "Should be MCI account: " + validity);

        // ── 3. Profile → Settings → verify each onboarding element step by step ──
        log.info("=== Tapping Profile → Settings ===");
        screens.profile().tapSettings();
        screens.settings().waitForScreen();

        // Verify each row one by one
        log.info("--- [1/7] Comparison Group ---");
        String actualComparison = screens.settings().getComparisonGroup();
        log.info("Expected: {} | Actual: {}", profileData.getAgeGroup(), actualComparison);
        softAssert.assertEquals(actualComparison, profileData.getAgeGroup(),
                "Comparison Group should match profile.ageGroup");

        log.info("--- [2/7] Language ---");
        String expectedLang = "de".equalsIgnoreCase(context.getLanguage())
                ? "Deutsch" : "English (United Kingdom)";
        String actualLang = screens.settings().getLanguage();
        log.info("Expected: {} | Actual: {}", expectedLang, actualLang);
        assertEquals(actualLang, expectedLang, "Language should match suite language");

        log.info("--- [3/7] Training Adaptation (from trainingComplexity) ---");
        String expectedAdaptation = "deactivate".equals(flow.getTrainingComplexity())
                ? "Don't ask me" : "Ask me";
        String actualAdaptation = screens.settings().getTrainingAdaptation();
        log.info("Expected: {} | Actual: {}", expectedAdaptation, actualAdaptation);
        assertEquals(actualAdaptation, expectedAdaptation,
                "Training Adaptation should match flow.trainingComplexity");

        log.info("--- [4/7] Special Needs ---");
        boolean expectedSpecialNeeds = !"standard".equals(flow.getSpecialNeeds());
        boolean actualSpecialNeeds = screens.settings().isSpecialNeedsEnabled();
        log.info("Expected: {} | Actual: {}", expectedSpecialNeeds, actualSpecialNeeds);
        assertEquals(actualSpecialNeeds, expectedSpecialNeeds,
                "Special needs switch should match flow.specialNeeds");

        log.info("--- [5/8] Available Exercises (depends on flow.specialNeeds) ---");
        String exercises = screens.settings().getAvailableExercises();
        // standard → all 23/23 available; colorVision/arithmetic/both → fewer (X<23)
        String expectedExercises = "standard".equals(flow.getSpecialNeeds()) ? "23/23" : null;
        log.info("Expected: {} | Actual: {}", expectedExercises != null ? expectedExercises : "X/23 with X<23", exercises);
        assertTrue(exercises.matches("\\d+/\\d+"),
                "Available Exercises should be 'X/Y' format, got: " + exercises);
        if (expectedExercises != null) {
            assertEquals(exercises, expectedExercises,
                    "Available Exercises should be 23/23 for standard special needs");
        } else {
            String[] parts = exercises.split("/");
            int x = Integer.parseInt(parts[0]), y = Integer.parseInt(parts[1]);
            assertTrue(x < y,
                    "Available Exercises X should be less than Y when special needs filter active: " + exercises);
        }

        log.info("--- [6/8] Training Priorities ---");
        String priorities = screens.settings().getTrainingPriorities();
        log.info("Actual: {} (default 'Recommended')", priorities);
        assertEquals(priorities, "Recommended",
                "Training Priorities should default to 'Recommended' for new account");

        // ── [7/8] Tap Training Reminder → per-day times → back to Settings ──
        log.info("--- [7/8] Training Reminder (tap → verify → back) ---");
        screens.settings().tapTrainingReminder();
        screens.trainingReminder().waitForScreen();

        String slot = flow.getTrainingTime();
        String expectedTime = slotToReminderTime(slot);
        assertNotNull(expectedTime, "No confirmed reminder mapping for slot '" + slot + "'");

        Map<String, String> times = screens.trainingReminder().getAllReminderTimes();
        log.info("Slot={} Expected={} per day", slot, expectedTime);
        for (String day : TrainingReminderScreen.DAYS) {
            String actualDayTime = times.get(day);
            log.info("  {} → expected={} actual={}", day, expectedTime, actualDayTime);
            assertEquals(actualDayTime, expectedTime,
                    day + " should be " + expectedTime + " for slot " + slot);
        }
        assertTrue(screens.trainingReminder().isPersonalisedTimesOn(),
                "'Personalised training times' should default ON");
        screens.trainingReminder().tapBack();   // → Settings

        // ── [8/8] NeuroBooster — last (Android navigates into detail screen) ──
        log.info("--- [8/8] NeuroBooster (tap → verify → back) ---");
        boolean actualNB = screens.settings().isNeuroBoosterEnabled();
        log.info("Expected: {} | Actual: {}", flow.isNeuroBooster(), actualNB);
        assertEquals(actualNB, flow.isNeuroBooster(),
                "NeuroBooster switch should match flow.neuroBooster");

        log.info("=== Settings VERIFIED ✓ comparison={}, lang={}, adaptation={}, "
                        + "specialNeeds={}, neuroBooster={}, exercises={}, reminder={} ===",
                profileData.getAgeGroup(), expectedLang, expectedAdaptation,
                expectedSpecialNeeds, flow.isNeuroBooster(), exercises, expectedTime);

        // ── 6. Profile → Logout → Launch ──
        // Android: NB back overshoots to Profile — already there
        // iOS: NB read directly on Settings — need to tap back from Settings to Profile
        if (screens.profile().isProfileDisplayed()) {
            log.info("Already on Profile after NB check (Android back-overshoot)");
        } else {
            log.info("Tapping back from Settings to reach Profile (iOS)");
            screens.settings().tapBack();
        }
        screens.profile().waitForScreen();
        screens.profile().tapLogout();
        screens.launch().waitForScreen();
        assertTrue(screens.launch().isStartNowDisplayed(),
                "Launch screen should appear after logout");

        // ── 6. Login with same credentials (auto-dismisses post-login Tips) ──
        String email = medFlow.getCurrentEmail();
        medFlow.loginWithCurrentCredentials();
        assertTrue(screens.dashboard().isDashboardDisplayed(),
                "Dashboard should be visible after re-login");

        // ── 7. Profile → Change Email → assert current email matches login email ──
        screens.dashboard().tapProfileTab();
        screens.profile().waitForScreen();
        screens.profile().tapChangeEmail();
        screens.changeEmail().waitForScreen();
        String currentEmail = screens.changeEmail().getCurrentEmail();
        log.info("Change Email expected={} actual={}", email, currentEmail);
        assertEquals(currentEmail, email,
                "Change Email should show the email used at login");

        log.info("Flow 1 full journey complete — {}", validity);
        softAssert.assertAll();
    }

    private static String slotToReminderTime(String slot) {
        if (slot == null) return null;
        switch (slot) {
            case "morning": return "09:00";
            case "noon":    return null;
            case "evening": return null;
            case "night":   return null;
            default:        return null;
        }
    }

    @Test(
        description = "Post-login Tips popup is shown; tap Understood reveals Dashboard",
        groups = {Features.MED, Features.LOGIN, Features.SMOKE, Features.CRITICAL}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Post-login Tips popup → Understood → Dashboard")
    @Description("After logging back in with existing credentials, a Tips popup with an "
            + "'Understood' button appears in front of the Dashboard. Verify it is displayed, "
            + "tap Understood, and confirm Dashboard is then accessible.")
    public void testPostLogin_TipsScreenDisplayed_Understood_Dashboard() {
        // ── Setup: register + onboarding to have a valid account ──
        medFlow.completeFullFlow("flow1_password_morning_skip");
        String email = medFlow.getCurrentEmail();
        String password = medFlow.getCurrentPassword();

        // ── Logout, return to Launch ──
        medFlow.logout();
        assertTrue(screens.launch().isStartNowDisplayed(),
                "Launch screen should appear after logout");

        // ── Inline login (bypasses auto-dismiss in loginWithCredentials so we can assert Tips) ──
        screens.launch().tapContinueTraining();
        medFlow.submitLoginForm(email, password);

        // ── Assert Tips popup IS displayed ──
        screens.tips().waitForScreen();
        assertTrue(screens.tips().verifyContentDisplayed(),
                "Post-login Tips popup should be displayed with Understood button");
        log.info("Post-login Tips popup displayed — tapping Understood");

        // ── Tap Understood ──
        screens.tips().tapUnderstood();

        // ── Verify Dashboard ──
        screens.dashboard().waitForScreen();
        assertTrue(screens.dashboard().isDashboardDisplayed(),
                "Dashboard should be visible after dismissing post-login Tips");
    }

    @Test(
        description = "Flow 2: Password + Standard + Evening + Doctor Fill + Newsletter Disagree",
        groups = {Features.MED, Features.REGISTRATION, Features.REGRESSION}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("MCI registration → password → standard evening → doctor filled → dashboard")
    @Description("Full E2E: register with password → fill doctor details → standard training → "
            + "evening schedule → newsletter disagree → NeuroBooster no → Promise no → Dashboard.")
    public void testFlow2_PasswordEveningDoctor() throws InterruptedException {
        medFlow.completeFullFlow("flow2_password_evening_doctor");

        assertTrue(screens.dashboard().isDashboardDisplayed(), "Dashboard should be visible");
        ScreenDumper.dumpCurrentScreen("flow2_dashboard");

        screens.dashboard().tapProfileTab();
        Thread.sleep(2000);
        String validity = screens.profile().getAccountValidity();
        assertTrue(validity.contains("MCI"), "Should be MCI account: " + validity);
        log.info("Flow 2 complete — {}", validity);
    }

    @Test(
        description = "Flow 3: No-Password + Color Vision + Noon + Deny Notifications + All Consents",
        groups = {Features.MED, Features.REGISTRATION, Features.REGRESSION}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("MCI registration → no-password → color vision → noon → deny notifications")
    @Description("Full E2E: register with 'No use password' → color vision deficiency → "
            + "noon schedule → deny notifications → all data consents → Dashboard.")
    public void testFlow3_NoPasswordNoonColorVision() throws InterruptedException {
        medFlow.completeFullFlow("flow3_nopassword_noon_colorvision");

        assertTrue(screens.dashboard().isDashboardDisplayed(), "Dashboard should be visible");
        ScreenDumper.dumpCurrentScreen("flow3_dashboard");

        screens.dashboard().tapProfileTab();
        Thread.sleep(2000);
        String validity = screens.profile().getAccountValidity();
        assertTrue(validity.contains("MCI"), "Should be MCI account: " + validity);
        log.info("Flow 3 complete — {}", validity);
    }

    @Test(
        description = "Flow 4: Password + Arithmetic + Night + Deactivate Complexity + Doctor Fill",
        groups = {Features.MED, Features.REGISTRATION, Features.REGRESSION}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("MCI registration → password → arithmetic → night → deactivate complexity")
    @Description("Full E2E: register with password → arithmetic impairment → night schedule → "
            + "deactivate training complexity → doctor fill → Dashboard.")
    public void testFlow4_PasswordNightArithmetic() throws InterruptedException {
        medFlow.completeFullFlow("flow4_password_night_arithmetic");

        assertTrue(screens.dashboard().isDashboardDisplayed(), "Dashboard should be visible");
        ScreenDumper.dumpCurrentScreen("flow4_dashboard");

        screens.dashboard().tapProfileTab();
        Thread.sleep(2000);
        String validity = screens.profile().getAccountValidity();
        assertTrue(validity.contains("MCI"), "Should be MCI account: " + validity);
        log.info("Flow 4 complete — {}", validity);
    }
}
