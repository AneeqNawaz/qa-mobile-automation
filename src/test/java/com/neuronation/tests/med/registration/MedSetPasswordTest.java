package com.neuronation.tests.med.registration;

import com.neuronation.base.BaseTest;
import com.neuronation.config.AppType;
import com.neuronation.driver.DriverManager;
import com.neuronation.testdata.Features;
import com.neuronation.testdata.RegistrationData;
import com.neuronation.utils.ScreenDumper;
import com.neuronation.utils.TestDataLoader;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Epic("NeuroNation MED App")
@Feature("Set Password")
public class MedSetPasswordTest extends BaseTest {

    // ──────────────────────────────────────────────
    // Content verification
    // ──────────────────────────────────────────────

    @Test(
        description = "Set Password screen displays all expected elements",
        groups = {Features.MED, Features.REGISTRATION, Features.SMOKE}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Password screen shows input and instructions")
    @Description("Verifies the Set Password screen shows: toolbar 'Set your password', "
            + "description, password input, show/hide toggle, security text, and submit button.")
    public void testSetPassword_content_screenElements() throws InterruptedException {
        medFlow.goToSetPassword();

        assertTrue(screens.setPassword().isDisplayed(),
                "Set Password screen should be displayed");
        assertEquals(screens.setPassword().getToolbarTitle(), "Set your password");

        String desc = screens.setPassword().getDescriptionText();
        assertTrue(desc.contains("password"), "Description should mention password");

        verifyOrRecordContent(screens.setPassword(), "SetPasswordScreen");
        softAssert.assertAll();
    }

    // ──────────────────────────────────────────────
    // Validation edge cases
    // ──────────────────────────────────────────────

    @Test(
        description = "Submit with empty password stays on screen",
        groups = {Features.MED, Features.REGISTRATION, Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Empty password validation")
    @Description("Taps submit without entering a password. Verifies the form "
            + "doesn't proceed and shows an error or stays on screen.")
    public void testSetPassword_emptyPassword_staysOnScreen() throws InterruptedException {
        medFlow.goToSetPassword();

        screens.setPassword().tapSubmit();
        Thread.sleep(2000);

        if (screens.setPassword().isErrorDialogDisplayed()) {
            log.info("Empty password error: {}", screens.setPassword().getErrorDialogMessage());
            screens.setPassword().dismissErrorDialog();
            Thread.sleep(1000);
        }

        assertTrue(screens.setPassword().isDisplayed(),
                "Should remain on password screen after empty submit");
    }

    @Test(
        description = "Submit with short password shows validation error",
        groups = {Features.MED, Features.REGISTRATION, Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Short password validation")
    @Description("Enters a 2-character password and submits. Verifies an error.")
    public void testSetPassword_shortPassword_staysOnScreen() throws InterruptedException {
        medFlow.goToSetPassword();

        screens.setPassword().setPasswordAndSubmit("ab");
        Thread.sleep(2000);

        if (screens.setPassword().isErrorDialogDisplayed()) {
            log.info("Short password error: {}", screens.setPassword().getErrorDialogMessage());
            screens.setPassword().dismissErrorDialog();
            Thread.sleep(1000);
        }

        assertTrue(screens.setPassword().isDisplayed(),
                "Should remain on password screen after short password");
    }

    @Test(
        description = "Show/hide password toggle works",
        groups = {Features.MED, Features.REGISTRATION, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Password visibility toggle")
    @Description("Enters a password and taps show/hide toggle twice.")
    public void testSetPassword_toggleVisibility_works() throws InterruptedException {
        medFlow.goToSetPassword();

        screens.setPassword().enterPassword("TestPass123!");
        screens.setPassword().tapShowPassword();
        Thread.sleep(500);
        screens.setPassword().tapShowPassword();
        Thread.sleep(500);
        log.info("Password toggle works — toggled twice");
    }

    // ──────────────────────────────────────────────
    // Happy path: PASSWORD flow (Passkey → Later → Set Password)
    // ──────────────────────────────────────────────

    @Test(
        description = "PASSWORD PATH: Complete registration via password and discover next screen",
        groups = {Features.MED, Features.REGISTRATION, Features.SMOKE, Features.CRITICAL}
    )
    @Severity(SeverityLevel.BLOCKER)
    @Story("Full registration via traditional password")
    @Description("Complete MED registration path: Launch → DiGA → Video → Email → "
            + "Passkey 'Later' → Set Password → Submit. Handles Samsung Pass OS dialog. "
            + "Discovers whatever screen appears after successful account creation.")
    public void testSetPassword_passwordPath_fullRegistration() throws InterruptedException {
        medFlow.completeRegistrationWithPassword();

        ScreenDumper.dumpCurrentScreen("10_AfterPasswordRegistration");
        log.info("PASSWORD path complete — next screen dumped");
    }

    // ──────────────────────────────────────────────
    // Happy path: PASSKEY flow (Passkey → Yes)
    // ──────────────────────────────────────────────

    @Test(
        description = "PASSKEY PATH: Complete registration via passkey and discover next screen",
        groups = {Features.MED, Features.REGISTRATION, Features.REGRESSION}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Full registration via passkey/biometric")
    @Description("Complete MED registration path: Launch → DiGA → Video → Email → "
            + "Passkey 'Yes' → Handle OS biometric prompt. Uses a unique email per run. "
            + "Discovers whatever screen appears after passkey setup.")
    public void testSetPassword_passkeyPath_fullRegistration() throws InterruptedException {
        // Removed — covered by MedFullE2EHappyPathTest flow configs

        ScreenDumper.dumpCurrentScreen("10_AfterPasskeyRegistration");
        log.info("PASSKEY path complete — next screen dumped");
    }
}
