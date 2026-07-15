package com.neuronation.listeners;

import com.neuronation.driver.DriverManager;
import com.neuronation.knownissues.KnownIssueTracker;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Logs test lifecycle and exposes a static screenshot helper for BaseTest.
 *
 * Screenshot capture is invoked from BaseTest.tearDown BEFORE the driver
 * is quit — otherwise the driver is null by the time onTestFailure fires
 * and the screenshot is silently lost.
 */
public class AllureScreenshotListener implements ITestListener, ISuiteListener {
    private static final Logger log = LoggerFactory.getLogger(AllureScreenshotListener.class);

    /** Deterministically flush the known-issue run summary at suite end (the JVM-exit hook is a
     *  backstop). Writing here guarantees target/known-issues-report.json exists before Jenkins
     *  stashes it for the Allure Environment widget / Slack line. */
    @Override
    public void onFinish(ISuite suite) {
        KnownIssueTracker.writeReport();
    }

    @Override
    public void onTestStart(ITestResult result) {
        log.info("Starting test: {}", result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("Test PASSED: {}", result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("Test FAILED: {} - {}", result.getMethod().getMethodName(),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("Test SKIPPED: {}", result.getMethod().getMethodName());
    }

    public static void captureScreenshot(String name) {
        AppiumDriver driver = DriverManager.getDriver();
        if (driver == null) {
            log.warn("Cannot capture screenshot — driver is null");
            return;
        }
        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            // Try Allure lifecycle first (works if test context is still active)
            try {
                Allure.addAttachment(name, "image/png",
                        new ByteArrayInputStream(screenshot), "png");
                log.info("Screenshot attached via Allure lifecycle: {}", name);
                return;
            } catch (Exception ignored) {
                // Lifecycle already closed — fall through to direct file write
            }

            // Fallback: write directly to allure-results directory
            String resultsDir = "target/allure-results";
            Path dir = Paths.get(resultsDir);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            String attachId = UUID.randomUUID().toString();
            Path screenshotPath = dir.resolve(attachId + "-attachment.png");
            Files.write(screenshotPath, screenshot);
            log.info("Screenshot saved to allure-results: {}", screenshotPath);

        } catch (Exception e) {
            log.error("Failed to capture screenshot: {}", e.getMessage());
        }
    }
}
