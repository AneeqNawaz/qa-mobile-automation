package com.neuronation.tests.med.profile;

import com.neuronation.config.AppType;
import com.neuronation.testdata.Features;
import com.neuronation.testdata.ProfileData;
import com.neuronation.utils.TestDataLoader;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Standalone, read-only Settings checks that don't need to run inside the full E2E happy path
 * (the full per-flow Settings/Privacy/Consent verification lives in MedFullE2EHappyPathTest via
 * the shared {@link MedSettingsVerifierBase}). These onboard flow1 and inspect one area in
 * depth. Tagged out of the `regression` group so the regular suites skip them.
 */
@Epic("NeuroNation MED App")
@Feature("Settings Verification")
public class MedSettingsVerificationTest extends MedSettingsVerifierBase {

    private static final String FLOW = "flow1_password_morning_skip";

    @Test(groups = {Features.MED, Features.PROFILE})
    @Severity(SeverityLevel.NORMAL)
    @Story("Comparison Group accordion lists all age groups; selected shows in subtitle")
    public void comparisonGroup_listsAllAgeGroups() {
        medFlow.completeFullFlow(FLOW);
        ProfileData profileData = TestDataLoader.loadProfileData(
                AppType.MED, context.getLanguage(), ProfileData.class);
        openSettings();
        verifyExpandableRow(FLOW, "Comparison Group", profileData.getAgeGroup());
        logoutQuietly();
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.PROFILE})
    @Severity(SeverityLevel.NORMAL)
    @Story("Training Priorities: Attention popup (close on outside tap); Understood reveals 4 domains")
    public void trainingPriorities_popupAndDomains() {
        medFlow.completeFullFlow(FLOW);
        openSettings();

        softAssert.assertEquals(screens.settings().getTrainingPriorities(), "Recommended",
                "Training Priorities should default to 'Recommended'");

        screens.settings().openTrainingPriorities();
        softAssert.assertTrue(screens.settings().isAttentionPopupShown(),
                "Attention popup (with Understood button) should appear on tapping Training Priorities");
        String popupMessage = screens.settings().getAttentionMessage();
        log.info("Attention popup msg='{}'", popupMessage);
        softAssert.assertFalse(popupMessage.isEmpty(), "Attention popup should show a warning message");

        screens.settings().dismissAttentionPopupOutside();
        softAssert.assertFalse(screens.settings().isAttentionPopupShown(),
                "Tapping outside should dismiss the Attention popup");

        screens.settings().openTrainingPriorities();
        screens.settings().tapUnderstood();
        List<String> domains = screens.settings().getTrainingPriorityDomains();
        softAssert.assertEqualsNoOrder(domains.toArray(), settingsOptions.trainingPriorityDomains().toArray(),
                "Understood should reveal the configured training-priority domains");

        logoutQuietly();
        softAssert.assertAll();
    }
}
