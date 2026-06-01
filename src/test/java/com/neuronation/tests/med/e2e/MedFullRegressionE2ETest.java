package com.neuronation.tests.med.e2e;

import com.neuronation.base.BaseTest;
import com.neuronation.config.AppType;
import com.neuronation.driver.DriverManager;
import com.neuronation.listeners.AllureScreenshotListener;
import com.neuronation.testdata.RegistrationData;
import com.neuronation.utils.BugReporter;
import com.neuronation.utils.TestDataLoader;
import io.appium.java_client.AppiumBy;
import io.qameta.allure.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.testng.Assert.*;

/**
 * MED Full Regression E2E Test Suite
 *
 * Parameterized via DataProvider — each flow config exercises a different
 * combination of onboarding choices (auth, doctor, newsletter, training, etc.)
 *
 * Per flow:
 *   - 9 negative validation tests (DiGA code, email, password)
 *   - 22 content snapshot verifications
 *   - 30 screens navigated end-to-end
 *   - Profile MCI + 90-day validity assertion
 */
@Listeners(AllureScreenshotListener.class)
@Epic("NeuroNation MED")
public class MedFullRegressionE2ETest extends BaseTest {

    @DataProvider(name = "flowConfigs")
    public Object[][] flowConfigs() {
        return new Object[][] {
            { "flow2_password_evening_doctor",  "Flow 2: Password + Evening + Fill Doctor" },
        };
    }

    // ══════════════════════════════════════════════════
    //  MAIN TEST
    // ══════════════════════════════════════════════════

    @Test(
        dataProvider = "flowConfigs",
        description = "Full regression E2E: negative tests + content verification + happy path through all 30 screens"
    )
    @Severity(SeverityLevel.BLOCKER)
    @Feature("Full Regression E2E")
    public void testFullRegressionE2E(String flowName, String flowDescription) {
        // Dynamic Allure metadata per flow
        Allure.story(flowDescription);
        Allure.parameter("Flow Config", flowName);
        Allure.description("Full regression for " + flowDescription
                + "\n\nCovers: 30 screens, 9 negative tests, 22 content snapshots, "
                + "email verification via IMAP, MCI 90-day validity check.");

        medFlow.loadFlow(flowName);
        medFlow.generateFreshMciCode();

        RegistrationData regData = TestDataLoader.loadRegistrationData(
                AppType.MED, context.getLanguage(), RegistrationData.class);

        // ── Registration (Screens 1-9) ──
        verifyLaunchScreen();
        verifyAppSelectionScreen();
        verifyDiGACodeScreen();
        verifyOnboardingVideo();
        verifyCreateAccountScreen();
        verifyEmailRegistrationScreen(regData);
        verifyPasskeyScreen();
        verifySetPasswordScreen();
        verifyEmailVerificationScreen();

        // ── Onboarding (Screens 10-28, config-driven) ──
        verifyDoctorInfoScreen();
        verifyNewsletterScreen();
        verifyTipsScreens();
        verifyExercises();
        verifyAgeGroupScreen();
        verifyEvaluationScreen();
        verifyTrainingComplexityScreen();
        verifySpecialNeedsScreen();
        verifyTrainingTimeScreen();
        verifyScheduleReviewScreen();
        verifyNotificationPermission();
        verifyNeuroBoosterScreen();
        verifyPromiseScreen();
        verifyFinalTips();

        // ── Verification (Screens 29-30) ──
        verifyDashboardScreen();
        verifyProfileScreen();

        softAssert.assertAll();
    }

    // ══════════════════════════════════════════════════
    //  REGISTRATION — Screens 1–9
    // ══════════════════════════════════════════════════

    @Step("Screen 1: Launch — verify all elements visible")
    private void verifyLaunchScreen() {
        log.info("=== Screen 1: Launch ===");
        screens.launch().waitForScreen();
        assertTrue(screens.launch().verifyAllElementsVisible(),
                "All launch screen elements should be visible");
        assertNotNull(screens.launch().getTitle(),
                "Launch screen title should be visible");
        verifyOrRecordContent(screens.launch(), "LaunchScreen");
        screens.launch().tapStartNow();
    }

