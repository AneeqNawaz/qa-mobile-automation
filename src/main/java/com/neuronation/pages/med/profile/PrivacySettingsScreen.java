package com.neuronation.pages.med.profile;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/**
 * Privacy Settings page — Profile → Privacy Settings (PrivacySettingsActivity).
 *
 * Four toggles (all with stable resource-ids):
 *   1. privacy_request_switch          — "I agree to the processing of …" (mandatory)
 *   2. keep_data_request_switch        — data retention
 *   3. data_processing_request_switch  — data processing
 *   4. newsletter_switch               — "Receive newsletters"
 *
 * The 4th (newsletter) reflects the "I agree / not agree" newsletter choice made after
 * email verification.
 */
public class PrivacySettingsScreen extends BaseScreen {

    // PrivacySettingsActivity's title TextView has NO resource-id, so anchor the screen on
    // the first toggle (stable id) instead of a toolbar title.
    @AndroidFindBy(id = "nn.mobile.app.med:id/privacy_request_switch")
    @iOSXCUITFindBy(accessibility = "privacy_request_switch")
    private WebElement privacyRequestSwitch;

    @AndroidFindBy(id = "nn.mobile.app.med:id/keep_data_request_switch")
    @iOSXCUITFindBy(accessibility = "keep_data_request_switch")
    private WebElement keepDataSwitch;

    @AndroidFindBy(id = "nn.mobile.app.med:id/data_processing_request_switch")
    @iOSXCUITFindBy(accessibility = "data_processing_request_switch")
    private WebElement dataProcessingSwitch;

    @AndroidFindBy(id = "nn.mobile.app.med:id/newsletter_switch")
    @iOSXCUITFindBy(accessibility = "newsletter_switch")
    private WebElement newsletterSwitch;

    @Step("Wait for Privacy Settings screen")
    public void waitForScreen() {
        waitForVisible(privacyRequestSwitch);
    }

    @Step("Are all 4 privacy toggles present")
    public boolean allTogglesPresent() {
        return isDisplayedById("nn.mobile.app.med:id/privacy_request_switch")
                && isDisplayedById("nn.mobile.app.med:id/keep_data_request_switch")
                && isDisplayedById("nn.mobile.app.med:id/data_processing_request_switch")
                && isDisplayedById("nn.mobile.app.med:id/newsletter_switch");
    }

    @Step("Is the newsletter toggle ON")
    public boolean isNewsletterEnabled() {
        return switchChecked(newsletterSwitch);
    }

    @Step("Is the data-retention toggle ON")
    public boolean isDataRetentionEnabled() {
        return switchChecked(keepDataSwitch);
    }

    @Step("Is the data-processing toggle ON")
    public boolean isDataProcessingEnabled() {
        return switchChecked(dataProcessingSwitch);
    }

    private boolean switchChecked(WebElement sw) {
        if (isAndroid()) return "true".equals(sw.getAttribute("checked"));
        return "1".equals(sw.getAttribute("value"));
    }

    @Step("Go back to Profile")
    public void tapBack() {
        driver.navigate().back();
    }
}
