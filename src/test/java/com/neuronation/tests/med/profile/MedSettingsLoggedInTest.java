package com.neuronation.tests.med.profile;

import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Focused, FAST settings check — no onboarding, no journey. Assumes the app is ALREADY logged in
 * on the Dashboard, then walks: Profile → Settings (verify every row: special needs, exercises,
 * comparison group, training priorities, language, training adaptation, training reminder,
 * NeuroBooster) → back → Privacy Settings → back → Consent History.
 *
 * It loads the expected values from a flow (default flow1) WITHOUT onboarding, so it iterates in
 * ~1-2 min instead of ~12. Skips step 0 (onboarding schedule) and the consent-timestamp freshness
 * check (the account predates this session) — VALUES are still verified.
 *
 * Run (iOS, app left on the Dashboard, WDA up):
 *   mvn -B test -Dconfig.profile=med-ios -Dforce.app.launch=false \
 *       -DsuiteFile=src/test/resources/suites/settings-loggedin.xml -Dflow=flow1_password_morning_skip
 */
@Epic("NeuroNation MED App")
@Feature("Settings Verification (logged-in)")
public class MedSettingsLoggedInTest extends MedSettingsVerifierBase {

    @Test(groups = {Features.MED, Features.PROFILE})
    @Severity(SeverityLevel.NORMAL)
    @Story("From an already-logged-in Dashboard: verify Settings → Privacy → Consent")
    public void verifySettingsPrivacyConsent_loggedIn() {
        String flowName = System.getProperty("flow", "flow1_password_morning_skip");
        medFlow.loadFlow(flowName);                 // load expected values; NO onboarding
        verifyOnboardingSettingsPrivacyConsent(flowName, LocalDateTime.now(ZoneOffset.UTC), false);
        softAssert.assertAll();
    }
}
