package com.neuronation.tests.med.e2e;

import com.neuronation.tests.med.profile.MedSettingsVerifierBase;
import com.neuronation.testdata.Features;
import com.neuronation.utils.ScreenDumper;
import io.qameta.allure.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Full E2E happy path — one comprehensive journey per flow, all soft-asserted (one broken step
 * never halts the rest):
 *   register + onboarding → Settings verification → Privacy Settings → Consent History →
 *   logout → re-login → Change Email shows that email → MCI 90-day validity → logout.
 * Settings/Privacy/Consent verification is the shared {@link MedSettingsVerifierBase} logic.
 * Runs flow1–flow4 via the data provider (one onboarding each).
 */
@Epic("NeuroNation MED App")
@Feature("Full E2E Happy Path")
public class MedFullE2EHappyPathTest extends MedSettingsVerifierBase {

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
          groups = {Features.MED, Features.REGISTRATION, Features.REGRESSION, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("Register → onboarding → settings/privacy/consent → logout → login → change email → MCI → logout")
    @Description("Comprehensive per-flow E2E: full onboarding, then verify every onboarding "
            + "selection in Settings + Privacy Settings + Consent History, then logout, re-login, "
            + "Change Email check, MCI 90-day validity, and final logout.")
    public void fullE2EHappyPath(String flowName) {
        // The FE currently shows the consent timestamp in UTC, so reference UTC for the ±3-min
        // check. When the FE is fixed to local time: uncomment the local line, remove the UTC line.
        // LocalDateTime flowStart = LocalDateTime.now();                       // local time (use when FE fixed)
        LocalDateTime flowStart = LocalDateTime.now(ZoneOffset.UTC);            // UTC (FE shows consent ts in UTC for now)

        // 1. Register + onboarding → Dashboard
        medFlow.completeFullFlow(flowName);
        softAssert.assertTrue(screens.dashboard().isDashboardDisplayed(),
                "Dashboard should be visible after onboarding");
        ScreenDumper.dumpCurrentScreen(flowName.replaceAll("[^a-zA-Z0-9]", "_") + "_dashboard");

        // 2-12. Settings + Privacy Settings + Consent History (shared, soft-asserted).
        verifyOnboardingSettingsPrivacyConsent(flowName, flowStart);

        // 13. Logout → Launch
        step("Logout → Launch", () -> {
            screens.profile().waitForScreen();
            screens.profile().tapLogout();
            screens.launch().waitForScreen();
            softAssert.assertTrue(screens.launch().isStartNowDisplayed(),
                    "Launch screen should appear after logout");
        });

        // 14. Re-login with the same credentials → Dashboard
        final String email = medFlow.getCurrentEmail();
        step("Re-login → Dashboard", () -> {
            medFlow.loginWithCurrentCredentials();
            softAssert.assertTrue(screens.dashboard().isDashboardDisplayed(),
                    "Dashboard should be visible after re-login");
        });

        // 15. Change Email shows the email used at login
        step("Change Email shows the login email", () -> {
            screens.dashboard().tapProfileTab();
            screens.profile().waitForScreen();
            screens.profile().tapChangeEmail();
            screens.changeEmail().waitForScreen();
            String currentEmail = screens.changeEmail().getCurrentEmail();
            log.info("[{}] Change Email expected={} actual={}", flowName, email, currentEmail);
            softAssert.assertEquals(currentEmail, email, "Change Email should show the email used at login");
            screens.changeEmail().tapBack();
            screens.profile().waitForScreen();
        });

        // 16. MCI 90-day validity
        step("MCI 90-day validity", () -> {
            String validity = screens.profile().getAccountValidity();
            String expected = LocalDate.now().plusDays(90)
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            log.info("[{}] MCI validity='{}' expected until {}", flowName, validity, expected);
            softAssert.assertTrue(validity.contains(expected),
                    "MCI account should be valid 90 days (until " + expected + "): " + validity);
        });

        // 17. Final logout → Launch (leaves app clean for the next flow)
        step("Final logout → Launch", () -> medFlow.logoutAndVerify());

        log.info("[{}] full E2E happy path complete", flowName);
        softAssert.assertAll();
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
        medFlow.completeFullFlow("flow1_password_morning_skip");
        String email = medFlow.getCurrentEmail();
        String password = medFlow.getCurrentPassword();

        medFlow.logout();
        assertTrueLaunch();

        screens.launch().tapContinueTraining();
        medFlow.submitLoginForm(email, password);

        screens.tips().waitForScreen();
        softAssert.assertTrue(screens.tips().verifyContentDisplayed(),
                "Post-login Tips popup should be displayed with Understood button");
        log.info("Post-login Tips popup displayed — tapping Understood");

        screens.tips().tapUnderstood();

        screens.dashboard().waitForScreen();
        softAssert.assertTrue(screens.dashboard().isDashboardDisplayed(),
                "Dashboard should be visible after dismissing post-login Tips");
        softAssert.assertAll();
    }

    private void assertTrueLaunch() {
        softAssert.assertTrue(screens.launch().isStartNowDisplayed(),
                "Launch screen should appear after logout");
    }
}
