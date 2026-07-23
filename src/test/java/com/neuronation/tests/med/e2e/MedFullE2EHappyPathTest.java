package com.neuronation.tests.med.e2e;

import com.neuronation.helpers.CatalogProvider;
import com.neuronation.testdata.NeuroBoosterCatalog;
import com.neuronation.testdata.NeuroBoosterLabels;
import com.neuronation.tests.med.extras.ExtrasContentVerifier;
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
import java.util.List;
import java.util.Map;

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

    private static final String FLOW1 = "flow1_password_morning_skip";

    /** Extras sections each flow verifies (read-only), in catalog display order. flow1 gets the
     *  body section on an MCI account (only section that exists there); flow2 re-verifies the SAME
     *  body section on a Parkinson account (proves body is identical across account types); flow3/4
     *  split the six cognitive sections. */
    private static final Map<String, List<String>> FLOW_SECTIONS = Map.of(
            FLOW1, List.of("Use mini-exercises for your body"),
            "flow2_password_evening_doctor", List.of("Use mini-exercises for your body"),
            "flow3_declinepasswordless_noon_colorvision",
                    List.of("Cognitive Health", "Drive and motivation", "Mood"),
            "flow4_password_night_arithmetic",
                    List.of("Dealing with anxieties about the future",
                            "Changes in relationships and roles", "The effects of medication"));

    @DataProvider(name = "flows")
    public static Object[][] flows() {
        Object[][] all = {
                {"flow1_password_morning_skip"},      // standard,    23/23, morning 09:00
                {"flow2_password_evening_doctor"},    // both,        17/23, evening 18:00
                {"flow3_declinepasswordless_noon_colorvision"},// colorVision, 20/23, noon    14:00
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

        // flow1 registers an MCI account (body section only); flow2/3/4 register Parkinson accounts
        // (body + six cognitive sections). Body content is identical across both — flow2 re-verifies it.
        final boolean mci = FLOW1.equals(flowName);

        // 1. Register + onboarding → Dashboard
        medFlow.completeFullFlow(flowName, !mci);
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

        // 14b. Extras content regression — the sections assigned to this flow, read-only (nothing is
        // completed, so "discovered" stays 0). Runs right after login. flow1 (MCI) additionally proves
        // ONLY the body section exists; the bottom-nav Profile tab survives, so step 15 continues from here.
        step("Extras content — " + flowName, () -> {
            screens.dashboard().tapExtrasTab();
            screens.extras().waitForScreen();
            NeuroBoosterCatalog catalog = CatalogProvider.load(context.getLanguage(), "parkinson");
            NeuroBoosterLabels labels = CatalogProvider.labels(context.getLanguage());
            ExtrasContentVerifier verifier =
                    new ExtrasContentVerifier(screens, softAssert, catalog, labels, context.getPlatform());
            List<String> sections = FLOW_SECTIONS.getOrDefault(flowName, List.of());
            // Per-tile lifecycle: header → initial listing (no tick) → detail → complete → tick + progress.
            if (mci) {
                verifier.verifyOnlySection(sections.get(0)); // body only + full lifecycle
            } else {
                verifier.verifySections(sections);
            }
        });

        // 15. Account 90-day validity FIRST — the account label is at the TOP of Profile, so verifying it
        // before Change Email/Logout (both lower down) avoids a scroll-up-then-down round trip. The label
        // reads "Account for MCI/Parkinson valid until DD.MM.YYYY" — same 90-day window, condition-specific text.
        step("Profile account type + 90-day validity", () -> {
            screens.dashboard().tapProfileTab();
            screens.profile().waitForScreen();
            String validity = screens.profile().getAccountValidity();
            String expected = LocalDate.now().plusDays(90)
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String expectedType = mci ? "MCI" : "Parkinson";
            log.info("[{}] validity='{}' expected type={} until {}", flowName, validity, expectedType, expected);
            softAssert.assertTrue(validity != null && validity.contains(expectedType),
                    expectedType + " account type should be shown: " + validity);
            softAssert.assertTrue(validity != null && validity.contains(expected),
                    expectedType + " account should be valid 90 days (until " + expected + "): " + validity);
        });

        // 16. Change Email shows the email used at login (scrolls down from the MCI label)
        step("Change Email shows the login email", () -> {
            screens.profile().waitForScreen();
            screens.profile().tapChangeEmail();
            screens.changeEmail().waitForScreen();
            String currentEmail = screens.changeEmail().getCurrentEmail();
            log.info("[{}] Change Email expected={} actual={}", flowName, email, currentEmail);
            softAssert.assertEquals(currentEmail, email, "Change Email should show the email used at login");
            screens.changeEmail().tapBack();
            screens.profile().waitForScreen();
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
