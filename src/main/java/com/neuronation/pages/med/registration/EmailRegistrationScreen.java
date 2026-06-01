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
 * Email Registration Form — enter email, accept terms, privacy, and register.
 * Required fields marked with * : Terms of Use, Privacy Policy.
 * Optional: data retention consent, data processing consent.
 * Submit button appears after scrolling down or checking required boxes.
 */
public class EmailRegistrationScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Create account")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/textEmailAddress")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField")
    private WebElement emailInput;

    @AndroidFindBy(id = "nn.mobile.app.med:id/loginTouCheck")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeSwitch[1]")
    private WebElement termsCheckbox;

    @AndroidFindBy(id = "nn.mobile.app.med:id/loginPPCheck")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeSwitch[2]")
    private WebElement privacyCheckbox;

    @AndroidFindBy(id = "nn.mobile.app.med:id/dataRetainConsentCheck")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeSwitch[3]")
    private WebElement dataRetainCheckbox;

    @AndroidFindBy(id = "nn.mobile.app.med:id/dataProcessingConsentCheck")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeSwitch[4]")
    private WebElement dataProcessingCheckbox;

    @AndroidFindBy(id = "nn.mobile.app.med:id/trustText")
    @iOSXCUITFindBy(accessibility = "Please make sure that only you have access to your end device and use trusted networks. Security problems that could otherwise arise cannot be fully addressed by us.")
    private WebElement trustText;

    @AndroidFindBy(id = "nn.mobile.app.med:id/requiredCheckInfo")
    @iOSXCUITFindBy(accessibility = "* indicates a required field")
    private WebElement requiredFieldInfo;

    @AndroidFindBy(accessibility = "Navigate up")
    @iOSXCUITFindBy(accessibility = "BackButton")
    private WebElement backButton;

    @Step("Wait for Email Registration screen to load")
    public void waitForScreen() {
        waitForVisible(emailInput);
    }

    @Step("Verify Email Registration screen is displayed")
    public boolean isDisplayed() {
        return isDisplayed(emailInput);
    }

    @Step("Enter email address: {email}")
    public void enterEmail(String email) {
        log.info("Entering email: {}", email);
        type(emailInput, email);
        if (isIOS()) {
            dismissKeyboardIOS();
        }
    }

    @Step("Check Terms of Use checkbox (required)")
    public void checkTerms() {
        log.info("Checking Terms of Use");
        if (!isToggleOn(termsCheckbox)) {
            tap(termsCheckbox);
        }
    }

    @Step("Check Privacy Policy checkbox (required)")
    public void checkPrivacy() {
        log.info("Checking Privacy Policy");
        if (!isToggleOn(privacyCheckbox)) {
            tap(privacyCheckbox);
        }
    }

    @Step("Check Data Retention checkbox (optional)")
    public void checkDataRetain() {
        log.info("Checking Data Retention consent");
        swipeUp();
        if (!isToggleOn(dataRetainCheckbox)) {
            tap(dataRetainCheckbox);
        }
    }

    @Step("Check Data Processing checkbox (optional)")
    public void checkDataProcessing() {
        log.info("Checking Data Processing consent");
        if (!isToggleOn(dataProcessingCheckbox)) {
            tap(dataProcessingCheckbox);
        }
    }

    /**
     * Check if a toggle/checkbox is on.
     * Android: isSelected(). iOS switches: value is "1" when on, "0" when off.
     */
    private boolean isToggleOn(WebElement element) {
        if (isIOS()) {
            String val = element.getAttribute("value");
            return "1".equals(val);
        }
        return element.isSelected();
    }

    @Step("Accept all required checkboxes")
    public void acceptRequiredCheckboxes() {
        checkTerms();
        checkPrivacy();
    }

    /**
     * iOS: dismiss keyboard by tapping the "Next:" button on the iOS keyboard.
     */
    private void dismissKeyboardIOS() {
        try {
            var nextBtn = driver.findElements(io.appium.java_client.AppiumBy.accessibilityId("Next:"));
            if (!nextBtn.isEmpty()) {
                nextBtn.get(0).click();
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                // Tap Next again to dismiss completely if still showing
                nextBtn = driver.findElements(io.appium.java_client.AppiumBy.accessibilityId("Next:"));
                if (!nextBtn.isEmpty()) nextBtn.get(0).click();
            }
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        } catch (Exception ignored) {}
    }

    @Step("Accept all checkboxes (required + optional)")
    public void acceptAllCheckboxes() {
        checkTerms();
        checkPrivacy();
        checkDataRetain();
        checkDataProcessing();
    }

    @Step("Scroll back to email input field")
    public void scrollToEmailInput() {
        log.info("Scrolling back to email input field");
        scrollToElement("nn.mobile.app.med:id/textEmailAddress", "Email address");
    }

    @Step("Scroll to and tap 'Create Account' submit button")
    public void scrollToSubmit() {
        log.info("Scrolling to Create Account submit button");
        if (isAndroid()) {
            scrollToElement("nn.mobile.app.med:id/LoginSubmitButton", "Create Account");
        } else {
            // iOS: swipe until Create Account button visible
            var locator = io.appium.java_client.AppiumBy.accessibilityId("Create Account");
            for (int i = 0; i < 10; i++) {
                var elements = driver.findElements(locator);
                if (!elements.isEmpty() && elements.get(0).isDisplayed()) return;
                swipeUp();
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }
        }
    }

    @Step("Tap 'Create Account' submit button")
    public void tapSubmit() {
        log.info("Tapping Create Account submit button");
        if (isAndroid()) {
            hideKeyboardSafe();
        }
        WebElement submit = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(
                        platformLocator("nn.mobile.app.med:id/LoginSubmitButton", "Create Account")));
        submit.click();
    }

    @Step("Complete registration with email: {email}")
    public void registerWithEmail(String email) {
        enterEmail(email);
        acceptRequiredCheckboxes();
        scrollToSubmit();
        tapSubmit();
    }

    @Step("Tap back button on Registration screen")
    public void tapBack() {
        log.info("Tapping Back");
        tap(backButton);
    }

    public String getToolbarTitle() {
        return getText(toolbarTitle);
    }

    public String getTermsText() {
        return getTextSafe(termsCheckbox);
    }

    public String getPrivacyText() {
        return getTextSafe(privacyCheckbox);
    }

    public String getTrustText() {
        return getTextSafe(trustText);
    }

    public boolean isTermsChecked() {
        return isToggleOn(termsCheckbox);
    }

    public boolean isPrivacyChecked() {
        return isToggleOn(privacyCheckbox);
    }

    /**
     * Check if an error dialog appeared (e.g. for validation errors).
     */
    public boolean isErrorDialogDisplayed() {
        return isAlertPresent();
    }

    public String getErrorDialogMessage() {
        return getAlertMessage();
    }

    @Step("Dismiss error dialog by tapping OK")
    public void dismissErrorDialog() {
        dismissAlert();
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("emailPlaceholder", getTextSafe(emailInput));
        content.put("termsCheckbox", getTextSafe(termsCheckbox));
        content.put("privacyCheckbox", getTextSafe(privacyCheckbox));
        content.put("dataRetainCheckbox", getTextSafe(dataRetainCheckbox));
        content.put("dataProcessingCheckbox", getTextSafe(dataProcessingCheckbox));
        content.put("trustText", getTextSafe(trustText));
        content.put("requiredFieldInfo", getTextSafe(requiredFieldInfo));
        return content;
    }

    @io.qameta.allure.Step("Try submit without required checkboxes")
    public boolean trySubmitWithoutCheckboxes(String email) {
        enterEmail(email);
        scrollToSubmit();
        tapSubmit();
        return isStillOnRegistrationScreen();
    }

    @io.qameta.allure.Step("Try submit with empty email — checkboxes NOT checked")
    public boolean trySubmitWithEmptyEmail() {
        scrollToEmailInput();
        type(emailInput, "");
        scrollToSubmit();
        tapSubmit();
        // Verify still on registration screen via toolbar (submit button may scroll off on emulator)
        return isStillOnRegistrationScreen();
    }

    @io.qameta.allure.Step("Try submit with invalid email format: {invalidEmail} — checkboxes NOT checked")
    public boolean trySubmitWithInvalidEmail(String invalidEmail) {
        scrollToEmailInput();
        type(emailInput, invalidEmail);
        scrollToSubmit();
        tapSubmit();
        return isStillOnRegistrationScreen();
    }

    /**
     * Check if we're still on the email registration screen.
     * Uses toolbar title (always visible) — submit button may be scrolled off on some devices.
     */
    private boolean isStillOnRegistrationScreen() {
        try {
            var toolbar = findAllByPlatformId(
                    "nn.mobile.app.med:id/main_toolbar_title", "Create account");
            if (!toolbar.isEmpty()) {
                return "Create account".equals(toolbar.get(0).getText());
            }
            return !findAllByPlatformId(
                    "nn.mobile.app.med:id/LoginSubmitButton", "Create Account").isEmpty();
        } catch (Exception e) { return false; }
    }

    @io.qameta.allure.Step("Verify Terms checkbox mentions Terms of Use")
    public boolean verifyTermsContent() {
        return getTextSafe(termsCheckbox).contains("Terms of Use");
    }

    @io.qameta.allure.Step("Verify Privacy checkbox mentions Privacy Policy")
    public boolean verifyPrivacyContent() {
        return getTextSafe(privacyCheckbox).contains("Privacy Policy");
    }

}
