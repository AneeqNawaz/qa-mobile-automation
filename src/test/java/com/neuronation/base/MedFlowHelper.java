package com.neuronation.base;

import com.neuronation.api.MockHealthInsuranceService;
import com.neuronation.config.AppType;
import com.neuronation.config.ConfigManager;
import com.neuronation.config.Platform;
import com.neuronation.driver.DriverManager;
import com.neuronation.testdata.*;
import com.neuronation.utils.TestDataLoader;
import io.appium.java_client.AppiumBy;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Config-driven navigation helper for MED app flow.
 * Uses explicit waits (waitForScreen) instead of Thread.sleep.
 * All choices come from FlowConfig loaded from testdata/med-en.json.
 * Cross-platform: supports both Android and iOS.
 */
public class MedFlowHelper {
    private static final Logger log = LoggerFactory.getLogger(MedFlowHelper.class);

    private final Screens screens;
    private final String language;

    private String currentEmail;
    private String currentPassword;
    private String currentCode;
    private FlowConfig flowConfig;

    /** Per-day reminder times captured from the onboarding Schedule Review screen. */
    private final Map<String, String> scheduleReviewTimes = new LinkedHashMap<>();

    public MedFlowHelper(Screens screens, String language) {
        this.screens = screens;
        this.language = language;
    }

    // ──────────────────────────────────────────────
    // Platform helpers
    // ──────────────────────────────────────────────

    private boolean isAndroid() {
        return ConfigManager.getInstance().getPlatform() == Platform.ANDROID;
    }

    private By platformLocator(String androidId, String iosAccessibilityId) {
        return isAndroid() ? AppiumBy.id(androidId) : AppiumBy.accessibilityId(iosAccessibilityId);
    }

