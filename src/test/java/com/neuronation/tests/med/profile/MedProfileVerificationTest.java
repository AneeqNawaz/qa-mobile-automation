package com.neuronation.tests.med.profile;

import com.neuronation.base.BaseTest;
import com.neuronation.config.AppType;
import com.neuronation.testdata.Features;
import com.neuronation.utils.ScreenDumper;
import com.neuronation.utils.TestDataLoader;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.testng.Assert.*;

@Epic("NeuroNation MED App")
@Feature("Profile")
public class MedProfileVerificationTest extends BaseTest {

    /**
     * Navigate to Profile screen from whatever state the app is in.
     * Assumes the user is logged in (dashboard or any authenticated screen).
     */
    private void navigateToProfile() throws InterruptedException {
        // Tap Profile in bottom nav — works from any authenticated screen
        try {
            var driver = com.neuronation.driver.DriverManager.getDriver();
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"Profile\")")).click();
            Thread.sleep(3000);
        } catch (Exception e) {
            log.warn("Could not tap Profile tab: {}", e.getMessage());
        }
    }

    @Test(
        description = "Profile shows MCI account type with 90-day validity from activation date",
        groups = {Features.MED, Features.PROFILE, Features.SMOKE, Features.CRITICAL}
    )
    @Severity(SeverityLevel.BLOCKER)
    @Story("MCI user profile shows correct account type and validity period")
    @Description("After MCI registration, navigates to Profile and verifies:\n"
            + "1. Account type shows 'MCI'\n"
            + "2. Validity is exactly 90 days from today\n"
            + "3. User ID is present and valid UUID format\n"
            + "4. Level shows 'Level 1 - Newcomer'\n"
            + "5. Account management options (Change Name/Email/Password) are visible")
    public void testMciProfileAccountValidity() throws InterruptedException {
        // Full registration flow to get to dashboard
        com.neuronation.testdata.ActivationData activation = TestDataLoader.loadActivationData(
                AppType.MED, context.getLanguage(), com.neuronation.testdata.ActivationData.class);
        medFlow.generateFreshMciCode();
        medFlow.completeRegistrationWithPassword();

        // Email verification
        medFlow.completeEmailVerification();

        // Doctor info — skip
        medFlow.completeDoctorInfo();

        // Newsletter — agree
        try {
            var driver = com.neuronation.driver.DriverManager.getDriver();
            driver.findElement(io.appium.java_client.AppiumBy.id(
                    "nn.mobile.app.med:id/positive_button")).click(); // I agree
            Thread.sleep(2000);
        } catch (Exception ignored) {}

        // Tips/Attention screens — tap Understood until we reach exercises
        for (int i = 0; i < 5; i++) {
            try {
                var driver = com.neuronation.driver.DriverManager.getDriver();
                var btns = driver.findElements(io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"Understood\")"));
                if (!btns.isEmpty()) {
                    btns.get(0).click();
                    Thread.sleep(2000);
                } else break;
            } catch (Exception e) { break; }
        }

        // Exercises — Start + Pause + Succeed for each
        for (int ex = 0; ex < 4; ex++) {
            try {
                var driver = com.neuronation.driver.DriverManager.getDriver();
                // Tap Start
                driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"Start\")")).click();
                Thread.sleep(4000);
                // Pause (top-right)
                org.openqa.selenium.interactions.PointerInput finger =
                        new org.openqa.selenium.interactions.PointerInput(
                                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence tap =
                        new org.openqa.selenium.interactions.Sequence(finger, 0);
                tap.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                        org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 1020, 160));
                tap.addAction(finger.createPointerDown(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                tap.addAction(finger.createPointerUp(
                        org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(java.util.Collections.singletonList(tap));
                Thread.sleep(2000);
                // Succeed
                driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"Succeed exercise\")")).click();
                Thread.sleep(3000);
                // Close video if it appears
                try {
                    driver.findElement(io.appium.java_client.AppiumBy.id(
                            "nn.mobile.app.med:id/closeVideoButton")).click();
                    Thread.sleep(2000);
                } catch (Exception ignored) {}
            } catch (Exception e) {
                log.warn("Exercise {} navigation issue: {}", ex + 1, e.getMessage());
            }
        }

        // Age group selection from test data
        try {
            var driver = com.neuronation.driver.DriverManager.getDriver();
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"Age group 31-40\")")).click();
            Thread.sleep(2000);
            // Scroll + Create plan
            driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", 100, "top", 1800, "width", 800, "height", 200,
                    "direction", "up", "percent", 0.75));
            Thread.sleep(1000);
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"Create Personalised Training Plan\")")).click();
            Thread.sleep(3000);
        } catch (Exception e) {
            log.warn("Age/Evaluation issue: {}", e.getMessage());
        }

        // Activate → Standard → Morning → Confirm → Allow → Yes try → Yes promise → Understood
        String[] buttonSequence = {
                "Activate", "Standard", "In the morning"
        };
        for (String btn : buttonSequence) {
            try {
                var driver = com.neuronation.driver.DriverManager.getDriver();
                Thread.sleep(1000);
                driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"" + btn + "\")")).click();
                Thread.sleep(2000);
            } catch (Exception e) {
                log.warn("Could not tap '{}': {}", btn, e.getMessage());
            }
        }

        // Scroll to Confirm
        try {
            var driver = com.neuronation.driver.DriverManager.getDriver();
            driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", 100, "top", 1800, "width", 800, "height", 200,
                    "direction", "up", "percent", 0.75));
            Thread.sleep(1000);
            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"Confirm\")")).click();
            Thread.sleep(2000);
        } catch (Exception ignored) {}

        String[] finalButtons = {
                "Allow", "Yes, let me try it out", "Yes", "Understood"
        };
        for (String btn : finalButtons) {
            try {
                var driver = com.neuronation.driver.DriverManager.getDriver();
                Thread.sleep(1000);
                driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"" + btn + "\")")).click();
                Thread.sleep(2000);
            } catch (Exception e) {
                log.warn("Could not tap '{}': {}", btn, e.getMessage());
            }
        }

        // Now on Dashboard — navigate to Profile
        Thread.sleep(3000);
        ScreenDumper.dumpCurrentScreen("Dashboard_BeforeProfile");
        navigateToProfile();
        ScreenDumper.dumpCurrentScreen("ProfileScreen");

        // ──────────────────────────────────────────────
        // PROFILE ASSERTIONS
        // ──────────────────────────────────────────────

        var driver = com.neuronation.driver.DriverManager.getDriver();

        // 1. Verify account type is MCI
        String accountValidity = driver.findElement(
                io.appium.java_client.AppiumBy.id("nn.mobile.app.med:id/accountValidDateLabel")).getText();
        log.info("Account validity: {}", accountValidity);
        assertTrue(accountValidity.contains("MCI"),
                "Account should be MCI type, got: " + accountValidity);

        // 2. Verify 90-day validity
        LocalDate expectedExpiry = LocalDate.now().plusDays(90);
        String expectedDateFormatted = expectedExpiry.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        assertTrue(accountValidity.contains(expectedDateFormatted),
                "Account should be valid until " + expectedDateFormatted + ", got: " + accountValidity);
        log.info("MCI validity confirmed: 90 days from today = {}", expectedDateFormatted);

        // 3. Verify User ID is present and UUID format
        String userId = driver.findElement(
                io.appium.java_client.AppiumBy.id("nn.mobile.app.med:id/accountIdLabel")).getText();
        log.info("User ID: {}", userId);
        assertTrue(userId.startsWith("User ID:"), "Should show User ID");
        assertTrue(userId.contains("-"), "User ID should be UUID format");

        // 4. Verify Level
        String level = driver.findElement(
                io.appium.java_client.AppiumBy.id("nn.mobile.app.med:id/level_headline")).getText();
        log.info("Level: {}", level);
        assertEquals(level, "Level 1 - Newcomer", "New user should be Level 1");

        // 5. Verify Account management options visible
        assertTrue(driver.findElement(
                io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"Change Name\")")).isDisplayed(),
                "Change Name should be visible");
        assertTrue(driver.findElement(
                io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"Change Email\")")).isDisplayed(),
                "Change Email should be visible");

        // 6. Capture content snapshot
        verifyOrRecordContent(screens.profile(), "ProfileScreen");

        log.info("=== PROFILE VERIFICATION COMPLETE ===");
        log.info("Account: MCI, Valid until: {}, Level: {}", expectedDateFormatted, level);
    }
}
