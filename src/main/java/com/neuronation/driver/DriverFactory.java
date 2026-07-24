package com.neuronation.driver;

import com.neuronation.config.ConfigManager;
import com.neuronation.config.Platform;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DriverFactory {
    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);

    public static AppiumDriver createDriver() {
        ConfigManager config = ConfigManager.getInstance();
        Platform platform = config.getPlatform();
        boolean isBrowserStack = config.getBoolean("browserstack.enabled");

        log.info("Creating {} driver (BrowserStack: {})", platform, isBrowserStack);

        if (platform == Platform.ANDROID) {
            return createAndroidDriver(config, isBrowserStack);
        } else {
            return createIOSDriver(config, isBrowserStack);
        }
    }

    private static AppiumDriver createAndroidDriver(ConfigManager config, boolean isBrowserStack) {
        UiAutomator2Options options = new UiAutomator2Options();

        if (isBrowserStack) {
            configureBrowserStack(options, config);
            options.setApp(config.getString("browserstack.app.url"));
        } else {
            options.setDeviceName(config.getString("device.name"));
            String udid = config.getString("device.udid");
            if (udid != null && !udid.isEmpty()) {
                options.setUdid(udid);
            }
            String appPath = config.getString("app.path");
            if (appPath != null) {
                options.setApp(appPath);
            } else {
                options.setAppPackage(config.getString("app.package"));
                options.setAppActivity(config.getString("app.activity"));
            }
            options.setAutomationName(config.getString("automation.name"));
            // App is pre-installed — never uninstall/reinstall
            options.setCapability("appium:noReset", true);
            // Default true (relaunch app fresh). Set -Dforce.app.launch=false to ATTACH to the
            // app's current state without relaunching — e.g. a logged-in-only settings check.
            boolean forceLaunch = !"false".equals(config.getString("force.app.launch"));
            options.setCapability("appium:forceAppLaunch", forceLaunch);
            // Unique systemPort per device for parallel execution on single Appium server
            String systemPort = config.getString("system.port");
            if (systemPort != null && !systemPort.isEmpty()) {
                options.setCapability("appium:systemPort", Integer.parseInt(systemPort));
            }
        }

        // 5 minutes — must survive IMAP email polling (60-90s) without session timeout
        options.setNewCommandTimeout(Duration.ofSeconds(300));

        // NOTE: do NOT set waitForIdleTimeout=0 globally — it makes the dashboard/onboarding waits
        // flaky (main_toolbar_title never resolves during launch). It is toggled to 0 ONLY around the
        // NeuroBooster video, where the never-idle playing video otherwise blocks every command ~10s
        // and the controller auto-hides first — see NeuroBoosterVideoScreen.fastForwardToEnd().

        try {
            URL url = new URL(isBrowserStack
                    ? "https://hub-cloud.browserstack.com/wd/hub"
                    : config.getString("appium.url"));
            return new AndroidDriver(url, options);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium URL", e);
        }
    }

    private static AppiumDriver createIOSDriver(ConfigManager config, boolean isBrowserStack) {
        XCUITestOptions options = new XCUITestOptions();

        if (isBrowserStack) {
            configureBrowserStack(options, config);
            options.setApp(config.getString("browserstack.app.url"));
        } else {
            options.setDeviceName(config.getString("device.name"));
            String platformVersion = config.getString("platform.version");
            if (platformVersion != null && !platformVersion.isEmpty()) {
                options.setPlatformVersion(platformVersion);
            }
            String udid = config.getString("device.udid");
            if (udid != null && !udid.isEmpty()) {
                options.setUdid(udid);
            }
            String appPath = config.getString("app.path");
            if (appPath != null) {
                options.setApp(appPath);
            } else {
                options.setBundleId(config.getString("bundle.id"));
            }
            options.setAutomationName(config.getString("automation.name"));
            options.setCapability("appium:noReset", true);
            // Default true (relaunch app fresh). Set -Dforce.app.launch=false to ATTACH to the
            // app's current state without relaunching — e.g. a logged-in-only settings check.
            boolean forceLaunch = !"false".equals(config.getString("force.app.launch"));
            options.setCapability("appium:forceAppLaunch", forceLaunch);
            // WDA signing for real devices
            String xcodeOrgId = config.getString("xcodeOrgId");
            if (xcodeOrgId != null && !xcodeOrgId.isEmpty()) {
                options.setCapability("appium:xcodeOrgId", xcodeOrgId);
                options.setCapability("appium:xcodeSigningId", config.getString("xcodeSigningId"));
            }
            String wdaBundleId = config.getString("updatedWDABundleId");
            if (wdaBundleId != null && !wdaBundleId.isEmpty()) {
                options.setCapability("appium:updatedWDABundleId", wdaBundleId);
            }
            // Auto-fix expired/missing provisioning profiles
            options.setCapability("appium:allowProvisioningDeviceRegistration", true);
            options.setCapability("appium:showXcodeLog", true);
            // Use external WDA (started via xcodebuild test + iproxy 8100)
            String wdaUrl = config.getString("webDriverAgentUrl");
            if (wdaUrl != null && !wdaUrl.isEmpty()) {
                options.setCapability("appium:webDriverAgentUrl", wdaUrl);
            }
            options.setCapability("appium:wdaLaunchTimeout", 120000);
            options.setCapability("appium:wdaConnectionTimeout", 120000);
            // Use pre-installed WDA to avoid rebuild + trust issues on real devices
            String usePreinstalled = config.getString("usePreinstalledWDA");
            if ("true".equals(usePreinstalled)) {
                options.setCapability("appium:usePreinstalledWDA", true);
                options.setCapability("appium:updatedWDABundleIdSuffix", ".xctrunner");
            }
        }

        // 5 minutes — must survive IMAP email polling (60-90s) without session timeout
        options.setNewCommandTimeout(Duration.ofSeconds(300));

        // NOTE on iOS speed: setting waitForIdleTimeout=0 (skip XCUITest's per-command quiescence
        // wait) sped up scrolling but BROKE the video→quiz→result flow (commands fired mid-animation
        // → empty result score, stuck on the result screen). Reverted. The real iOS speedup comes from
        // the monotonic scan (no scroll-to-top oscillation on deep sections). If more speed is needed,
        // tune waitForIdleTimeout to a MODEST value (e.g. 1-2s) — never 0 — and re-validate the quiz.

        try {
            URL url = new URL(isBrowserStack
                    ? "https://hub-cloud.browserstack.com/wd/hub"
                    : config.getString("appium.url"));
            return new IOSDriver(url, options);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium URL", e);
        }
    }

    private static void configureBrowserStack(
            io.appium.java_client.remote.options.BaseOptions<?> options,
            ConfigManager config) {
        Map<String, Object> bsOptions = new HashMap<>();
        bsOptions.put("userName", config.getString("browserstack.username"));
        bsOptions.put("accessKey", config.getString("browserstack.access.key"));
        bsOptions.put("projectName", config.getString("browserstack.project"));
        bsOptions.put("buildName", config.getString("browserstack.build.name"));
        // Network logs use a MITM proxy that intercepts HTTPS — this breaks SSL-pinned
        // apps (the app rejects the injected cert → "Integrity Check Failed"). Default ON,
        // but set browserstack.network.logs=false (e.g. for iOS) to disable interception.
        boolean networkLogs = !"false".equals(config.getString("browserstack.network.logs"));
        bsOptions.put("networkLogs", networkLogs);
        bsOptions.put("deviceLogs", true);
        // App Automate requires a target device + OS version, else BROWSERSTACK_MISSING_CAPS.
        String deviceName = config.getString("browserstack.device.name");
        String osVersion = config.getString("browserstack.os.version");
        if (deviceName != null && !deviceName.isEmpty()) {
            bsOptions.put("deviceName", deviceName);
        }
        if (osVersion != null && !osVersion.isEmpty()) {
            bsOptions.put("osVersion", osVersion);
        }
        options.setCapability("bstack:options", bsOptions);
    }
}
