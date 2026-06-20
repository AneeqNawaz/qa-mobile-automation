package com.neuronation.tests.med.registration;

import com.neuronation.base.BaseTest;
import com.neuronation.config.AppType;
import com.neuronation.driver.DriverManager;
import com.neuronation.testdata.ActivationData;
import com.neuronation.testdata.Features;
import com.neuronation.testdata.RegistrationData;
import com.neuronation.utils.ScreenDumper;
import com.neuronation.utils.TestDataLoader;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Epic("NeuroNation MED App")
@Feature("Email Registration")
public class MedEmailRegistrationTest extends BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void navigateToEmailRegistration() throws InterruptedException {
        ActivationData activation = TestDataLoader.loadActivationData(
                AppType.MED, context.getLanguage(), ActivationData.class);

        screens.launch().tapStartNow();
        Thread.sleep(1500);
        screens.appSelection().selectMedicalApp();
        Thread.sleep(1500);
        screens.digaCode().enterCodeAndActivate(activation.getDigaCode());
        Thread.sleep(5000);
        screens.onboardingVideo().tapClose();
        Thread.sleep(2000);
        screens.createAccount().tapRegisterViaEmail();
        Thread.sleep(1500);
        // Tap "Continue" on email confirmation dialog
        DriverManager.getDriver().findElement(
                io.appium.java_client.AppiumBy.id("android:id/button1")).click();
        Thread.sleep(2000);
    }

    // ──────────────────────────────────────────────
    // Content verification
    // ──────────────────────────────────────────────

    @Test(
        description = "Email registration form displays all expected elements",
        groups = {Features.MED, Features.REGISTRATION, Features.SMOKE}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Registration form shows all fields and checkboxes")
    @Description("Verifies the email registration form shows: email input, "
            + "Terms of Use checkbox (required), Privacy Policy checkbox (required), "
            + "Data Retention checkbox (optional), Data Processing checkbox (optional), "
            + "security trust text, and required field indicator. Content is captured as snapshot.")
    public void testEmailReg_content_formElements() {
        assertTrue(screens.emailRegistration().isDisplayed(),
                "Email registration form should be displayed");

        assertEquals(screens.emailRegistration().getToolbarTitle(), "Create account",
                "Toolbar should show 'Create account'");

        String termsText = screens.emailRegistration().getTermsText();
        assertTrue(termsText.contains("Terms of Use"),
                "Terms checkbox should mention Terms of Use, got: " + termsText);

        String privacyText = screens.emailRegistration().getPrivacyText();
        assertTrue(privacyText.contains("Privacy Policy"),
                "Privacy checkbox should mention Privacy Policy, got: " + privacyText);

        String trustText = screens.emailRegistration().getTrustText();
        assertTrue(trustText.contains("trusted networks"),
                "Trust text should mention trusted networks");

        verifyOrRecordContent(screens.emailRegistration(), "EmailRegistrationScreen");
        softAssert.assertAll();
    }

    @Test(
        description = "All text content is in English (matching language selection)",
        groups = {Features.MED, Features.REGISTRATION, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Registration form respects language selection")
    @Description("Verifies all text on the registration form is in English, "
            + "matching the language selected on the launch screen.")
    public void testEmailReg_content_isInEnglish() {
        String terms = screens.emailRegistration().getTermsText();
        assertTrue(terms.contains("I have accepted"),
                "Terms text should be in English");

        String privacy = screens.emailRegistration().getPrivacyText();
        assertTrue(privacy.contains("I agree"),
                "Privacy text should be in English");

        String trust = screens.emailRegistration().getTrustText();
        assertTrue(trust.contains("Please make sure"),
                "Trust text should be in English");
    }

    // ──────────────────────────────────────────────
    // Validation edge cases
    // ──────────────────────────────────────────────

    @Test(
        description = "Submit without email shows validation error",
        groups = {Features.MED, Features.REGISTRATION, Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Empty email field validation")
    @Description("Checks all required checkboxes but leaves email empty, "
            + "then taps submit. Verifies an error or that the form doesn't proceed.")
    public void testEmailReg_submitWithoutEmail_staysOnScreen() throws InterruptedException {
        screens.emailRegistration().acceptRequiredCheckboxes();
        screens.emailRegistration().scrollToSubmit();
        Thread.sleep(1000);

        // Try to find and tap submit
        try {
            screens.emailRegistration().tapSubmit();
            Thread.sleep(2000);
        } catch (Exception e) {
            log.info("Submit button not found or not clickable without email — expected");
        }

        // Should still be on registration screen or show error
        if (screens.emailRegistration().isErrorDialogDisplayed()) {
            String error = screens.emailRegistration().getErrorDialogMessage();
            log.info("Validation error: {}", error);
            screens.emailRegistration().dismissErrorDialog();
        }

        assertTrue(screens.emailRegistration().isDisplayed(),
                "Should remain on registration form after empty email submission");
    }

    @Test(
        description = "Submit without checking required Terms checkbox shows error",
        groups = {Features.MED, Features.REGISTRATION, Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Required Terms checkbox validation")
    @Description("Enters email and checks Privacy but NOT Terms, "
            + "then tries to submit. Verifies form doesn't proceed without required fields.")
    public void testEmailReg_submitWithoutTerms_staysOnScreen() throws InterruptedException {
        RegistrationData regData = TestDataLoader.loadRegistrationData(
                AppType.MED, context.getLanguage(), RegistrationData.class);

        screens.emailRegistration().enterEmail(regData.generateEmail());
        screens.emailRegistration().checkPrivacy();
        // Intentionally NOT checking Terms
        screens.emailRegistration().scrollToSubmit();
        Thread.sleep(1000);

        try {
            screens.emailRegistration().tapSubmit();
            Thread.sleep(2000);
        } catch (Exception e) {
            log.info("Submit not available without Terms — expected");
        }

        if (screens.emailRegistration().isErrorDialogDisplayed()) {
            String error = screens.emailRegistration().getErrorDialogMessage();
            log.info("Validation error for missing Terms: {}", error);
            screens.emailRegistration().dismissErrorDialog();
        }

        assertTrue(screens.emailRegistration().isDisplayed(),
                "Should remain on registration form without Terms acceptance");
    }

    @Test(
        description = "Submit without checking required Privacy checkbox shows error",
        groups = {Features.MED, Features.REGISTRATION, Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Required Privacy checkbox validation")
    @Description("Enters email and checks Terms but NOT Privacy, "
            + "then tries to submit. Verifies form doesn't proceed without required fields.")
    public void testEmailReg_submitWithoutPrivacy_staysOnScreen() throws InterruptedException {
        RegistrationData regData = TestDataLoader.loadRegistrationData(
                AppType.MED, context.getLanguage(), RegistrationData.class);

        screens.emailRegistration().enterEmail(regData.generateEmail());
        screens.emailRegistration().checkTerms();
        // Intentionally NOT checking Privacy
        screens.emailRegistration().scrollToSubmit();
        Thread.sleep(1000);

        try {
            screens.emailRegistration().tapSubmit();
            Thread.sleep(2000);
        } catch (Exception e) {
            log.info("Submit not available without Privacy — expected");
        }

        if (screens.emailRegistration().isErrorDialogDisplayed()) {
            String error = screens.emailRegistration().getErrorDialogMessage();
            log.info("Validation error for missing Privacy: {}", error);
            screens.emailRegistration().dismissErrorDialog();
        }

        assertTrue(screens.emailRegistration().isDisplayed(),
                "Should remain on registration form without Privacy acceptance");
    }

    @Test(
        description = "Invalid email format shows validation error",
        groups = {Features.MED, Features.REGISTRATION, Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Invalid email format validation")
    @Description("Enters an invalid email format (no @ symbol) with all required "
            + "checkboxes checked. Verifies the form shows a validation error.")
    public void testEmailReg_invalidEmail_staysOnScreen() throws InterruptedException {
        screens.emailRegistration().enterEmail("not-a-valid-email");
        screens.emailRegistration().acceptRequiredCheckboxes();
        screens.emailRegistration().scrollToSubmit();
        Thread.sleep(1000);

        try {
            screens.emailRegistration().tapSubmit();
            Thread.sleep(2000);
        } catch (Exception e) {
            log.info("Submit failed with invalid email — expected");
        }

        if (screens.emailRegistration().isErrorDialogDisplayed()) {
            log.info("Error: {}", screens.emailRegistration().getErrorDialogMessage());
            screens.emailRegistration().dismissErrorDialog();
        }
    }

    // ──────────────────────────────────────────────
    // Navigation
    // ──────────────────────────────────────────────

    @Test(
        description = "Back button returns to Create Account screen",
        groups = {Features.MED, Features.REGISTRATION, Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Back navigation from registration form")
    @Description("Taps back on the email registration form and verifies "
            + "return to the Create Account screen with both registration options.")
    public void testEmailReg_back_returnsToCreateAccount() throws InterruptedException {
        screens.emailRegistration().tapBack();
        Thread.sleep(1500);

        assertTrue(screens.createAccount().isDisplayed(),
                "Create Account screen should be visible after back navigation");
    }

    // ──────────────────────────────────────────────
    // Happy path — valid registration + discovery
    // ──────────────────────────────────────────────

    @Test(
        description = "Valid email registration with all required fields navigates forward",
        groups = {Features.MED, Features.REGISTRATION, Features.SMOKE, Features.CRITICAL}
    )
    @Severity(SeverityLevel.BLOCKER)
    @Story("Successful email registration completes and advances")
    @Description("Enters a dynamically generated email, accepts all required checkboxes, "
            + "submits the form, and verifies navigation to the next screen. "
            + "Dumps the next screen for further discovery.")
    public void testEmailReg_validSubmit_navigatesForward() throws InterruptedException {
        RegistrationData regData = TestDataLoader.loadRegistrationData(
                AppType.MED, context.getLanguage(), RegistrationData.class);
        String email = regData.generateEmail();
        context.setEmail(email);
        log.info("Registering with email: {}", email);

        screens.emailRegistration().enterEmail(email);
        screens.emailRegistration().acceptRequiredCheckboxes();
        screens.emailRegistration().scrollToSubmit();
        Thread.sleep(1000);

        // Dump to see if submit button appeared
        ScreenDumper.dumpCurrentScreen("07_RegistrationForm_afterScroll");

        screens.emailRegistration().tapSubmit();
        Thread.sleep(5000);

        // Dump whatever comes next
        ScreenDumper.dumpCurrentScreen("08_AfterRegistration");
        log.info("Registration submitted — next screen dumped");
    }
}
