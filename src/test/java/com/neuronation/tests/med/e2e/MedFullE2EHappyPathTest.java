package com.neuronation.tests.med.e2e;

import com.neuronation.base.BaseTest;
import com.neuronation.testdata.Features;
import com.neuronation.utils.ScreenDumper;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
        // Settings verification (onboarding selections → Settings) is covered for all 4
        // flows in MedSettingsVerificationTest; this test owns the registration → dashboard
        // → re-login → change-email journey.
        medFlow.completeFullFlow("flow1_password_morning_skip");

        assertTrue(screens.dashboard().isDashboardDisplayed(), "Dashboard should be visible");
        ScreenDumper.dumpCurrentScreen("flow1_dashboard");

        // ── 2. Profile → MCI validity ──
        screens.dashboard().tapProfileTab();
        screens.profile().waitForScreen();
        String validity = screens.profile().getAccountValidity();
        assertMciNinetyDayValidity(validity);

        // ── 3. Profile → Logout → Launch ──
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

        // ── 8. Back to Profile → common verified logout (leaves app on Launch for next flow) ──
        // tapBack: system back on Android, nav-bar back icon on iOS (cross-platform helper).
        screens.changeEmail().tapBack();
        screens.profile().waitForScreen();
        medFlow.logoutAndVerify();

        log.info("Flow 1 full journey complete — {}", validity);
        softAssert.assertAll();
    }

    /**
     * Verify the Profile account-validity label shows an MCI account valid for exactly
     * 90 days from today. Merged in from the former standalone testMciProfileAccountValidity
     * so every flow checks it right after registration (no separate test needed).
     */
    private void assertMciNinetyDayValidity(String validity) {
        String expected = LocalDate.now().plusDays(90)
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        assertTrue(validity.contains(expected),
                "MCI account should be valid 90 days (until " + expected + "): " + validity);
        log.info("MCI 90-day validity confirmed — valid until {}", expected);
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
        assertMciNinetyDayValidity(validity);
        log.info("Flow 2 complete — {}", validity);

        // On Profile already → common verified logout (leaves app on Launch for next flow)
        medFlow.logoutAndVerify();
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
        assertMciNinetyDayValidity(validity);
        log.info("Flow 3 complete — {}", validity);

        // On Profile already → common verified logout (leaves app on Launch for next flow)
        medFlow.logoutAndVerify();
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
        assertMciNinetyDayValidity(validity);
        log.info("Flow 4 complete — {}", validity);

        // On Profile already → common verified logout (leaves app on Launch for next flow)
        medFlow.logoutAndVerify();
    }
}
