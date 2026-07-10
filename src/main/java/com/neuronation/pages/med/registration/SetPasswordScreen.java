package com.neuronation.pages.med.registration;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Set Password screen — appears after Passkey dialog (Later/No options).
 * User creates a password for their account.
 */
public class SetPasswordScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Set your password")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/setPasswordText")
    @iOSXCUITFindBy(accessibility = "Please insert the new password you want to use for your account")
    private WebElement descriptionText;

    @AndroidFindBy(id = "nn.mobile.app.med:id/textPassword")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeSecureTextField")
    private WebElement passwordInput;

    @AndroidFindBy(id = "nn.mobile.app.med:id/text_input_end_icon")
    @iOSXCUITFindBy(accessibility = "show")
    private WebElement showPasswordToggle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/LoginSubmitButton")
    @iOSXCUITFindBy(accessibility = "Create Account")
    private WebElement submitButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/trustText")
    @iOSXCUITFindBy(accessibility = "Please make sure that only you have access to your end device and use trusted networks. Security problems that could otherwise arise cannot be fully addressed by us.")
    private WebElement trustText;

    @AndroidFindBy(accessibility = "Navigate up")
    @iOSXCUITFindBy(accessibility = "BackButton")
    private WebElement backButton;

    @Step("Wait for Set Password screen to load")
    public void waitForScreen() {
        waitForVisible(passwordInput);
    }

    @Step("Verify Set Password screen is displayed")
    public boolean isDisplayed() {
        return isDisplayed(passwordInput);
    }

    @Step("Enter password: {password}")
    public void enterPassword(String password) {
        log.info("Entering password");
        type(passwordInput, password);
    }

    @Step("Tap Show/Hide password toggle")
    public void tapShowPassword() {
        log.info("Toggling password visibility");
        tap(showPasswordToggle);
    }

    @Step("Tap 'Create Account' submit button")
    public void tapSubmit() {
        log.info("Tapping Create Account");
        tap(submitButton);
    }

    @Step("Set password and submit: {password}")
    public void setPasswordAndSubmit(String password) {
        if (isIOS()) {
            // iOS shows a "Use Strong Password?" popup the instant the SecureTextField gains focus,
            // which otherwise swallows every keystroke after the first (the "only one digit typed"
            // symptom). So focus FIRST, dismiss that popup, THEN type the whole password in one clean
            // pass — once declined it does not reappear, so no retype dance is needed.
            // 1. Focus the field (triggers the popup) and wait for it.
            passwordInput.click();
            try {
                new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(3))
                        .until(d -> !d.findElements(
                                io.appium.java_client.AppiumBy.accessibilityId("xmark")).isEmpty());
            } catch (Exception ignored) {}
            // 2. Dismiss "Use Strong Password?" before typing.
            dismissStrongPasswordPopup();
            // 3. Type the full password once (clean now).
            type(passwordInput, password);
            // 4. Safety net: if the popup still slipped in and truncated input, dismiss + retype once.
            if (dismissStrongPasswordPopup()) {
                type(passwordInput, password);
            }
            // 5. Dismiss keyboard via Next button, then Create Account.
            try {
                var nextBtn = driver.findElements(io.appium.java_client.AppiumBy.accessibilityId("Next:"));
                if (!nextBtn.isEmpty()) nextBtn.get(0).click();
            } catch (Exception ignored) {}
            tapSubmit();
        } else {
            enterPassword(password);
            tapSubmit();
        }
    }

    /**
     * iOS: dismiss Apple's "Use Strong Password" suggestion popup.
     * Looks for the close/cross button on the popup.
     */
    private boolean dismissStrongPasswordPopup() {
        try {
            // iOS "Use Strong Password?" popup — close via the "xmark" (X) button
            var xmark = driver.findElements(io.appium.java_client.AppiumBy.accessibilityId("xmark"));
            if (!xmark.isEmpty()) {
                xmark.get(0).click();
                log.info("Dismissed 'Use Strong Password' popup via xmark");
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Step("Tap back button on Set Password screen")
    public void tapBack() {
        log.info("Tapping Back");
        tap(backButton);
    }

    public String getToolbarTitle() {
        return getText(toolbarTitle);
    }

    public String getDescriptionText() {
        return getText(descriptionText);
    }

    public boolean isErrorDialogDisplayed() {
        return isAlertPresent();
    }

    public String getErrorDialogMessage() {
        return getAlertMessage();
    }

    @Step("Dismiss error dialog")
    public void dismissErrorDialog() {
        dismissAlert();
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("descriptionText", getTextSafe(descriptionText));
        content.put("passwordPlaceholder", getTextSafe(passwordInput));
        content.put("submitButton", getTextSafe(submitButton));
        content.put("trustText", getTextSafe(trustText));
        return content;
    }

    @io.qameta.allure.Step("Try submit with empty password")
    public boolean trySubmitEmptyPassword() {
        tapSubmit();
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        if (isErrorDialogDisplayed()) { log.info("Empty password error: {}", getErrorDialogMessage()); dismissErrorDialog(); }
        return isDisplayed();
    }

    @io.qameta.allure.Step("Try submit with short password: {shortPass}")
    public String tryShortPassword(String shortPass) {
        enterPassword(shortPass);
        tapSubmit();
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        if (isErrorDialogDisplayed()) { String msg = getErrorDialogMessage(); dismissErrorDialog(); return msg; }
        return null;
    }

}
