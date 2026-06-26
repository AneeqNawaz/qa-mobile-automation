package com.neuronation.utils;

import com.neuronation.driver.DriverManager;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized bug reporting utility.
 * Captures detailed evidence when an app bug is discovered during test execution.
 * All evidence is attached to the Allure report for clear bug documentation.
 *
 * Usage:
 *   BugReporter.reportBug("Login failed after password submit",
 *       "Screen: Set Password",
 *       "Action: Tapped 'Create Account' with valid password",
 *       "Expected: Navigate to email verification screen",
 *       "Actual: Error dialog 'Login failed. Please try again.'",
 *       "Password used: TestPass@123",
 *       "Email: qa.mci+123@nntest.de");
 */
public class BugReporter {
    private static final Logger log = LoggerFactory.getLogger(BugReporter.class);

    /**
     * Report a bug found during test execution.
     * Captures screenshot + device info + all provided details as Allure attachments.
     * Then throws AssertionError so the test fails with clear bug evidence.
     *
     * @param title     Short bug title
     * @param details   One or more detail lines (screen, action, expected, actual, data used)
     */
    public static void reportBug(String title, String... details) {
        log.error("=== BUG FOUND: {} ===", title);

        StringBuilder report = new StringBuilder();
        report.append("BUG: ").append(title).append("\n");
        report.append("Timestamp: ").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("─────────────────────────────\n");

        for (String detail : details) {
            report.append(detail).append("\n");
            log.error("  {}", detail);
        }

        // Add device info
        try {
            AppiumDriver driver = DriverManager.getDriver();
            if (driver != null) {
                var caps = driver.getCapabilities();
                report.append("─────────────────────────────\n");
                report.append("Device: ").append(caps.getCapability("appium:deviceName")).append("\n");
                report.append("Platform: ").append(caps.getCapability("platformName"))
                        .append(" ").append(caps.getCapability("appium:platformVersion")).append("\n");
                Object appId = caps.getCapability("appium:appPackage");
                if (appId == null) appId = caps.getCapability("appium:bundleId");
                report.append("App: ").append(appId).append("\n");
            }
        } catch (Exception ignored) {}

        String reportText = report.toString();

        // Attach to Allure
        try {
            Allure.addAttachment("BUG REPORT: " + title, "text/plain", reportText);
        } catch (Exception ignored) {}

        // Capture screenshot
        captureScreenshot("BUG Screenshot: " + title);

        // Dump current screen elements for debugging
        try {
            String dump = ScreenDumper.dumpCurrentScreen("BUG_" + title.replaceAll("[^a-zA-Z0-9]", "_"));
            Allure.addAttachment("BUG Screen Elements: " + title, "text/plain", dump);
        } catch (Exception ignored) {}

        // Fail the test with bug evidence
        throw new AssertionError("APP BUG: " + title + "\n" + reportText);
    }

    /**
     * Check if an error dialog is displayed. If so, report it as a bug.
     * If no dialog, returns silently.
     *
     * @param context Where in the flow this check happens (e.g. "after password submit")
     * @param extraDetails Additional context details to include in the bug report
     */
    public static void checkForErrorDialog(String context, String... extraDetails) {
        try {
            var driver = DriverManager.getDriver();
            boolean isAndroid = com.neuronation.config.ConfigManager.getInstance()
                    .getPlatform() == com.neuronation.config.Platform.ANDROID;

            // Detect alert/dialog
            boolean hasAlert;
            if (isAndroid) {
                hasAlert = !driver.findElements(
                        io.appium.java_client.AppiumBy.id("android:id/message")).isEmpty();
            } else {
                hasAlert = !driver.findElements(io.appium.java_client.AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeAlert")).isEmpty();
            }

            if (!hasAlert) return; // No error — all good

            // Extract error message
            String errorMsg;
            String dialogTitle = "";
            if (isAndroid) {
                errorMsg = driver.findElement(
                        io.appium.java_client.AppiumBy.id("android:id/message")).getText();
                try {
                    dialogTitle = driver.findElement(
                            io.appium.java_client.AppiumBy.id("android:id/alertTitle")).getText();
                } catch (Exception ignored) {}
            } else {
                var texts = driver.findElements(io.appium.java_client.AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeAlert/**/XCUIElementTypeStaticText"));
                dialogTitle = texts.size() > 0 ? texts.get(0).getText() : "";
                errorMsg = texts.size() > 1 ? texts.get(1).getText() : dialogTitle;
            }

            // Benign info dialogs are NOT bugs — dismiss and continue. The "Adjusting the volume"
            // popup (shown before an explanatory video when the device volume is low/muted) is the
            // known case; it can appear at any video point, so handle it here at the source rather
            // than racing a per-wait dismissal.
            String benign = (dialogTitle + " " + errorMsg).toLowerCase();
            if (benign.contains("volume") || benign.contains("explanatory video")
                    || benign.contains("spoken language")) {
                try {
                    if (isAndroid) {
                        driver.findElement(io.appium.java_client.AppiumBy.id("android:id/button1")).click(); // "Yes, continue"
                    } else {
                        var btns = driver.findElements(io.appium.java_client.AppiumBy.iOSClassChain(
                                "**/XCUIElementTypeAlert/**/XCUIElementTypeButton"));
                        if (!btns.isEmpty()) btns.get(btns.size() - 1).click();
                    }
                } catch (Exception ignored) {}
                return; // not a bug — proceed with the regular test
            }

            // Build detail lines
            java.util.List<String> details = new java.util.ArrayList<>();
            details.add("Context: " + context);
            details.add("Dialog title: " + (dialogTitle.isEmpty() ? "(none)" : dialogTitle));
            details.add("Error message: " + errorMsg);
            for (String extra : extraDetails) {
                details.add(extra);
            }

            // Capture screenshot before dismissing
            captureScreenshot("BUG Dialog: " + context);

            // Dismiss the dialog
            if (isAndroid) {
                try {
                    driver.findElement(io.appium.java_client.AppiumBy.id("android:id/button2")).click();
                } catch (Exception e) {
                    try {
                        driver.findElement(io.appium.java_client.AppiumBy.id("android:id/button1")).click();
                    } catch (Exception e2) {
                        try {
                            driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                                    "new UiSelector().text(\"OK\")")).click();
                        } catch (Exception ignored) {}
                    }
                }
            } else {
                try {
                    var buttons = driver.findElements(io.appium.java_client.AppiumBy.iOSClassChain(
                            "**/XCUIElementTypeAlert/**/XCUIElementTypeButton"));
                    if (!buttons.isEmpty()) buttons.get(buttons.size() - 1).click();
                } catch (Exception ignored) {}
            }

            reportBug("Error dialog: " + errorMsg, details.toArray(new String[0]));

        } catch (AssertionError ae) {
            throw ae;
        } catch (Exception ignored) {
            // No dialog found — fine
        }
    }

    private static void captureScreenshot(String name) {
        try {
            AppiumDriver driver = DriverManager.getDriver();
            if (driver != null) {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Allure.addAttachment(name, "image/png",
                        new ByteArrayInputStream(screenshot), "png");
            }
        } catch (Exception e) {
            log.warn("Could not capture bug screenshot: {}", e.getMessage());
        }
    }
}