    private boolean isAlertPresent() {
        var driver = DriverManager.getDriver();
        try {
            if (isAndroid()) {
                return !driver.findElements(AppiumBy.id("android:id/message")).isEmpty();
            } else {
                return !driver.findElements(AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeAlert")).isEmpty();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void dismissAlert() {
        var driver = DriverManager.getDriver();
        try {
            if (isAndroid()) {
                var btn = driver.findElements(AppiumBy.id("android:id/button1"));
                if (!btn.isEmpty()) { btn.get(0).click(); return; }
                btn = driver.findElements(AppiumBy.id("android:id/button2"));
                if (!btn.isEmpty()) btn.get(0).click();
            } else {
                var buttons = driver.findElements(AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeAlert/**/XCUIElementTypeButton"));
                if (!buttons.isEmpty()) buttons.get(buttons.size() - 1).click();
            }
        } catch (Exception ignored) {}
    }

    // ──────────────────────────────────────────────
    // Setup
    // ──────────────────────────────────────────────

    @Step("Load flow config: {flowName}")
    public void loadFlow(String flowName) {
        flowConfig = TestDataLoader.loadFlowConfig(AppType.MED, language, flowName, FlowConfig.class);
        log.info("Loaded flow: {} — {}", flowName, flowConfig.getDescription());
    }

    @Step("Generate fresh MCI activation code via API")
    public String generateFreshMciCode() {
        String override = System.getProperty("activation.code");
        if (override != null && !override.isEmpty()) {
            currentCode = override;
            log.info("Using -Dactivation.code override: {}", currentCode);
            return currentCode;
        }
        currentCode = new MockHealthInsuranceService().generateMciCode();
        log.info("Generated fresh MCI code: {}", currentCode);
        return currentCode;
    }

    @Step("Generate fresh Parkinson activation code via API")
    public String generateFreshParkinsonCode() {
        String override = System.getProperty("activation.code");
        if (override != null && !override.isEmpty()) {
            currentCode = override;
            log.info("Using -Dactivation.code override: {}", currentCode);
            return currentCode;
        }
        currentCode = new MockHealthInsuranceService().generateParkinsonCode();
        log.info("Generated fresh Parkinson code: {}", currentCode);
        return currentCode;
    }

    public String getActivationCode() {
        if (currentCode != null) return currentCode;
        currentCode = TestDataLoader.loadActivationData(
                AppType.MED, language, ActivationData.class).getDigaCode();
        return currentCode;
    }

    public String generateEmail() {
        RegistrationData regData = TestDataLoader.loadRegistrationData(
                AppType.MED, language, RegistrationData.class);
        currentEmail = regData.generateEmail();
        currentPassword = regData.getPassword();
        return currentEmail;
    }

    public String getCurrentEmail() { return currentEmail; }
    public String getCurrentPassword() { return currentPassword; }
    public String getCurrentCode() { return currentCode; }
    public FlowConfig getFlowConfig() { return flowConfig; }

    // ──────────────────────────────────────────────
    // Registration flow (screens 1-10)
    // ──────────────────────────────────────────────

    @Step("Navigate: Launch → App Selection → DiGA Code")
    public void goToDiGAScreen() {
        screens.launch().waitForScreen();
        screens.launch().tapStartNow();
        screens.appSelection().waitForScreen();
        screens.appSelection().selectMedicalApp();
        screens.digaCode().waitForScreen();
    }

    @Step("Navigate: ... → DiGA → Activate → Video → Close → Create Account")
    public void goToCreateAccount() {
        goToDiGAScreen();
        screens.digaCode().enterCodeAndActivate(getActivationCode());

        // Wait for EITHER video screen OR error dialog (code validation is async)
        // On iOS, also check if we've left the Activation screen (nav bar changed)
        var driver = DriverManager.getDriver();
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
            if (isAlertPresent()) {
                com.neuronation.utils.BugReporter.checkForErrorDialog(
                        "after DiGA code activation", "Code: " + currentCode);
                return true;
            }
            try {
                // Android: check for onboarding container
                if (isAndroid()) {
                    var el = d.findElements(AppiumBy.id("nn.mobile.app.med:id/onboardingContainer"));
                    if (!el.isEmpty() && el.get(0).isDisplayed()) return true;
                } else {
                    // iOS: check for video screen or nav bar no longer showing "Activation"
                    var video = d.findElements(AppiumBy.accessibilityId("onboardingContainer"));
                    if (!video.isEmpty() && video.get(0).isDisplayed()) return true;
                    // Fallback: check if "Activation" nav bar is gone (we left DiGA screen)
                    var activationNav = d.findElements(AppiumBy.accessibilityId("Activation"));
                    if (activationNav.isEmpty()) return true;
                }
            } catch (Exception ignored) {}
            return false;
        });

        dismissVolumePopupIfPresent();
        screens.onboardingVideo().tapClose();
        screens.createAccount().waitForScreen();
    }

    /** Optionally dismiss the "Adjusting the volume" popup shown before an explanatory video when
     *  the device volume is low/muted. No-op when it isn't shown (volume up). Best-effort —
     *  Android only (the id-based lookup simply finds nothing on iOS). */
    private void dismissVolumePopupIfPresent() {
        var driver = DriverManager.getDriver();
        try {
            for (var t : driver.findElements(AppiumBy.id("android:id/alertTitle"))) {
                String txt = t.getText();
                if (txt != null && txt.toLowerCase().contains("volume")) {
                    driver.findElement(AppiumBy.id("android:id/button1")).click(); // "Yes, continue"
                    log.info("Dismissed 'Adjusting the volume' popup before video");
                    return;
                }
            }
        } catch (Exception e) {
            log.debug("Volume popup check skipped: {}", e.getMessage());
        }
    }

    @Step("Navigate: ... → Create Account → Email Registration Form")
    public void goToEmailRegistration() {
        goToCreateAccount();
        screens.createAccount().tapRegisterViaEmail();
        // Dismiss email confirmation dialog (iOS: "Continue", Android: OK button)
        waitAndTapDialogButton("android:id/button1", "Continue");
        screens.emailRegistration().waitForScreen();
    }

    @Step("Navigate: ... → Fill email + consents → Submit → Passkey Dialog")
    public void goToPasskeyDialog() {
        goToEmailRegistration();
        generateEmail();

        screens.emailRegistration().enterEmail(currentEmail);
        screens.emailRegistration().acceptRequiredCheckboxes();

        if (flowConfig != null) {
            if (flowConfig.isDataRetainConsent()) screens.emailRegistration().checkDataRetain();
            if (flowConfig.isDataProcessingConsent()) screens.emailRegistration().checkDataProcessing();
        }

        screens.emailRegistration().scrollToSubmit();
        screens.emailRegistration().tapSubmit();

        waitForEitherScreenOrError(
                "nn.mobile.app.med:id/yesButton", "Use Passwordless Login?",
                "after email registration submit", "Email: " + currentEmail);
    }

    @Step("Navigate: ... → Passkey Later → Set Password")
    public void goToSetPassword() {
        goToPasskeyDialog();
        screens.passkeyDialog().tapLater();
        screens.setPassword().waitForScreen();
    }

    @Step("PASSWORD PATH: Full registration → email verification screen")
    public void completeRegistrationWithPassword() {
        goToSetPassword();
        submitPasswordAndAdvance();
    }

    @Step("Complete registration with configured auth method")
    public void completeRegistration() {
        String authMethod = flowConfig != null ? flowConfig.getAuthMethod() : "password";

        if ("passkey".equals(authMethod)) {
            goToPasskeyDialog();
            screens.passkeyDialog().tapYes();
            waitAndDismissOsPasswordManager();
            screens.emailVerification().waitForScreen();
        } else if ("nopassword".equals(authMethod)) {
            goToPasskeyDialog();
            screens.passkeyDialog().tapNoUsePassword();
            screens.setPassword().waitForScreen();
            submitPasswordAndAdvance();
        } else {
            goToPasskeyDialog();
            screens.passkeyDialog().tapLater();
            screens.setPassword().waitForScreen();
            submitPasswordAndAdvance();
        }
    }

    /**
     * Submit password and handle all possible outcomes:
     * 1. OS password manager dialog appears → dismiss
     * 2. "Login failed" error appears → report as bug
     * 3. Email verification loads directly → continue
     */
    private void submitPasswordAndAdvance() {
        screens.setPassword().setPasswordAndSubmit(currentPassword);

        var driver = DriverManager.getDriver();
        // Wait for either: password manager dismissed, error dialog, or email verification screen
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
            try {
                if (isAndroid()) {
                    if (checkAndDismissAndroidPasswordManager(d)) return true;
                } else {
                    if (checkAndDismissIosPasswordManager(d)) return true;
                }
                // Check if error dialog appeared
                if (isAlertPresent()) return true;
                // Check if email verification screen loaded
                if (isAndroid()) {
                    var title = d.findElements(AppiumBy.id("nn.mobile.app.med:id/main_toolbar_title"));
                    return !title.isEmpty() && "Protect your account".equals(title.get(0).getText());
                } else {
                    return !d.findElements(AppiumBy.accessibilityId("Protect your account")).isEmpty();
                }
            } catch (Exception ignored) {}
            return false;
        });

        // Check for error dialog after dismissing any OS overlay
        com.neuronation.utils.BugReporter.checkForErrorDialog(
                "after password submit", "Email: " + currentEmail, "Password: " + currentPassword);

        screens.emailVerification().waitForScreen();
    }

