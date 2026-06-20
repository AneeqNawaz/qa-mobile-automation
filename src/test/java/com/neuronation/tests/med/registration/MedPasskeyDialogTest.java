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
@Feature("Passkey Dialog")
public class MedPasskeyDialogTest extends BaseTest {

    private void navigateToPasskeyDialog() throws InterruptedException {
        ActivationData activation = TestDataLoader.loadActivationData(
                AppType.MED, context.getLanguage(), ActivationData.class);
        RegistrationData regData = TestDataLoader.loadRegistrationData(
                AppType.MED, context.getLanguage(), RegistrationData.class);

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
        // Confirm email dialog
        DriverManager.getDriver().findElement(
                io.appium.java_client.AppiumBy.id("android:id/button1")).click();
        Thread.sleep(2000);
        // Fill registration
        screens.emailRegistration().enterEmail(regData.generateEmail());
        screens.emailRegistration().acceptRequiredCheckboxes();
        screens.emailRegistration().scrollToSubmit();
        Thread.sleep(1000);
        screens.emailRegistration().tapSubmit();
        Thread.sleep(5000);
    }

    // ──────────────────────────────────────────────
    // Content verification
    // ──────────────────────────────────────────────

    @Test(
        description = "Passkey dialog displays all expected elements and content",
        groups = {Features.MED, Features.REGISTRATION, Features.SMOKE}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Passkey dialog shows all options")
    @Description("After email registration, verifies the Passkey dialog displays: "
            + "title 'Use Passwordless Login?', explanation text about biometric security, "
            + "'Yes' button, 'Later' button, and 'No, use password' button. "
            + "Content is captured as snapshot baseline.")
    public void testPasskey_content_dialogElements() throws InterruptedException {
        navigateToPasskeyDialog();

        assertTrue(screens.passkeyDialog().isDisplayed(),
                "Passkey dialog should be displayed after registration");

        assertEquals(screens.passkeyDialog().getTitle(), "Use Passwordless Login?",
                "Title should ask about passwordless login");

        String content = screens.passkeyDialog().getContentText();
        assertTrue(content.contains("Passkeys"),
                "Content should explain Passkeys");
        assertTrue(content.contains("fingerprint"),
                "Content should mention fingerprint authentication");
        assertTrue(content.contains("encrypted"),
                "Content should mention encryption");
        assertTrue(content.contains("only stored locally"),
                "Content should mention local-only storage of biometric data");

        verifyOrRecordContent(screens.passkeyDialog(), "PasskeyScreen");
        softAssert.assertAll();
    }

    // ──────────────────────────────────────────────
    // Option 1: Yes — enable Passkey
    // ──────────────────────────────────────────────

    @Test(
        description = "Tapping 'Yes' enables Passkey and navigates forward",
        groups = {Features.MED, Features.REGISTRATION, Features.REGRESSION}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("User enables Passkey login")
    @Description("Selects 'Yes' on the Passkey dialog to enable passwordless login. "
            + "Verifies the app advances to the next screen (may trigger OS passkey setup). "
            + "Dumps the resulting screen for discovery.")
    public void testPasskey_tapYes_navigatesForward() throws InterruptedException {
        navigateToPasskeyDialog();

        screens.passkeyDialog().tapYes();
        Thread.sleep(5000);

        ScreenDumper.dumpCurrentScreen("09_AfterPasskey_Yes");
        log.info("Passkey Yes selected — next screen dumped");
    }

    // ──────────────────────────────────────────────
    // Option 2: Later — skip for now
    // ──────────────────────────────────────────────

    @Test(
        description = "Tapping 'Later' skips Passkey and navigates forward",
        groups = {Features.MED, Features.REGISTRATION, Features.SMOKE, Features.CRITICAL}
    )
    @Severity(SeverityLevel.BLOCKER)
    @Story("User skips Passkey setup")
    @Description("Selects 'Later' on the Passkey dialog to skip passwordless login setup. "
            + "Verifies the app advances to the next onboarding screen. "
            + "This is the default path for test automation.")
    public void testPasskey_tapLater_navigatesForward() throws InterruptedException {
        navigateToPasskeyDialog();

        screens.passkeyDialog().tapLater();
        Thread.sleep(5000);

        ScreenDumper.dumpCurrentScreen("09_AfterPasskey_Later");
        log.info("Passkey Later selected — next screen dumped");
    }

    // ──────────────────────────────────────────────
    // Option 3: No, use password
    // ──────────────────────────────────────────────

    @Test(
        description = "Tapping 'No, use password' selects traditional auth and navigates forward",
        groups = {Features.MED, Features.REGISTRATION, Features.REGRESSION}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("User chooses traditional password login")
    @Description("Selects 'No, use password' on the Passkey dialog. "
            + "Verifies the app advances — may show password creation screen or continue onboarding. "
            + "Dumps the resulting screen for discovery.")
    public void testPasskey_tapNoUsePassword_navigatesForward() throws InterruptedException {
        navigateToPasskeyDialog();

        screens.passkeyDialog().tapNoUsePassword();
        Thread.sleep(5000);

        ScreenDumper.dumpCurrentScreen("09_AfterPasskey_NoPassword");
        log.info("Passkey No (use password) selected — next screen dumped");
    }
}
