package com.neuronation.base;

import com.neuronation.config.ConfigManager;
import com.neuronation.config.Platform;
import com.neuronation.content.ContentSnapshot;
import com.neuronation.content.ContentVerifier;
import com.neuronation.content.SnapshotManager;
import com.neuronation.context.TestContext;
import com.neuronation.driver.DriverFactory;
import com.neuronation.driver.DriverManager;
import com.neuronation.listeners.AllureScreenshotListener;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.asserts.SoftAssert;

import java.util.Map;

public class BaseTest {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** Playwright-style fixture — lazy-initialized screen page objects. */
    protected Screens screens;

    /** Per-test runtime state (auth token, userId, language, etc.) */
    protected TestContext context;

    /** Soft assertions for content mismatches (don't block functional flow). */
    protected SoftAssert softAssert;

    /** Centralized navigation helper for MED app flow. */
    protected MedFlowHelper medFlow;

    // Internal
    private SnapshotManager snapshotManager;
    private ContentVerifier contentVerifier;

    @BeforeMethod(alwaysRun = true)
    @Parameters({"language", "config.profile"})
    public void setUp(@Optional("en") String language, @Optional("") String configProfile) {
        // Thread-local config — supports parallel execution across devices
        if (configProfile != null && !configProfile.isEmpty()) {
            ConfigManager.initForThread(configProfile);
        }

        // Clear app data if configured — ensures clean state for regression tests
        if ("true".equals(ConfigManager.getInstance().getString("reset.app.data"))) {
            resetAppData();
        }

        // Driver creates session — app launches with noReset=true (pre-installed)
        AppiumDriver driver = DriverFactory.createDriver();
        DriverManager.setDriver(driver);

        // Context
        context = new TestContext();
        context.setLanguage(language);
        context.setAppType(ConfigManager.getInstance().getAppType().name().toLowerCase());
        context.setPlatform(ConfigManager.getInstance().getPlatform().name().toLowerCase());
        TestContext.set(context);

        // Fixtures
        screens = new Screens();
        softAssert = new SoftAssert();
        medFlow = new MedFlowHelper(screens, language);
        snapshotManager = new SnapshotManager("src/test/resources/snapshots");
        contentVerifier = new ContentVerifier();

        log.info("Test setup complete — app: {}, platform: {}, language: {}",
                context.getAppType(), context.getPlatform(), language);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        // Capture screenshot BEFORE quitDriver — listener.onTest* fires too late
        // (TestNG runs @AfterMethod before ITestListener callbacks, so the driver
        // is already null by then and the screenshot is silently lost).
        if (DriverManager.getDriver() != null) {
            String methodName = result.getMethod().getMethodName();
            String label = switch (result.getStatus()) {
                case ITestResult.SUCCESS -> "Final State - " + methodName;
                case ITestResult.FAILURE -> "Failure Screenshot - " + methodName;
                case ITestResult.SKIP    -> "Skipped Screenshot - " + methodName;
                default -> "Screenshot - " + methodName;
            };
            AllureScreenshotListener.captureScreenshot(label);
        }

        if (screens != null) screens.reset();
        DriverManager.quitDriver();
        TestContext.remove();
        ConfigManager.removeThread();
        log.info("Test teardown complete");
    }

    /**
     * Clear app data before driver creation via direct adb.
     * No relaunch needed — Appium session will launch the app.
     */
    private void resetAppData() {
        String appPackage = ConfigManager.getInstance().getString("app.package");
        String udid = ConfigManager.getInstance().getString("device.udid");
        if (appPackage == null) return;
        try {
            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add("adb");
            if (udid != null && !udid.isEmpty()) { cmd.add("-s"); cmd.add(udid); }
            cmd.add("shell"); cmd.add("pm"); cmd.add("clear"); cmd.add(appPackage);
            new ProcessBuilder(cmd).redirectErrorStream(true).start().waitFor();
            log.info("App data cleared for {} on {}", appPackage, udid != null ? udid : "default");
        } catch (Exception e) {
            log.warn("Could not clear app data: {}", e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // Content snapshot helpers
    // ──────────────────────────────────────────────

    /**
     * Captures screen content and either records a new baseline or verifies against existing one.
     * Uses soft assertions so content mismatches don't block the functional flow.
     */
    @Step("Verify/record content snapshot for {screenName}")
    protected void verifyOrRecordContent(BaseScreen screen, String screenName) {
        Map<String, String> content = screen.captureContent();
        if (content.isEmpty()) return;

        String mode = ConfigManager.getInstance().getString("snapshot.mode");
        String variant = String.valueOf(context.getVariantId());

        if ("record".equals(mode)) {
            snapshotManager.record(screenName, context.getAppType(), context.getPlatform(),
                    context.getLanguage(), variant, content);
        } else {
            ContentSnapshot baseline = snapshotManager.load(screenName, context.getAppType(),
                    context.getPlatform(), context.getLanguage(), variant);
            if (baseline != null) {
                ContentSnapshot current = new ContentSnapshot(screenName, content);
                var mismatches = contentVerifier.verify(current, baseline);
                for (String mismatch : mismatches) {
                    softAssert.fail("Content mismatch on " + screenName + ": " + mismatch);
                }
            }
        }
    }
}
