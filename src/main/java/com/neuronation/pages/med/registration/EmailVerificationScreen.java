package com.neuronation.pages.med.registration;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Email Verification / Protect Your Account screen.
 * Appears after password setup. Shows instructions to check email.
 * Has "Resend email" and "Change email address" buttons.
 * Auto-advances after email is verified via API.
 */
public class EmailVerificationScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Protect your account")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/content")
    @iOSXCUITFindBy(accessibility = "verification_content")
    private WebElement contentText;

    @AndroidFindBy(id = "nn.mobile.app.med:id/positive_button")
    @iOSXCUITFindBy(accessibility = "Resend email")
    private WebElement resendButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/negative_button")
    @iOSXCUITFindBy(accessibility = "Change email address")
    private WebElement changeEmailButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/image")
    @iOSXCUITFindBy(accessibility = "verification_image")
    private WebElement headerImage;

    @Step("Wait for Email Verification screen to load")
    public void waitForScreen() {
        // Use toolbar title — consistent across Android versions
        // (button IDs differ: positive_button on API 36, positive_button_inner on API 34)
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                        platformLocator("nn.mobile.app.med:id/main_toolbar_title", "Protect your account")));
    }

    @Step("Verify Email Verification screen is displayed")
    public boolean isDisplayed() {
        return isDisplayed(resendButton);
    }

    @Step("Tap 'Resend email' button")
    public void tapResendEmail() {
        log.info("Tapping Resend email");
        tap(resendButton);
    }

    @Step("Tap 'Change email address' button")
    public void tapChangeEmail() {
        log.info("Tapping Change email address");
        tap(changeEmailButton);
    }

    public String getToolbarTitle() {
        return getText(toolbarTitle);
    }

    public String getContentText() {
        return getText(contentText);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        // Buttons may be below fold on smaller screens — use getTextByPlatformId for instant check
        String resend = getTextByPlatformId("nn.mobile.app.med:id/positive_button", "Resend email");
        String change = getTextByPlatformId("nn.mobile.app.med:id/negative_button", "Change email address");
        if (!resend.isEmpty()) content.put("resendButton", resend);
        if (!change.isEmpty()) content.put("changeEmailButton", change);
        return content;
    }
}