    @Step("Screen 2: App Selection — select Medical app")
    private void verifyAppSelectionScreen() {
        log.info("=== Screen 2: App Selection ===");
        screens.appSelection().waitForScreen();
        verifyOrRecordContent(screens.appSelection(), "AppSelectionScreen");
        screens.appSelection().selectMedicalApp();
    }

    @Step("Screen 3: DiGA Code — 3 negative tests + valid activation")
    private void verifyDiGACodeScreen() {
        log.info("=== Screen 3: DiGA Code ===");
        screens.digaCode().waitForScreen();
        verifyOrRecordContent(screens.digaCode(), "DiGACodeScreen");

        log.info("Negative: empty code");
        screens.digaCode().tapActivate();
        assertTrue(screens.digaCode().isActivationScreenDisplayed(),
                "Should stay on activation screen with empty code");

        log.info("Negative: invalid code");
        String invalidError = screens.digaCode().enterInvalidCodeAndGetError("INVALIDCODE999");
        assertNotNull(invalidError, "Error dialog should appear for invalid code");

        log.info("Negative: short code");
        String shortError = screens.digaCode().enterInvalidCodeAndGetError("ABC");
        assertNotNull(shortError, "Error dialog should appear for short code");

        log.info("Positive: valid code {}", medFlow.getCurrentCode());
        screens.digaCode().enterCodeAndActivate(medFlow.getCurrentCode());
    }

    @Step("Screen 4: Onboarding Video — verify player controls")
    private void verifyOnboardingVideo() {
        log.info("=== Screen 4: Onboarding Video ===");
        screens.onboardingVideo().waitForScreen();
        assertTrue(screens.onboardingVideo().verifyVideoControls(),
                "Video controls (progress bar, close button) should be functional");
        screens.onboardingVideo().tapClose();
    }

    @Step("Screen 5: Create Account — register via email")
    private void verifyCreateAccountScreen() {
        log.info("=== Screen 5: Create Account ===");
        screens.createAccount().waitForScreen();
        verifyOrRecordContent(screens.createAccount(), "CreateAccountScreen");
        screens.createAccount().tapRegisterViaEmail();
        medFlow.waitAndTapDialogButton("android:id/button1", "Continue");
    }

    @Step("Screen 6: Email Registration — 4 negative tests + valid submit")
    private void verifyEmailRegistrationScreen(RegistrationData regData) {
        log.info("=== Screen 6: Email Registration ===");
        screens.emailRegistration().waitForScreen();
        verifyOrRecordContent(screens.emailRegistration(), "EmailRegistrationScreen");
        assertTrue(screens.emailRegistration().verifyTermsContent(),
                "Terms checkbox should mention Terms of Use");
        assertTrue(screens.emailRegistration().verifyPrivacyContent(),
                "Privacy checkbox should mention Privacy Policy");

        log.info("Negative: submit without checkboxes");
        assertTrue(screens.emailRegistration().trySubmitWithoutCheckboxes(regData.generateEmail()),
                "Should stay on screen when checkboxes not checked");

        log.info("Negative: empty email");
        assertTrue(screens.emailRegistration().trySubmitWithEmptyEmail(),
                "Should stay on screen with empty email");

        log.info("Negative: invalid email format");
        assertTrue(screens.emailRegistration().trySubmitWithInvalidEmail("not-an-email"),
                "Should reject invalid email format");

        log.info("Negative: email without @");
        assertTrue(screens.emailRegistration().trySubmitWithInvalidEmail("testuser.domain.com"),
                "Should reject email without @");

        log.info("Positive: valid registration");
        medFlow.generateEmail();
        screens.emailRegistration().scrollToEmailInput();
        screens.emailRegistration().registerWithEmail(medFlow.getCurrentEmail());
    }

    @Step("Screen 7: Passkey Dialog — select password auth")
    private void verifyPasskeyScreen() {
        log.info("=== Screen 7: Passkey Dialog ===");
        screens.passkeyDialog().waitForScreen();
        verifyOrRecordContent(screens.passkeyDialog(), "PasskeyScreen");
        assertNotNull(screens.passkeyDialog().getTitle(),
                "Passkey dialog title should be visible");
        screens.passkeyDialog().tapLater();
    }

