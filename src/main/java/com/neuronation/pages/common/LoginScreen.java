package com.neuronation.pages.common;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Login screen — for returning users.
 *
 * Layout (two sub-screens, both owned by this POM):
 *   Login Choice: main_toolbar_title="Login", LoginMailButtonSimple, LoginHealthIdButton
 *   Login Form:   textEmailAddress, textPassword, text_input_end_icon (show pwd),
 *                 LoginSubmitButton, LoginForgotFassButton
 *
 * Methods are atomic POM actions — composition (overlay-dismiss + retype loop +
 * post-login interstitials) lives in MedFlowHelper.
 */
public class LoginScreen extends BaseScreen {

    // ── Login Choice screen ──
    @AndroidFindBy(id = "nn.mobile.app.med:id/LoginMailButtonSimple")
    @iOSXCUITFindBy(accessibility = "LoginMailButtonSimple")
    private WebElement loginViaEmailButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/LoginHealthIdButton")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeButton[`label == 'Via Gesundheits-ID'`]")
    private WebElement loginViaHealthIdButton;

    // ── Login Form ──
    @AndroidFindBy(id = "nn.mobile.app.med:id/textEmailAddress")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField")
    private WebElement emailInput;

    @AndroidFindBy(id = "nn.mobile.app.med:id/textPassword")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeSecureTextField")
    private WebElement passwordInput;

    @AndroidFindBy(id = "nn.mobile.app.med:id/text_input_end_icon")
    @iOSXCUITFindBy(accessibility = "show")
    private WebElement showPasswordToggle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/LoginSubmitButton")
    @iOSXCUITFindBy(accessibility = "Log In Via Email")
    private WebElement submitButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/LoginForgotFassButton")
    @iOSXCUITFindBy(accessibility = "Forgot your password?")
    private WebElement forgotPasswordButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Login")
    private WebElement toolbarTitle;

    // ──────────────────────────────────────────────
    // Waits
    // ──────────────────────────────────────────────

    @Step("Wait for Login Choice screen")
    public void waitForScreen() {
        waitForVisible(loginViaEmailButton);
    }

    @Step("Wait for Login Form")
    public void waitForLoginForm() {
        waitForVisible(emailInput);
    }

    /** Short-timeout wait used in dismiss-poll loop. Throws if not visible within 3s. */
    @Step("Wait for Login Form (short timeout)")
    public void waitForLoginFormShort() {
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(3))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf(emailInput));
    }

    public boolean isDisplayed() {
        return isDisplayed(loginViaEmailButton) || isDisplayed(emailInput);
    }

    public String getToolbarTitle() {
        return getText(toolbarTitle);
    }

    // ──────────────────────────────────────────────
    // Atomic actions — Login Choice screen
    // ──────────────────────────────────────────────

    @Step("Tap 'Log In Via Email' on Login Choice screen")
    public void tapLoginViaEmail() {
        tap(loginViaEmailButton);
    }

    @Step("Tap 'Via Gesundheits-ID' on Login Choice screen")
    public void tapLoginViaHealthId() {
        tap(loginViaHealthIdButton);
    }

    // ──────────────────────────────────────────────
    // Atomic actions — Login Form
    // ──────────────────────────────────────────────

    @Step("Enter email: {email}")
    public void enterEmail(String email) {
        type(emailInput, email);
    }

    @Step("Enter password")
    public void enterPassword(String password) {
        type(passwordInput, password);
    }

    /** Reads the current value of the email field — Android via getText, iOS via the value attribute. */
    public String getEnteredEmail() {
        return isAndroid() ? emailInput.getText() : emailInput.getAttribute("value");
    }

    /** Returns true if the email field currently contains the expected address. */
    public boolean isEnteredEmail(String expectedEmail) {
        String typed = getEnteredEmail();
        if (typed == null || expectedEmail == null) return false;
        int prefix = Math.min(10, expectedEmail.length());
        return typed.contains(expectedEmail.substring(0, prefix));
    }

    @Step("Tap Show/Hide password toggle")
    public void tapShowPassword() {
        tap(showPasswordToggle);
    }

    @Step("Tap 'Forgot your password?'")
    public void tapForgotPassword() {
        tap(forgotPasswordButton);
    }

    /**
     * Submit the login form. iOS: prefers the keyboard "Go" key (the submit button is
     * often hidden behind the keyboard); falls back to hiding the keyboard + tapping
     * the submit button. Android: taps the submit button directly.
     */
    @Step("Submit login form")
    public void tapSubmit() {
        if (isIOS()) {
            try {
                var goBtn = driver.findElements(AppiumBy.accessibilityId("Go"));
                if (!goBtn.isEmpty()) {
                    goBtn.get(0).click();
                    return;
                }
            } catch (Exception ignored) {}
            hideKeyboardSafe();
        }
        tap(submitButton);
    }

    // ──────────────────────────────────────────────
    // OS password manager / passkey overlay
    // ──────────────────────────────────────────────

    /**
     * Dismiss the OS password manager / passkey overlay that appears after tapping
     * "Log In Via Email". This is a system surface — invisible or partially visible
     * to Appium, so we use either UiSelector text matching (Android) or coordinate
     * tap on the close X (iOS).
     *
     *   Android: Google PM / Samsung Pass shows saved sign-ins → back press
     *   iOS:     System Passkey sheet → tap X close button at ~90% width / ~18% height
     */
    @Step("Dismiss OS password manager / passkey overlay")
    public void dismissPasswordManagerOverlay() {
        try {
            if (isAndroid()) {
                // Pixel emulator: Credential Manager has "Close sheet" content-desc button
                var closeBtn = driver.findElements(AppiumBy.accessibilityId("Close sheet"));
                if (!closeBtn.isEmpty()) {
                    closeBtn.get(0).click();
                    log.info("Dismissed Android Credential Manager via 'Close sheet' button");
                    return;
                }
                // Samsung physical: Credential Manager has "Cancel" text button
                // Only tap if we're sure it's the credential overlay (check for known surrounding text)
                var passkeyOverlay = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"saved passkey\")" +
                        ".fromParent(new UiSelector().packageName(\"com.android.credentialmanager\"))"));
                var saveSignIn = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"Choose a saved\")"));
                if (!passkeyOverlay.isEmpty() || !saveSignIn.isEmpty()) {
                    var cancelBtn = driver.findElements(AppiumBy.androidUIAutomator(
                            "new UiSelector().packageName(\"com.android.credentialmanager\").text(\"Cancel\")"));
                    if (!cancelBtn.isEmpty()) {
                        cancelBtn.get(0).click();
                        log.info("Dismissed Samsung Credential Manager via 'Cancel' button");
                        return;
                    }
                    // Fall back to back press for the credential overlay
                    driver.navigate().back();
                    log.info("Dismissed Android credential overlay via back press");
                }
            } else {
                var dimensions = driver.manage().window().getSize();
                int xBtn = (int) (dimensions.getWidth() * 0.903);
                int yBtn = (int) (dimensions.getHeight() * 0.182);
                var finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                var tap = new Sequence(finger, 0);
                tap.addAction(finger.createPointerMove(Duration.ZERO,
                        PointerInput.Origin.viewport(), xBtn, yBtn));
                tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(Collections.singletonList(tap));
                log.info("Dismissed iOS Passkey sheet via X at ({}, {})", xBtn, yBtn);
            }
        } catch (Exception e) {
            log.debug("No password manager overlay detected: {}", e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // Snapshot
    // ──────────────────────────────────────────────

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("loginViaEmailButton", getTextSafe(loginViaEmailButton));
        content.put("submitButton", getTextSafe(submitButton));
        content.put("forgotPasswordButton", getTextSafe(forgotPasswordButton));
        return content;
    }
}
