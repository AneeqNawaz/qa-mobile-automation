package com.neuronation.pages.med.profile;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.time.Duration;

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
        if (isAndroid()) {
            waitForVisible(privacyRequestSwitch);
            return;
        }
        // iOS: the 4 toggles are UNNAMED Switches (no accessibility id), so anchor on the title.
        new org.openqa.selenium.support.ui.WebDriverWait(driver, Duration.ofSeconds(10))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                        AppiumBy.iOSNsPredicateString(
                                "type == \"XCUIElementTypeStaticText\" AND name == \"Privacy Settings\"")));
    }

    @Step("Are all 4 privacy toggles present")
    public boolean allTogglesPresent() {
        if (isAndroid()) {
            return isDisplayedById("nn.mobile.app.med:id/privacy_request_switch")
                    && isDisplayedById("nn.mobile.app.med:id/keep_data_request_switch")
                    && isDisplayedById("nn.mobile.app.med:id/data_processing_request_switch")
                    && isDisplayedById("nn.mobile.app.med:id/newsletter_switch");
        }
        return driver.findElements(AppiumBy.className("XCUIElementTypeSwitch")).size() >= 4;
    }

    @Step("Is the newsletter toggle ON")
    public boolean isNewsletterEnabled() {
        if (isAndroid()) return switchChecked(newsletterSwitch);
        return iosToggleNearLabel("Receive newsletters");
    }

    @Step("Is the data-retention toggle ON")
    public boolean isDataRetentionEnabled() {
        if (isAndroid()) return switchChecked(keepDataSwitch);
        return iosToggleNearLabel("I would like my access to remain active");
    }

    @Step("Is the data-processing toggle ON")
    public boolean isDataProcessingEnabled() {
        if (isAndroid()) return switchChecked(dataProcessingSwitch);
        return iosToggleNearLabel("In order to support the further development");
    }

    private boolean switchChecked(WebElement sw) {
        if (isAndroid()) return "true".equals(sw.getAttribute("checked"));
        return "1".equals(sw.getAttribute("value"));
    }

    /** iOS: the privacy toggles are unnamed Switches; the one for a given paragraph is the Switch
     *  nearest that paragraph's StaticText by Y. */
    private boolean iosToggleNearLabel(String labelPrefix) {
        var labels = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == \"XCUIElementTypeStaticText\" AND name BEGINSWITH \"" + labelPrefix + "\""));
        if (labels.isEmpty()) return false;
        int ly = labels.get(0).getLocation().getY();
        WebElement best = null;
        int bestDy = Integer.MAX_VALUE;
        for (WebElement sw : driver.findElements(AppiumBy.className("XCUIElementTypeSwitch"))) {
            try {
                int dy = Math.abs(sw.getLocation().getY() - ly);
                if (dy < bestDy) { bestDy = dy; best = sw; }
            } catch (Exception ignored) {}
        }
        return best != null && "1".equals(best.getAttribute("value"));
    }

    @Step("Go back to Profile")
    public void tapBack() {
        driver.navigate().back();
    }
}