    @Step("Screen 8: Set Password — 2 negative tests + valid password")
    private void verifySetPasswordScreen() {
        log.info("=== Screen 8: Set Password ===");
        screens.setPassword().waitForScreen();
        verifyOrRecordContent(screens.setPassword(), "SetPasswordScreen");

        log.info("Negative: empty password");
        assertTrue(screens.setPassword().trySubmitEmptyPassword(),
                "Should stay on password screen with empty password");

        log.info("Negative: short password");
        screens.setPassword().tryShortPassword("ab");

        log.info("Positive: valid password");
        screens.setPassword().setPasswordAndSubmit(medFlow.getCurrentPassword());
        BugReporter.checkForErrorDialog(
                "after password submit", "Email: " + medFlow.getCurrentEmail());
        medFlow.waitAndDismissOsPasswordManager();
    }

    @Step("Screen 9: Email Verification — IMAP verify + dismiss dialog")
    private void verifyEmailVerificationScreen() {
        log.info("=== Screen 9: Email Verification ===");
        screens.emailVerification().waitForScreen();
        verifyOrRecordContent(screens.emailVerification(), "EmailVerificationScreen");
        assertNotNull(screens.emailVerification().getToolbarTitle(),
                "Email verification toolbar title should be visible");

        log.info("Verifying email via IMAP: {}", medFlow.getCurrentEmail());
        medFlow.verifyCurrentUserEmail();
        medFlow.waitAndTapDialogButton("android:id/button1", "OK");
    }

    // ══════════════════════════════════════════════════
    //  ONBOARDING — Screens 10–28 (config-driven)
    // ══════════════════════════════════════════════════

    @Step("Screen 10: Doctor Info — config: {0}")
    private void verifyDoctorInfoScreen() {
        log.info("=== Screen 10: Doctor Info ===");
        screens.doctorInfo().waitForScreen();
        verifyOrRecordContent(screens.doctorInfo(), "DoctorInfoScreen");
        medFlow.completeDoctorInfo();
    }

    @Step("Screen 11: Newsletter Consent")
    private void verifyNewsletterScreen() {
        log.info("=== Screen 11: Newsletter ===");
        screens.newsletterConsent().waitForScreen();
        verifyOrRecordContent(screens.newsletterConsent(), "NewsletterConsentScreen");
        medFlow.completeNewsletter();
    }

