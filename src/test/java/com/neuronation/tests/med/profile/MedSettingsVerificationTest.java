package com.neuronation.tests.med.profile;

import com.neuronation.base.BaseTest;
import com.neuronation.config.AppType;
import com.neuronation.listeners.AllureScreenshotListener;
import com.neuronation.pages.med.profile.TrainingReminderScreen;
import com.neuronation.testdata.Features;
import com.neuronation.testdata.FlowConfig;
import com.neuronation.testdata.ProfileData;
import com.neuronation.utils.TestDataLoader;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.*;

/**
 * Verifies that every onboarding selection from the active flow appears
 * correctly in Profile → Settings (and Profile → Settings → Training Reminder).
 *
 * Expectations are derived from the FlowConfig that drove registration —
 * no hardcoded literals — so the same test class covers all flows.
 */
@Listeners(AllureScreenshotListener.class)
@Epic("NeuroNation MED App")
@Feature("Settings Verification")
public class MedSettingsVerificationTest extends BaseTest {

    private static final String FLOW_NAME = "flow1_password_morning_skip";

    private FlowConfig flow;
    private ProfileData profileData;

    @BeforeMethod(alwaysRun = true)
    public void completeFlowAndNavigateToSettings() {
        medFlow.completeFullFlow(FLOW_NAME);
        flow = medFlow.getFlowConfig();
        profileData = TestDataLoader.loadProfileData(AppType.MED, context.getLanguage(), ProfileData.class);

        screens.dashboard().tapProfileTab();
        screens.profile().waitForScreen();
        screens.profile().tapSettings();
        screens.settings().waitForScreen();
    }

    // ──────────────────────────────────────────────
    // Settings screen — onboarding selections
    // ──────────────────────────────────────────────

    @Test(groups = {Features.MED, Features.PROFILE, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("Settings screen loads from Profile")
    public void testSettings_screenLoads() {
        assertTrue(screens.settings().isDisplayed(), "Settings should be visible");
        assertEquals(screens.settings().getToolbarTitle(), "Settings",
                "Toolbar should show 'Settings'");
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.REGRESSION})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Comparison Group matches age group selected in onboarding")
    public void testSettings_comparisonGroup_matchesProfile() {
        String expected = profileData.getAgeGroup();
        String actual = screens.settings().getComparisonGroup();
        log.info("ComparisonGroup expected={} actual={}", expected, actual);
        assertEquals(actual, expected, "Comparison Group should match profile.json ageGroup");
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Language matches suite language parameter")
    public void testSettings_language_matchesSelection() {
        String expected = expectedLanguageLabel(context.getLanguage());
        String actual = screens.settings().getLanguage();
        log.info("Language expected={} actual={}", expected, actual);
        assertEquals(actual, expected, "Language label should match suite language");
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Training Adaptation matches trainingComplexity flow choice")
    public void testSettings_trainingAdaptation_matchesFlow() {
        String expected = "deactivate".equals(flow.getTrainingComplexity()) ? "Don't ask me" : "Ask me";
        String actual = screens.settings().getTrainingAdaptation();
        log.info("TrainingAdaptation expected={} actual={}", expected, actual);
        assertEquals(actual, expected, "Training Adaptation should match flow.trainingComplexity");
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Special needs switch reflects flow choice")
    public void testSettings_specialNeedsSwitch_matchesFlow() {
        boolean expected = !"standard".equals(flow.getSpecialNeeds());
        boolean actual = screens.settings().isSpecialNeedsEnabled();
        log.info("SpecialNeeds expected={} actual={}", expected, actual);
        assertEquals(actual, expected,
                "Special needs switch should be ON for non-standard, OFF for standard");
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("NeuroBooster switch reflects flow choice")
    public void testSettings_neuroBoosterSwitch_matchesFlow() {
        boolean expected = flow.isNeuroBooster();
        boolean actual = screens.settings().isNeuroBoosterEnabled();
        log.info("NeuroBooster expected={} actual={}", expected, actual);
        assertEquals(actual, expected, "NeuroBooster switch should match flow.neuroBooster");
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Available exercises shown as X/Y count")
    public void testSettings_availableExercises_shown() {
        String exercises = screens.settings().getAvailableExercises();
        log.info("AvailableExercises: {}", exercises);
        assertNotNull(exercises, "Available Exercises should not be null");
        assertTrue(exercises.matches("\\d+/\\d+"),
                "Available Exercises should be 'X/Y' format, got: " + exercises);
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Settings content snapshot")
    public void testSettings_contentSnapshot() {
        verifyOrRecordContent(screens.settings(), "SettingsScreen");
        softAssert.assertAll();
    }

    // ──────────────────────────────────────────────
    // Training Reminder — per-day reminder times
    // ──────────────────────────────────────────────

    @Test(groups = {Features.MED, Features.PROFILE, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Training Reminder loads with all 7 days")
    public void testTrainingReminder_screenLoadsAllDays() {
        screens.settings().tapTrainingReminder();
        screens.trainingReminder().waitForScreen();

        assertTrue(screens.trainingReminder().isDisplayed(),
                "Training Reminder screen should be visible");

        Map<String, String> times = screens.trainingReminder().getAllReminderTimes();
        log.info("Reminder times: {}", times);
        for (String day : TrainingReminderScreen.DAYS) {
            String t = times.get(day);
            assertNotNull(t, day + " reminder time should be present");
            assertTrue(t.matches("\\d{1,2}:\\d{2}"),
                    day + " reminder should be HH:MM, got: " + t);
        }
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.REGRESSION})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Per-day reminder times match the flow's training time slot")
    public void testTrainingReminder_perDayTime_matchesFlowSlot() {
        screens.settings().tapTrainingReminder();
        screens.trainingReminder().waitForScreen();

        String slot = flow.getTrainingTime();
        String expected = slotToReminderTime(slot);
        if (expected == null) {
            throw new SkipException("No confirmed reminder-time mapping for slot '"
                    + slot + "'. Run a flow with a known slot to extend the mapping.");
        }

        Map<String, String> times = screens.trainingReminder().getAllReminderTimes();
        log.info("Slot={} expected={} actual={}", slot, expected, times);

        for (String day : TrainingReminderScreen.DAYS) {
            assertEquals(times.get(day), expected,
                    day + " reminder time should match slot '" + slot + "' (" + expected + ")");
        }
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Personalised training times toggle is ON by default")
    public void testTrainingReminder_personalisedToggle_onByDefault() {
        screens.settings().tapTrainingReminder();
        screens.trainingReminder().waitForScreen();

        boolean on = screens.trainingReminder().isPersonalisedTimesOn();
        log.info("PersonalisedTimes: {}", on);
        assertTrue(on, "'Personalised training times' should default to ON");
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    /** Map of training-time slot → expected reminder time HH:MM.
     *  Only "morning" is verified live so far. Others return null until confirmed. */
    private static String slotToReminderTime(String slot) {
        if (slot == null) return null;
        switch (slot) {
            case "morning": return "09:00";
            case "noon":    return null; // TODO: confirm by registering a noon flow
            case "evening": return null; // TODO: confirm by registering an evening flow
            case "night":   return null; // TODO: confirm by registering a night flow
            default:        return null;
        }
    }

    private static String expectedLanguageLabel(String lang) {
        return "de".equalsIgnoreCase(lang) ? "Deutsch" : "English (United Kingdom)";
    }

    private static class SkipException extends org.testng.SkipException {
        SkipException(String msg) { super(msg); }
    }
}
