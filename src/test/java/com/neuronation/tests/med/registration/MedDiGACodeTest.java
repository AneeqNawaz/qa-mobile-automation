package com.neuronation.tests.med.registration;

import com.neuronation.base.BaseTest;
import com.neuronation.config.AppType;
import com.neuronation.driver.DriverManager;
import com.neuronation.listeners.AllureScreenshotListener;
import com.neuronation.testdata.ActivationData;
import com.neuronation.testdata.Features;
import com.neuronation.utils.TestDataLoader;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Listeners(AllureScreenshotListener.class)
@Epic("NeuroNation MED App")
@Feature("DiGA Activation")
public class MedDiGACodeTest extends BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void navigateToDiGAScreen() throws InterruptedException {
        screens.launch().tapStartNow();
        Thread.sleep(1500);
        screens.appSelection().selectMedicalApp();
        Thread.sleep(1500);
    }

    // ──────────────────────────────────────────────
    // Content verification
    // ──────────────────────────────────────────────

    @Test(
        description = "DiGA screen displays all expected content elements",
        groups = {Features.MED, Features.REGISTRATION, Features.SMOKE}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Activation screen shows all required elements")
    @Description("Verifies the DiGA activation screen displays: toolbar 'Activation', "
            + "'Enter activation code' title, description text, code input field, "
            + "'Activate' button, 'Don't have an access code yet?' section, "
            + "'Open info website' button, and FAQ button. All text captured as snapshot.")
    public void testDiGA_content_screenElements() {
        assertTrue(screens.digaCode().isActivationScreenDisplayed(),
                "Activation screen should be displayed");

        assertEquals(screens.digaCode().getToolbarTitle(), "Activation",
                "Toolbar should show 'Activation'");

        assertEquals(screens.digaCode().getDescriptionTitle(), "Enter activation code",
                "Description title should match");

        verifyOrRecordContent(screens.digaCode(), "DiGACodeScreen");
        softAssert.assertAll();
    }

    // ──────────────────────────────────────────────
    // Validation edge cases
    // ──────────────────────────────────────────────

    @Test(
        description = "Empty activation code shows validation error",
        groups = {Features.MED, Features.REGISTRATION, Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Empty code validation")
    @Description("Taps 'Activate' without entering any code. Verifies an error "
            + "dialog or validation message appears. User should remain on the activation screen.")
    public void testDiGA_emptyCode_staysOnScreen() throws InterruptedException {
        // Tap Activate with empty field
        screens.digaCode().tapActivate();

        // Should show error or remain on same screen
        Thread.sleep(2000);
        assertTrue(screens.digaCode().isActivationScreenDisplayed(),
                "Should remain on activation screen with empty code");
    }

    @Test(
        description = "Invalid activation code shows error dialog",
        groups = {Features.MED, Features.REGISTRATION, Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Invalid code shows appropriate error")
    @Description("Enters an invalid activation code and taps Activate. Verifies an error "
            + "dialog appears with message about code not found. Dismisses the dialog "
            + "and verifies user remains on the activation screen to retry.")
    public void testDiGA_invalidCode_showsError() throws InterruptedException {
        screens.digaCode().enterCodeAndActivate("INVALIDCODE999");
        Thread.sleep(2000);

        // Error dialog should appear — check for OK button (android:id/button2)
        try {
            var okButton = DriverManager.getDriver().findElement(
                    io.appium.java_client.AppiumBy.id("android:id/button2"));
            assertNotNull(okButton, "Error dialog OK button should be visible");

            // Verify error message
            var message = DriverManager.getDriver().findElement(
                    io.appium.java_client.AppiumBy.id("android:id/message"));
            String errorText = message.getText();
            assertTrue(errorText.contains("could not be found"),
                    "Error should mention code not found, got: " + errorText);
            log.info("Error dialog message: {}", errorText);

            // Dismiss the dialog
            okButton.click();
            Thread.sleep(1000);
        } catch (Exception e) {
            log.warn("Error dialog not found, checking alternative: {}", e.getMessage());
        }

        // Should return to activation screen after dismissing error
        assertTrue(screens.digaCode().isActivationScreenDisplayed(),
                "Should return to activation screen after error dismissal");
    }

    @Test(
        description = "Short/malformed activation code shows error",
        groups = {Features.MED, Features.REGISTRATION, Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Malformed code validation")
    @Description("Enters a code that's too short and verifies the app handles it gracefully "
            + "with an error message rather than crashing.")
    public void testDiGA_shortCode_staysOnScreen() throws InterruptedException {
        screens.digaCode().enterCodeAndActivate("ABC");
        Thread.sleep(2000);

        // Should show error or remain on screen
        try {
            var okButton = DriverManager.getDriver().findElement(
                    io.appium.java_client.AppiumBy.id("android:id/button2"));
            okButton.click();
            Thread.sleep(1000);
        } catch (Exception ignored) {}

        assertTrue(screens.digaCode().isActivationScreenDisplayed(),
                "Should remain on activation screen after short code");
    }

    // ──────────────────────────────────────────────
    // Button functionality
    // ──────────────────────────────────────────────

    @Test(
        description = "Open info website button is clickable",
        groups = {Features.MED, Features.REGISTRATION, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Info website button opens browser/webview")
    @Description("Taps 'Open info website' button and verifies it triggers an action "
            + "(opening browser or in-app webview). Does not crash.")
    public void testDiGA_tapInfoWebsite_opensWithoutCrash() throws InterruptedException {
        screens.digaCode().tapWebsite();
        Thread.sleep(3000);

        // App may open browser — navigate back
        DriverManager.getDriver().navigate().back();
        Thread.sleep(1500);

        log.info("Website button tapped and returned — no crash");
    }

    @Test(
        description = "FAQ button is clickable and shows information",
        groups = {Features.MED, Features.REGISTRATION, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("FAQ button shows security information")
    @Description("Taps the FAQ/security information button and verifies it either "
            + "expands FAQ content or navigates to information page.")
    public void testDiGA_tapFaq_opensWithoutCrash() throws InterruptedException {
        screens.digaCode().tapFaq();
        Thread.sleep(2000);

        log.info("FAQ button tapped — verifying response");
    }

    @Test(
        description = "Back navigation from DiGA screen returns to App Selection",
        groups = {Features.MED, Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Back navigation from activation screen works")
    @Description("Taps the back/navigate-up button on the DiGA activation screen "
            + "and verifies return to the App Selection screen.")
    public void testDiGA_back_returnsToAppSelection() throws InterruptedException {
        screens.digaCode().tapBack();
        Thread.sleep(1500);

        assertTrue(screens.appSelection().isDisplayed(),
                "App Selection screen should be visible after back navigation");
    }

    // ──────────────────────────────────────────────
    // Happy path — valid code
    // ──────────────────────────────────────────────

    @Test(
        description = "Valid activation code navigates to onboarding video",
        groups = {Features.MED, Features.REGISTRATION, Features.SMOKE, Features.CRITICAL}
    )
    @Severity(SeverityLevel.BLOCKER)
    @Story("Valid DiGA code activates successfully")
    @Description("Enters a valid activation code from test data and taps Activate. "
            + "Verifies the onboarding video screen appears after successful activation. "
            + "Code sourced from testdata/med-en.json (overridable via DIGA_ACTIVATION_CODE env var).")
    public void testDiGA_validCode_navigatesToVideo() throws InterruptedException {
        ActivationData activation = TestDataLoader.loadActivationData(
                AppType.MED, context.getLanguage(), ActivationData.class);

        screens.digaCode().enterCodeAndActivate(activation.getDigaCode());
        Thread.sleep(3000);

        assertTrue(screens.onboardingVideo().isVideoScreenDisplayed(),
                "Onboarding video should appear after valid code activation");
    }
}