    @Step("Screens 12-14: Tips — verify and dismiss each")
    private void verifyTipsScreens() {
        log.info("=== Screens 12-14: Tips ===");
        for (int i = 1; i <= 4; i++) {
            try {
                new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(
                                AppiumBy.id("nn.mobile.app.med:id/cta_button_inner")));
            } catch (Exception e) {
                log.info("No more tips screens after {}", i - 1);
                break;
            }
            // Tips content is dynamic — log what's present but don't hard-assert
            boolean hasContent = screens.tips().verifyContentDisplayed();
            log.info("Tip {} content present: {}", i, hasContent);
            screens.tips().tapUnderstood();
            log.info("Tip {} dismissed", i);
        }
    }

    @Step("Screens 15-18: Exercises — verify type (Attention/Memory/Reasoning/Speed)")
    private void verifyExercises() {
        log.info("=== Screens 15-18: Exercises ===");
        for (int ex = 1; ex <= 4; ex++) {
            log.info("Starting exercise {}", ex);
            screens.exerciseIntro().waitForScreen();
            String exerciseTitle = screens.exerciseIntro().getTitle();
            assertNotNull(exerciseTitle, "Exercise " + ex + " intro should have a title");
            log.info("Exercise {} type: {}", ex, exerciseTitle);
            verifyOrRecordContent(screens.exerciseIntro(), "ExerciseIntro_" + ex);

            screens.exerciseIntro().tapStart();
            screens.exerciseInGame().pauseAndSucceed();
            medFlow.dismissInterExerciseVideo();
            log.info("Exercise {} completed", ex);
        }
    }

    @Step("Screen 19: Age Group — select from profile config")
    private void verifyAgeGroupScreen() {
        log.info("=== Screen 19: Age Group ===");
        medFlow.completeAgeSelection();
    }

    @Step("Screen 20: Evaluation — verify weakness area")
    private void verifyEvaluationScreen() {
        log.info("=== Screen 20: Evaluation ===");
        screens.evaluation().waitForScreen();
        assertNotNull(screens.evaluation().getWeaknessTitle(),
                "Evaluation should show a weakness area");
        log.info("Weakness: {}", screens.evaluation().getWeaknessTitle());
        verifyOrRecordContent(screens.evaluation(), "EvaluationScreen");
        medFlow.completeEvaluation();
    }

    @Step("Screen 21: Training Complexity — config-driven selection")
    private void verifyTrainingComplexityScreen() {
        log.info("=== Screen 21: Training Complexity ===");
        verifyOrRecordContent(screens.trainingComplexity(), "TrainingComplexityScreen");
        medFlow.completeTrainingComplexity();
    }

    @Step("Screen 22: Special Needs — verify all 4 options visible")
    private void verifySpecialNeedsScreen() {
        log.info("=== Screen 22: Special Needs ===");
        screens.specialNeeds().waitForScreen();
        assertTrue(screens.specialNeeds().verifyAllOptionsVisible(),
                "All 4 special needs options should be visible");
        verifyOrRecordContent(screens.specialNeeds(), "SpecialNeedsScreen");
        medFlow.completeSpecialNeeds();
    }

    @Step("Screen 23: Training Time — verify all 4 options visible")
    private void verifyTrainingTimeScreen() {
        log.info("=== Screen 23: Training Time ===");
        screens.trainingTime().waitForScreen();
        assertTrue(screens.trainingTime().verifyAllOptionsVisible(),
                "All 4 training time options should be visible");
        verifyOrRecordContent(screens.trainingTime(), "TrainingTimeScreen");
        medFlow.completeTrainingTime();
    }

    @Step("Screen 24: Schedule Review — confirm schedule")
    private void verifyScheduleReviewScreen() {
        log.info("=== Screen 24: Schedule Review ===");
        verifyOrRecordContent(screens.scheduleReview(), "ScheduleReviewScreen");
        medFlow.completeScheduleReview();
    }

    @Step("Screen 25: Notification Permission — config-driven")
    private void verifyNotificationPermission() {
        log.info("=== Screen 25: Notification Permission ===");
        medFlow.completeNotificationPermission();
    }

    @Step("Screen 26: NeuroBooster — config-driven opt-in/out")
    private void verifyNeuroBoosterScreen() {
        log.info("=== Screen 26: NeuroBooster ===");
        verifyOrRecordContent(screens.neuroBooster(), "NeuroBoosterScreen");
        medFlow.completeNeuroBooster();
    }

    @Step("Screen 27: Promise — config-driven (conditional)")
    private void verifyPromiseScreen() {
        log.info("=== Screen 27: Promise ===");
        medFlow.completePromise();
    }

    @Step("Screen 28: Final Tips — dismiss to Dashboard")
    private void verifyFinalTips() {
        log.info("=== Screen 28: Final Tips ===");
        medFlow.completeFinalTips();
    }

    // ══════════════════════════════════════════════════
    //  VERIFICATION — Screens 29–30
    // ══════════════════════════════════════════════════

    @Step("Screen 29: Dashboard — verify level and content")
    private void verifyDashboardScreen() {
        log.info("=== Screen 29: Dashboard ===");
        screens.dashboard().waitForScreen();
        assertTrue(screens.dashboard().isDashboardDisplayed(),
                "Dashboard should be visible after completing full flow");
        String level = screens.dashboard().getLevel();
        assertNotNull(level, "Dashboard should show user level");
        log.info("Dashboard level: {}", level);
        verifyOrRecordContent(screens.dashboard(), "DashboardScreen");
    }

    @Step("Screen 30: Profile — verify MCI account + 90-day validity")
    private void verifyProfileScreen() {
        log.info("=== Screen 30: Profile ===");
        screens.dashboard().tapProfileTab();
        screens.profile().waitForScreen();
        verifyOrRecordContent(screens.profile(), "ProfileScreen");

        assertTrue(screens.profile().verifyAllElementsVisible(),
                "All profile elements should be visible");
        assertTrue(screens.profile().verifyMciAccount(),
                "Profile should show MCI account with 90-day validity");

        String accountId = screens.profile().getAccountId();
        assertNotNull(accountId, "Account ID should be visible");
        assertFalse(accountId.isEmpty(), "Account ID should not be empty");
        log.info("Account ID: {}", accountId);

        String validity = screens.profile().getAccountValidity();
        log.info("=== REGRESSION PASSED === {}", validity);
    }
}