    private boolean checkAndDismissAndroidPasswordManager(org.openqa.selenium.WebDriver d) {
        try {
            var samsungPass = d.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"Samsung Pass\")"));
            var savePass = d.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"Save password\")"));
            var googlePM = d.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"Google Password Manager\")"));
            if (!samsungPass.isEmpty() || !savePass.isEmpty() || !googlePM.isEmpty()) {
                var cancel = d.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"Cancel\")"));
                var notNow = d.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"Not now\")"));
                if (!cancel.isEmpty()) cancel.get(0).click();
                else if (!notNow.isEmpty()) notNow.get(0).click();
                else d.navigate().back();
                log.info("Dismissed OS password manager after password submit");
                return true;
            }
            // Google PM on emulator renders outside app hierarchy — check if app screen is blocked
            var appTitle = d.findElements(AppiumBy.id("nn.mobile.app.med:id/main_toolbar_title"));
            if (appTitle.isEmpty()) {
                // Something is covering the app — press back to dismiss
                d.navigate().back();
                log.info("Dismissed unknown overlay via back");
                return true;
            }
        } catch (Exception e) {
            // UiAutomator query failed — overlay might be blocking, press back
            d.navigate().back();
            log.info("Dismissed overlay via back (fallback)");
            return true;
        }
        return false;
    }

    private boolean checkAndDismissIosPasswordManager(org.openqa.selenium.WebDriver d) {
        // iOS: "Save Password?" sheet — wait up to 5s for it to appear
        try {
            new WebDriverWait(d, Duration.ofSeconds(5))
                    .until(driver -> !driver.findElements(AppiumBy.accessibilityId("Not Now")).isEmpty());
            d.findElement(AppiumBy.accessibilityId("Not Now")).click();
            log.info("Dismissed iOS 'Save Password?' prompt via 'Not Now'");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ──────────────────────────────────────────────
    // Email verification
    // ──────────────────────────────────────────────

    @Step("Complete email verification via IMAP and dismiss dialogs")
    public void completeEmailVerification() {
        if (!isAndroid()) {
            // iOS: dismiss "Save Password?" popup if still showing from password submit
            checkAndDismissIosPasswordManager(DriverManager.getDriver());
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }

        verifyCurrentUserEmail();

        // Wait for "Your device is connected" dialog and dismiss it
        if (isAndroid()) {
            waitAndTapDialogButton("android:id/button1", "OK");
        } else {
            // iOS: "OK" button is an in-app button, not a system alert
            var driver = DriverManager.getDriver();
            try {
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(
                                AppiumBy.accessibilityId("OK"))).click();
                log.info("Tapped OK on 'device connected' dialog");
            } catch (Exception e) {
                log.info("No 'device connected' dialog found — continuing");
            }
        }
        screens.doctorInfo().waitForScreen();
    }

    @Step("Verify current user's email via IMAP")
    public String verifyCurrentUserEmail() {
        var emailService = new com.neuronation.api.EmailVerificationService();
        String url = emailService.verifyEmail(currentEmail, 90);
        log.info("Email verified via IMAP: {}", url);
        return url;
    }

    // ──────────────────────────────────────────────
    // Onboarding flow (screens 11-30) — config-driven
    // ──────────────────────────────────────────────

    @Step("Complete doctor info screen based on config")
    public void completeDoctorInfo() {
        String mode = flowConfig != null ? flowConfig.getDoctorInfo() : "skip";
        if ("fill".equals(mode)) {
            DoctorData doc = TestDataLoader.loadDoctorData(AppType.MED, language, DoctorData.class);
            screens.doctorInfo().fillAndContinue(
                    doc.getPostalCode(), doc.getCity(), doc.getFirstName(), doc.getLastName());
        } else {
            screens.doctorInfo().skipAndContinue();
        }
        screens.newsletterConsent().waitForScreen();
    }

    @Step("Complete newsletter consent based on config")
    public void completeNewsletter() {
        boolean agree = flowConfig == null || flowConfig.isNewsletterConsent();
        if (agree) {
            screens.newsletterConsent().tapAgree();
        } else {
            screens.newsletterConsent().tapDisagree();
        }
        screens.tips().waitForScreen();
    }

    @Step("Dismiss all tips/attention screens")
    public void completeTipsScreens() {
        // Tips appear in sequence; the LAST iteration has no more tips and would
        // burn the 30s default implicit wait. Use a short poll instead — exits fast
        // when no Understood button appears within 2s.
        var d = DriverManager.getDriver();
        // Cross-platform locator — Android uses resource id, iOS uses accessibility id.
        var btnLocator = isAndroid()
                ? AppiumBy.id("nn.mobile.app.med:id/cta_button_inner")
                : AppiumBy.accessibilityId("Understood");
        for (int i = 0; i < 4; i++) {
            long deadline = System.currentTimeMillis() + 2_000;
            boolean found = false;
            while (System.currentTimeMillis() < deadline) {
                var btns = d.findElements(btnLocator);
                if (!btns.isEmpty()) {
                    try {
                        if (btns.get(0).isDisplayed()) { found = true; break; }
                    } catch (Exception ignored) {}
                }
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }
            if (!found) {
                log.info("No more tips screens after {}", i);
                return;
            }
            screens.tips().tapUnderstood();
        }
    }

    @Step("Complete 4 exercises: start → pause → succeed each")
    public void completeExercises() {
        for (int ex = 1; ex <= 4; ex++) {
            log.info("Starting exercise {}", ex);
            screens.exerciseIntro().waitForScreen();
            screens.exerciseIntro().tapStart();

            // Pause + succeed (handles retry internally)
            screens.exerciseInGame().pauseAndSucceed();

            // Story video only appears AFTER exercise 4. Skip the wait for 1-3.
            if (ex == 4) dismissInterExerciseVideo();

            log.info("Exercise {} completed", ex);
        }
    }

    /** Dismiss the story video that appears after the LAST exercise.
     *  Android: tap close. iOS: close is disabled, so fast-forward instead. */
    @Step("Dismiss post-exercise story video")
    public void dismissInterExerciseVideo() {
        try {
            new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            platformLocator("nn.mobile.app.med:id/onboardingContainer", "Video")));
            dismissVolumePopupIfPresent();
            if (isAndroid()) {
                screens.onboardingVideo().tapClose();
            } else {
                screens.onboardingVideo().skipVideoViaForward();
            }
            log.info("Dismissed post-exercise story video");
        } catch (Exception ignored) {
            // No video — continue
        }
    }

    @Step("Select age group from config")
    public void completeAgeSelection() {
        screens.ageGroup().waitForScreen();
        ProfileData profile = TestDataLoader.loadProfileData(AppType.MED, language, ProfileData.class);
        screens.ageGroup().selectAgeGroup(profile.getAgeGroup());
    }

    @Step("Tap Create Personalised Training Plan")
    public void completeEvaluation() {
        screens.evaluation().waitForScreen();
        // Scroll into view if needed — button may be below performance chart
        if (isAndroid()) {
            DriverManager.getDriver().findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))" +
                    ".scrollIntoView(new UiSelector().resourceId(\"nn.mobile.app.med:id/continueAssessmentButton\"))"));
        } else {
            // iOS: swipe up until button is visible
            for (int i = 0; i < 3; i++) {
                var btn = DriverManager.getDriver().findElements(
                        AppiumBy.accessibilityId("continueAssessmentButton"));
                if (!btn.isEmpty() && btn.get(0).isDisplayed()) break;
                swipeUp();
            }
        }
        screens.evaluation().tapCreatePlan();
        screens.trainingComplexity().waitForScreen();
    }

    @Step("Complete training complexity based on config")
    public void completeTrainingComplexity() {
        String mode = flowConfig != null ? flowConfig.getTrainingComplexity() : "activate";
        if ("deactivate".equals(mode)) {
            screens.trainingComplexity().tapDeactivate();
        } else {
            screens.trainingComplexity().tapActivate();
        }
        screens.specialNeeds().waitForScreen();
    }

    @Step("Select special needs based on config")
    public void completeSpecialNeeds() {
        String needs = flowConfig != null ? flowConfig.getSpecialNeeds() : "standard";
        switch (needs) {
            case "colorVision": screens.specialNeeds().tapColorVision(); break;
            case "arithmetic": screens.specialNeeds().tapArithmetic(); break;
            case "both": screens.specialNeeds().tapBoth(); break;
            default: screens.specialNeeds().tapStandard(); break;
        }
        screens.trainingTime().waitForScreen();
    }

    @Step("Select training time based on config")
    public void completeTrainingTime() {
        String time = flowConfig != null ? flowConfig.getTrainingTime() : "morning";
        switch (time) {
            case "noon": screens.trainingTime().tapNoon(); break;
            case "evening": screens.trainingTime().tapEvening(); break;
            case "night": screens.trainingTime().tapNight(); break;
            default: screens.trainingTime().tapMorning(); break;
        }
        screens.scheduleReview().waitForScreen();
    }

    @Step("Complete schedule review — scroll and confirm")
    public void completeScheduleReview() {
        // Capture the per-day times shown here during onboarding so a test can verify them
        // (they should all equal the chosen slot time). Best-effort — never break the flow.
        try {
            scheduleReviewTimes.clear();
            scheduleReviewTimes.putAll(screens.scheduleReview().getScheduleTimes());
            log.info("Captured Schedule Review per-day times: {}", scheduleReviewTimes);
        } catch (Exception e) {
            log.warn("Could not capture Schedule Review per-day times: {}", e.getMessage());
        }
        screens.scheduleReview().tapConfirm();
        // Notification popup follows
        waitForNotificationPopup();
    }

    /** Per-day reminder times captured from the onboarding Schedule Review screen (day→"HH:MM"). */
    public Map<String, String> getScheduleReviewTimes() {
        return scheduleReviewTimes;
    }

    private void waitForNotificationPopup() {
        var driver = DriverManager.getDriver();
        try {
            if (isAndroid()) {
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.presenceOfElementLocated(
                                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"notifications\")")));
            } else {
                // iOS shows a native alert for notification permission
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(d -> !d.findElements(AppiumBy.iOSClassChain(
                                "**/XCUIElementTypeAlert")).isEmpty());
            }
        } catch (Exception e) {
            log.info("Notification popup not detected — continuing");
        }
    }

    @Step("Handle notification permission based on config")
    public void completeNotificationPermission() {
        String perm = flowConfig != null ? flowConfig.getNotificationPermission() : "allow";
        try {
            if (isAndroid()) {
                handleAndroidNotificationPermission(perm);
            } else {
                handleIosNotificationPermission(perm);
            }
        } catch (Exception e) {
            log.info("No notification popup found");
        }
        screens.neuroBooster().waitForScreen();
    }

    private void handleAndroidNotificationPermission(String perm) {
        var driver = DriverManager.getDriver();
        try {
            if ("allow".equals(perm)) {
                driver.findElement(
                        AppiumBy.id("com.android.permissioncontroller:id/permission_allow_button")).click();
            } else {
                driver.findElement(
                        AppiumBy.id("com.android.permissioncontroller:id/permission_deny_button")).click();
            }
        } catch (Exception e) {
            try {
                String btnText = "allow".equals(perm) ? "Allow" : "Don't allow";
                driver.findElement(
                        AppiumBy.androidUIAutomator("new UiSelector().text(\"" + btnText + "\")")).click();
            } catch (Exception ignored) {}
        }
    }

    private void handleIosNotificationPermission(String perm) {
        var driver = DriverManager.getDriver();
        try {
            if ("allow".equals(perm)) {
                // Use Appium's native alert accept for iOS system alerts
                driver.switchTo().alert().accept();
                log.info("Accepted iOS notification permission via alert API");
            } else {
                driver.switchTo().alert().dismiss();
                log.info("Dismissed iOS notification permission via alert API");
            }
        } catch (Exception e) {
            // Fallback: try button tap
            try {
                String buttonLabel = "allow".equals(perm) ? "Allow" : "Don\u2019t Allow";
                var buttons = driver.findElements(AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeAlert/**/XCUIElementTypeButton[`label == '" + buttonLabel + "'`]"));
                if (!buttons.isEmpty()) buttons.get(0).click();
            } catch (Exception ignored) {}
        }
    }

    @Step("Complete NeuroBooster based on config")
    public void completeNeuroBooster() {
        boolean yes = flowConfig == null || flowConfig.isNeuroBooster();
        if (yes) {
            screens.neuroBooster().tapYes();
            screens.promise().waitForScreen();
        } else {
            screens.neuroBooster().tapNo();
        }
    }

    @Step("Complete promise if present")
    public void completePromise() {
        try {
            var driver = DriverManager.getDriver();
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            platformLocator("nn.mobile.app.med:id/alertTitle", "Promise?")));
            boolean yes = flowConfig == null || flowConfig.isPromise();
            if (yes) {
                screens.promise().tapYes();
            } else {
                screens.promise().tapNo();
            }
        } catch (Exception e) {
            log.info("Promise screen not shown — continuing to tips");
        }
        screens.tips().waitForScreen();
    }

    @Step("Dismiss final tips screen")
    public void completeFinalTips() {
        screens.tips().tapUnderstood();
        screens.dashboard().waitForScreen();
    }

    // ──────────────────────────────────────────────
    // Full E2E: registration + onboarding → dashboard
    // ──────────────────────────────────────────────

    @Step("Complete FULL E2E flow to dashboard using config: {flowName}")
    public void completeFullFlow(String flowName) {
        loadFlow(flowName);
        generateFreshMciCode();

        completeRegistration();
        completeEmailVerification();
        completeDoctorInfo();
        completeNewsletter();
        completeTipsScreens();
        completeExercises();
        completeAgeSelection();
        completeEvaluation();
        completeTrainingComplexity();
        completeSpecialNeeds();
        completeTrainingTime();
        completeScheduleReview();
        completeNotificationPermission();
        completeNeuroBooster();
        completePromise();
        completeFinalTips();

        log.info("=== DASHBOARD REACHED via flow: {} ===", flowName);
    }

    // ──────────────────────────────────────────────
    // Logout + Login (returning user)
    // ──────────────────────────────────────────────

    @Step("Logout from Profile screen")
    public void logout() {
        screens.dashboard().tapProfileTab();
        screens.profile().waitForScreen();
        screens.profile().tapLogout();
        screens.launch().waitForScreen();
        log.info("Logged out — Launch screen visible");
    }

    /**
     * Common end-of-flow logout. Assumes the Profile screen is already visible
     * (caller navigates there first), taps Log out, and VERIFIES the app returns
     * to the Launch screen. Call at the end of each flow so the next test starts fresh.
     * Throws if logout does not land on the Launch screen.
     */
    @Step("Log out from Profile and verify the app returns to the Launch screen")
    public void logoutAndVerify() {
        screens.profile().waitForScreen();   // ensure we are on Profile
        screens.profile().tapLogout();
        screens.launch().waitForScreen();
        if (!screens.launch().isStartNowDisplayed()) {
            throw new AssertionError("Logout did not land on the Launch screen (Start Now not visible)");
        }
        log.info("Logout verified — back on Launch screen");
    }

    /**
     * Login with email and password via the returning user flow.
     * Assumes app is on Launch screen (after logout or fresh start).
     * Flow: Launch → Continue Training → Login Choice → Log In Via Email → Login Form → Dashboard
     */
    @Step("Login with credentials: {email}")
    public void loginWithCredentials(String email, String password) {
        screens.launch().waitForScreen();
        screens.launch().tapContinueTraining();

        submitLoginForm(email, password);

        // Post-login: app may show interstitials (Passkey screen, Tips popup) before
        // Dashboard. Poll for known dismissable buttons; tap whichever appears.
        dismissPostLoginInterstitials();
        screens.dashboard().waitForScreen();
        log.info("Login complete — Dashboard visible");
    }

    /**
     * Submit the login form by composing atomic LoginScreen actions.
     * Assumes the Login Choice screen is visible (post-Continue-training).
     * Does NOT dismiss post-login interstitials — callers wanting to assert on
     * the Tips/Passkey screens (e.g. testPostLogin_TipsScreenDisplayed) call this directly.
     */
    @Step("Submit login form for {email}")
    public void submitLoginForm(String email, String password) {
        var login = screens.login();

        // Credential Manager / Password Manager may pop up RIGHT AFTER Continue Training,
        // blocking the login choice screen. Dismiss before waitForScreen.
        login.dismissPasswordManagerOverlay();
        login.waitForScreen();
        login.tapLoginViaEmail();

        // Password-manager / passkey overlay may appear AFTER tap (timing varies).
        // Poll: alternate between dismiss attempts and waitForLoginForm until form visible.
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            login.dismissPasswordManagerOverlay();
            try {
                login.waitForLoginFormShort();   // 3s wait
                break;
            } catch (Exception ignored) {
                // overlay may have re-appeared; loop to dismiss again
            }
        }
        login.waitForLoginForm();

        // Type email and verify the field accepted it. Overlay may pop up during typing,
        // making the field stale. Catch stale errors, dismiss overlay, and retry.
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                login.enterEmail(email);
                if (login.isEnteredEmail(email)) break;
                log.info("Email did not stick (attempt {}) — re-dismissing overlay", attempt);
            } catch (Exception e) {
                log.info("Email entry failed (attempt {}): {} — dismissing overlay and retrying",
                        attempt, e.getMessage());
            }
            login.dismissPasswordManagerOverlay();
        }
        login.enterPassword(password);
        login.tapSubmit();
        log.info("Login form submitted for {}", email);
    }

    /** Dismiss post-login interstitials (Passkey screen, Tips popup) if shown,
     *  otherwise exit fast when Dashboard's tab bar appears.
     *  Passkey appears once per account — once "No, use password" is tapped it won't
     *  reappear on subsequent logins, so this method must NOT block waiting for it. */
    private void dismissPostLoginInterstitials() {
        var d = DriverManager.getDriver();
        boolean iOS = "ios".equalsIgnoreCase(ConfigManager.getInstance().getPlatform().name());

        // Locators — iOS uses XPath to disambiguate Button from child StaticText.
        // Android: registration Passkey screen uses neverButton/cta_button_inner;
        //          POST-LOGIN Passkey screen uses passkey_create_button_never (different IDs!)
        var passkeyBtn = iOS
                ? org.openqa.selenium.By.xpath("//XCUIElementTypeButton[@name=\"No, use password\"]")
                : AppiumBy.androidUIAutomator(
                        "new UiSelector().resourceIdMatches(\".*:id/(neverButton|passkey_create_button_never)\")");
        var tipsBtn = iOS
                ? org.openqa.selenium.By.xpath("//XCUIElementTypeButton[@name=\"Understood\"]")
                : AppiumBy.id("nn.mobile.app.med:id/cta_button_inner");
        var dashboardSignal = iOS
                ? org.openqa.selenium.By.xpath("//XCUIElementTypeButton[@name=\"Profile\" and @value=\"1\"]")
                : AppiumBy.id("nn.mobile.app.med:id/navigation_profile");

        // Interstitials appear SEQUENTIALLY and login is slow: on a real run the
        // login-form → Passkey transition alone took ~21s, after which the Tips popup
        // ("Understood") renders. A 20s budget expired before Tips appeared, leaving it
        // un-dismissed and blocking the Dashboard. Poll long enough to clear BOTH; the
        // loop returns the instant the Dashboard tab bar shows, so the happy path is unaffected.
        long deadline = System.currentTimeMillis() + 90_000;
        while (System.currentTimeMillis() < deadline) {
            if (isAnyDisplayed(d, dashboardSignal)) {
                log.info("Dashboard tab bar visible — no interstitials");
                return;
            }
            // Android: OS save-password popup (Samsung Pass / Google PM) appears
            // BEFORE the in-app Passkey screen. Reuse the registration dismissal logic.
            if (!iOS && checkAndDismissAndroidPasswordManager(d)) continue;

            // After tapping Passkey or Tips the dialog takes 1-3s to transition out;
            // wait for the tapped button to disappear before re-checking the loop, so
            // we don't fire 4-6 redundant clicks on the same dialog.
            if (tapIfDisplayed(d, passkeyBtn, "Post-login Passkey screen", "No, use password")) {
                waitUntilGone(d, passkeyBtn, 5);
                continue;
            }
            if (tapIfDisplayed(d, tipsBtn, "Post-login Tips popup", "Understood")) {
                waitUntilGone(d, tipsBtn, 5);
                continue;
            }
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
    }

    private boolean isAnyDisplayed(io.appium.java_client.AppiumDriver d, org.openqa.selenium.By locator) {
        var els = d.findElements(locator);
        if (els.isEmpty()) return false;
        try { return els.get(0).isDisplayed(); }
        catch (Exception e) { return false; }
    }

    private boolean tapIfDisplayed(io.appium.java_client.AppiumDriver d, org.openqa.selenium.By locator,
                                   String label, String name) {
        var els = d.findElements(locator);
        if (els.isEmpty()) return false;
        try {
            if (els.get(0).isDisplayed()) {
                log.info("{} detected — tapping '{}'", label, name);
                els.get(0).click();
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /** Wait up to {@code timeoutSeconds} for {@code locator} to disappear from the screen. */
    private void waitUntilGone(io.appium.java_client.AppiumDriver d, org.openqa.selenium.By locator,
                               int timeoutSeconds) {
        try {
            new WebDriverWait(d, Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (Exception e) {
            log.debug("Element {} still visible after {}s — continuing loop", locator, timeoutSeconds);
        }
    }

    /**
     * Login with the credentials from the current test (saved during registration).
     * Assumes app is on Launch screen.
     */
    @Step("Login with current test credentials")
    public void loginWithCurrentCredentials() {
        loginWithCredentials(currentEmail, currentPassword);
    }

    // ──────────────────────────────────────────────
    // Stats API
    // ──────────────────────────────────────────────

    @Step("Check onboarding variant via Stats API")
    public int checkOnboardingVariant() {
        var digaService = new com.neuronation.api.DiGARegistrationService();
        digaService.login(currentEmail, currentPassword);
        digaService.validateSession();
        return new com.neuronation.api.StatsApiService().getOnboardingVariantId(
                digaService.getAccessToken(), digaService.getUserUniqueId());
    }

    // ──────────────────────────────────────────────
    // Wait helpers (no Thread.sleep!)
    // ──────────────────────────────────────────────

    /**
     * Dismiss OS password manager (Samsung Pass, Google, iCloud Keychain) if it appears.
     */
    @Step("Wait for and dismiss OS password manager")
    public void waitAndDismissOsPasswordManager() {
        var driver = DriverManager.getDriver();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8)).until(d -> {
                try {
                    if (isAndroid()) {
                        return checkAndDismissAndroidPasswordManager(d);
                    } else {
                        return checkAndDismissIosPasswordManager(d);
                    }
                } catch (Exception ignored) {}
                // Check if app already advanced
                var emailVerify = d.findElements(platformLocator(
                        "nn.mobile.app.med:id/positive_button", "positive_button"));
                if (!emailVerify.isEmpty()) {
                    log.info("App advanced — no OS dialog");
                    return true;
                }
                if (isAlertPresent()) {
                    log.info("Error dialog detected during password manager wait");
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            log.info("OS password manager wait timed out — continuing");
        }
    }

    @Step("Wait for dialog button and tap it")
    public void waitAndTapDialogButton(String androidButtonId, String iosButtonLabel) {
        var driver = DriverManager.getDriver();
        try {
            if (isAndroid()) {
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(ExpectedConditions.elementToBeClickable(AppiumBy.id(androidButtonId))).click();
            } else {
                new WebDriverWait(driver, Duration.ofSeconds(15))
                        .until(d -> !d.findElements(AppiumBy.iOSClassChain(
                                "**/XCUIElementTypeAlert/**/XCUIElementTypeButton[`label == '" + iosButtonLabel + "'`]")).isEmpty());
                driver.findElement(AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeAlert/**/XCUIElementTypeButton[`label == '" + iosButtonLabel + "'`]")).click();
            }
            log.info("Tapped dialog button: {} / {}", androidButtonId, iosButtonLabel);
        } catch (Exception e) {
            log.info("Dialog button not found — continuing");
        }
    }

    /**
     * Wait for either the expected next screen element OR an error dialog.
     */
    private void waitForEitherScreenOrError(String androidId, String iosId,
                                            String errorContext, String... errorDetails) {
        var driver = DriverManager.getDriver();
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
            if (isAlertPresent()) {
                com.neuronation.utils.BugReporter.checkForErrorDialog(errorContext, errorDetails);
                return true;
            }
            try {
                var el = d.findElements(platformLocator(androidId, iosId));
                if (!el.isEmpty() && el.get(0).isDisplayed()) return true;
            } catch (Exception ignored) {}
            return false;
        });
    }

    private void waitForElement(By locator, int timeoutSeconds) {
        new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public void dismissSystemDialog(String buttonText) {
        var driver = DriverManager.getDriver();
        if (isAndroid()) {
            try {
                var btn = driver.findElements(AppiumBy.id("android:id/button1"));
                if (!btn.isEmpty()) { btn.get(0).click(); return; }
            } catch (Exception ignored) {}
            try {
                var btn = driver.findElements(
                        AppiumBy.androidUIAutomator("new UiSelector().text(\"" + buttonText + "\")"));
                if (!btn.isEmpty()) { btn.get(0).click(); }
            } catch (Exception ignored) {}
        } else {
            try {
                var buttons = driver.findElements(AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeAlert/**/XCUIElementTypeButton[`label == '" + buttonText + "'`]"));
                if (!buttons.isEmpty()) buttons.get(0).click();
            } catch (Exception ignored) {}
        }
    }

    private void swipeUp() {
        var driver = DriverManager.getDriver();
        var dimensions = driver.manage().window().getSize();
        int startX = dimensions.getWidth() / 2;
        int startY = (int) (dimensions.getHeight() * 0.8);
        int endY = (int) (dimensions.getHeight() * 0.2);
        var finger = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        var swipe = new org.openqa.selenium.interactions.Sequence(finger, 0);
        swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(500),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, endY));
        swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(java.util.Collections.singletonList(swipe));
    }
}
