package com.neuronation.tests.med.e2e;

import com.neuronation.base.BaseTest;
import com.neuronation.listeners.AllureScreenshotListener;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests logout from Profile and login with same credentials.
 * Flow: Complete E2E → Dashboard → Profile → Log out → Launch → Continue Training
 *       → Login Choice → Log In Via Email → Login Form → Dashboard (logged in).
 *
 * Uses medFlow.logout() and medFlow.loginWithCurrentCredentials() which are reusable
 * by any test that needs a logged-in state without going through registration.
 */
@Listeners(AllureScreenshotListener.class)
@Epic("NeuroNation MED App")
@Feature("Logout & Login")
public class MedLogoutLoginTest extends BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void completeFlowToDashboard() {
        medFlow.completeFullFlow("flow1_password_morning_skip");
        log.info("Flow complete. Email: {}, Password: {}",
                medFlow.getCurrentEmail(), medFlow.getCurrentPassword());
    }

    // ── LOGOUT ──

    @Test(groups = {Features.MED, Features.LOGIN, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("Logout returns to Launch screen")
    @Description("Dashboard → Profile → tap 'Log out' → Launch screen with 'Start now' and 'Continue training'.")
    public void testLogout_returnsToLaunchScreen() {
        medFlow.logout();
        assertTrue(screens.launch().isStartNowDisplayed(),
                "Launch screen should be visible after logout");
    }

    // ── LOGIN ──

    @Test(groups = {Features.MED, Features.LOGIN, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("Login with same credentials reaches Dashboard")
    @Description("Logout → Launch → Continue Training → Login Choice → Log In Via Email "
            + "→ enter email + password → Dashboard loads.")
    public void testLogin_withSameCredentials_reachesDashboard() {
        medFlow.logout();
        medFlow.loginWithCurrentCredentials();

        assertTrue(screens.dashboard().isDashboardDisplayed(),
                "Dashboard should be visible after login");
        log.info("Login successful — Dashboard visible");
    }

    // ── PROFILE AFTER RE-LOGIN ──

    @Test(groups = {Features.MED, Features.LOGIN, Features.REGRESSION})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Profile shows same MCI account after re-login")
    @Description("Saves account info → logout → login with same credentials "
            + "→ verify account ID and MCI validity are identical.")
    public void testLogin_profileShowsSameAccount() {
        // Save original account info
        screens.dashboard().tapProfileTab();
        screens.profile().waitForScreen();
        String originalValidity = screens.profile().getAccountValidity();
        String originalId = screens.profile().getAccountId();
        log.info("Original: {} / {}", originalId, originalValidity);

        // Logout and re-login
        medFlow.logout();
        medFlow.loginWithCurrentCredentials();

        // Verify same account
        screens.dashboard().tapProfileTab();
        screens.profile().waitForScreen();
        String newValidity = screens.profile().getAccountValidity();
        String newId = screens.profile().getAccountId();
        log.info("After login: {} / {}", newId, newValidity);

        assertEquals(newValidity, originalValidity,
                "Account validity should be the same after re-login");
        assertEquals(newId, originalId,
                "Account ID should be the same after re-login");
    }
}
