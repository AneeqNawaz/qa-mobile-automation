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

    /** Non-blocking: is the email field visible right now? Used to poll the login form while a
     *  credential chooser may be covering it (returns fast, never throws). */
    public boolean isLoginFormVisible() {
        try {
            var els = driver.findElements(isAndroid()
                    ? AppiumBy.id("nn.mobile.app.med:id/textEmailAddress")
                    : AppiumBy.iOSClassChain("**/XCUIElementTypeTextField"));
            return !els.isEmpty() && els.get(0).isDisplayed();
        } catch (Exception e) {
            return false;
        }
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
    /** @return true if a credential/password overlay was found and dismissed this call. */
    public boolean dismissPasswordManagerOverlay() {
        try {
            if (isAndroid()) {
                // The credential chooser (GMS Identity Credentials, AOSP/Pixel Credential Manager, or
                // Samsung Pass) covers the login form on re-login. FAST detection: only act when the
                // email field is NOT reachable (something's covering it) — a cheap resourceId check —
                // then look DIRECTLY for the chooser's dismiss control with cheap EXACT-match queries.
                // (The previous full-tree regex scans — textMatches/packageNameMatches — were slow when
                // polled repeatedly, which is why cancelling felt sluggish.)
                if (isLoginFormVisible()) return false;   // form reachable → nothing to dismiss
                // 1. Close 'X' — content-desc "Close" (GMS) or "Close sheet" (Pixel CM).
                for (String desc : new String[]{"Close", "Close sheet"}) {
                    var b = driver.findElements(AppiumBy.accessibilityId(desc));
                    if (!b.isEmpty()) {
                        b.get(0).click();
                        log.info("Dismissed credential chooser via '{}'", desc);
                        waitCredentialChooserGone();
                        return true;
                    }
                }
                // 2. Named dismiss buttons (Samsung uses "Cancel"). Exact text() — fast.
                for (String label : new String[]{"Cancel", "No thanks", "Not now", "Dismiss"}) {
                    var b = driver.findElements(AppiumBy.androidUIAutomator(
                            "new UiSelector().text(\"" + label + "\")"));
                    if (!b.isEmpty()) {
                        b.get(0).click();
                        log.info("Dismissed credential chooser via '{}'", label);
                        waitCredentialChooserGone();
                        return true;
                    }
                }
                // 3. Confirm it IS a chooser (chooser-only "Sign-in options" text) before a back press,
                //    so we never navigate away from a non-chooser screen. Exact text() — fast.
                if (!driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"Sign-in options\")")).isEmpty()) {
                    driver.navigate().back();
                    log.info("Dismissed credential chooser via back press");
                    waitCredentialChooserGone();
                    return true;
                }
                return false;
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
                return true;
            }
        } catch (Exception e) {
            log.debug("No password manager overlay detected: {}", e.getMessage());
        }
        return false;
    }

    /** After tapping a chooser's dismiss control, wait briefly for the login form to come back
     *  (a cheap resourceId poll — the form reappearing IS the "chooser gone" signal). Returns as
     *  soon as the field is visible, so a quick dismiss costs almost nothing. */
    private void waitCredentialChooserGone() {
        if (!isAndroid()) return;
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(3))
                    .pollingEvery(java.time.Duration.ofMillis(150))
                    .until(d -> isLoginFormVisible());
        } catch (Exception ignored) {}
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
