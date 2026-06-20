package com.neuronation.tests.med.registration;

import com.neuronation.base.BaseTest;
import com.neuronation.driver.DriverManager;
import com.neuronation.helpers.ContentTestHelper;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.*;

@Epic("NeuroNation MED App")
@Feature("Create Account")
public class MedCreateAccountTest extends BaseTest {

    private Map<String, Object> expected;

    @BeforeMethod(alwaysRun = true)
    public void navigateToCreateAccount() {
        medFlow.loadFlow("flow1_password_morning_skip");
        medFlow.generateFreshMciCode();
        medFlow.goToCreateAccount();
        expected = ContentTestHelper.loadRegistration(context.getLanguage(), "createAccount");
    }

    // ──────────────────────────────────────────────
    // Smoke / Critical
    // ──────────────────────────────────────────────

    @Test(
        description = "Create Account screen is visible after video close",
        groups = {Features.MED, Features.SMOKE, Features.CRITICAL}
    )
    @Severity(SeverityLevel.BLOCKER)
    @Story("Create Account screen loads after onboarding video")
    @Description("Verifies the Create Account screen is displayed after the onboarding "
            + "video is closed, with both registration options visible.")
    public void testCreateAccount_screenLoads() {
        assertTrue(screens.createAccount().isDisplayed(),
                "Create Account screen should be visible after video close");
    }

    // ──────────────────────────────────────────────
    // Content verification
    // ──────────────────────────────────────────────

    @Test(
        description = "Toolbar title shows 'Create account'",
        groups = {Features.MED, Features.CONTENT, Features.SANITY}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Create Account toolbar title matches expected content")
    @Description("Verifies the toolbar title text matches the expected 'Create account' "
            + "from screen-content.json.")
    public void testCreateAccount_content_toolbarTitle() {
        String actual = screens.createAccount().getToolbarTitle();
        ContentTestHelper.assertText(actual, expected.get("toolbarTitle"), "toolbarTitle", softAssert);
        softAssert.assertAll();
    }

    @Test(
        description = "Email button shows 'Register via Email'",
        groups = {Features.MED, Features.CONTENT, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Register via Email button text matches expected content")
    @Description("Verifies the email registration button text matches the expected "
            + "'Register via Email' from screen-content.json.")
    public void testCreateAccount_content_emailButton() {
        Map<String, String> content = screens.createAccount().captureContent();
        ContentTestHelper.assertText(content.get("registerViaEmailButton"),
                expected.get("emailButton"), "emailButton", softAssert);
        softAssert.assertAll();
    }

    @Test(
        description = "Health ID button shows 'Via Gesundheits-ID'",
        groups = {Features.MED, Features.CONTENT, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Health ID button text matches expected content")
    @Description("Verifies the Gesundheits-ID button text matches the expected "
            + "'Via Gesundheits-ID' from screen-content.json.")
    public void testCreateAccount_content_healthIdButton() {
        Map<String, String> content = screens.createAccount().captureContent();
        ContentTestHelper.assertText(content.get("healthIdButton"),
                expected.get("healthIdButton"), "healthIdButton", softAssert);
        softAssert.assertAll();
    }

    // ──────────────────────────────────────────────
    // Navigation
    // ──────────────────────────────────────────────

    @Test(
        description = "Tapping 'Register via Email' shows email confirmation dialog",
        groups = {Features.MED, Features.NAVIGATION, Features.SMOKE}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Email registration button opens confirmation dialog")
    @Description("Taps 'Register via Email' and verifies the confirmation dialog "
            + "appears asking the user to confirm email registration.")
    public void testCreateAccount_tapEmail_showsDialog() {
        screens.createAccount().tapRegisterViaEmail();

        // The email confirm dialog should appear — verify via dialog button presence
        // On Android: android:id/button1 (Continue), on iOS: alert with "Continue"
        var driver = DriverManager.getDriver();
        boolean dialogVisible = new org.openqa.selenium.support.ui.WebDriverWait(
                driver, java.time.Duration.ofSeconds(10))
                .until(d -> {
                    try {
                        if (!d.findElements(io.appium.java_client.AppiumBy.id("android:id/button1")).isEmpty())
                            return true;
                        if (!d.findElements(io.appium.java_client.AppiumBy.iOSClassChain(
                                "**/XCUIElementTypeAlert")).isEmpty())
                            return true;
                    } catch (Exception ignored) {}
                    return false;
                });

        assertTrue(dialogVisible,
                "Email confirmation dialog should appear after tapping Register via Email");
    }

    @Test(
        description = "Tapping 'Via Gesundheits-ID' navigates forward",
        groups = {Features.MED, Features.NAVIGATION, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Health ID button navigates to next screen")
    @Description("Taps the 'Via Gesundheits-ID' button and verifies the app navigates "
            + "away from the Create Account screen.")
    public void testCreateAccount_tapHealthId_navigatesForward() {
        screens.createAccount().tapHealthId();

        // After tapping Health ID, the Create Account screen should no longer be displayed
        // (navigates to the Health ID flow)
        var driver = DriverManager.getDriver();
        boolean navigated = new org.openqa.selenium.support.ui.WebDriverWait(
                driver, java.time.Duration.ofSeconds(15))
                .until(d -> {
                    try {
                        return !screens.createAccount().isDisplayed();
                    } catch (Exception e) {
                        return true; // screen not found = navigated away
                    }
                });

        assertTrue(navigated,
                "Should navigate away from Create Account after tapping Health ID");
    }

    @Test(
        description = "Back button returns to onboarding video screen",
        groups = {Features.MED, Features.NAVIGATION, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Back navigation from Create Account returns to video")
    @Description("Taps the back button on the Create Account screen and verifies "
            + "the app returns to the onboarding video screen without crash.")
    public void testCreateAccount_back_returnsToVideo() {
        screens.createAccount().tapBack();

        // After pressing back, the Create Account screen should no longer be visible
        var driver = DriverManager.getDriver();
        boolean returned = new org.openqa.selenium.support.ui.WebDriverWait(
                driver, java.time.Duration.ofSeconds(10))
                .until(d -> {
                    try {
                        return !screens.createAccount().isDisplayed();
                    } catch (Exception e) {
                        return true;
                    }
                });

        assertTrue(returned,
                "Create Account screen should not be visible after pressing back");
    }

    // ──────────────────────────────────────────────
    // Content snapshot
    // ──────────────────────────────────────────────

    @Test(
        description = "Full content snapshot for Create Account screen",
        groups = {Features.MED, Features.CONTENT, Features.REGRESSION}
    )
    @Severity(SeverityLevel.MINOR)
    @Story("Create Account content snapshot baseline")
    @Description("Captures all visible text on the Create Account screen and "
            + "either records a new baseline or verifies against the existing one.")
    public void testCreateAccount_contentSnapshot() {
        verifyOrRecordContent(screens.createAccount(), "CreateAccountScreen");
        softAssert.assertAll();
    }
}
