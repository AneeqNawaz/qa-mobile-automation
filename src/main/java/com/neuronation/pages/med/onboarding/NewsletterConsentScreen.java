package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Newsletter Consent screen — "Stay informed"
 * Asks the user whether they want to receive newsletter communications.
 */
public class NewsletterConsentScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Stay informed")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/content")
    @iOSXCUITFindBy(accessibility = "newsletter_content")
    private WebElement content;

    @AndroidFindBy(id = "nn.mobile.app.med:id/image")
    @iOSXCUITFindBy(accessibility = "newsletter_image")
    private WebElement image;

    @AndroidFindBy(id = "nn.mobile.app.med:id/positive_button")
    @iOSXCUITFindBy(accessibility = "I agree")
    private WebElement agreeButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/negative_button")
    @iOSXCUITFindBy(accessibility = "I disagree")
    private WebElement disagreeButton;

    @Step("Tap 'I agree' on Newsletter Consent screen")
    public void tapAgree() {
        log.info("Tapping 'I agree' on Newsletter Consent screen");
        tap(agreeButton);
    }

    @Step("Tap 'I disagree' on Newsletter Consent screen")
    public void tapDisagree() {
        log.info("Tapping 'I disagree' on Newsletter Consent screen");
        tap(disagreeButton);
    }

    @Step("Wait for Newsletter Consent screen to load")
    public void waitForScreen() {
        waitForVisible(agreeButton);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("content", getTextSafe(this.content));
        content.put("agreeButton", getTextSafe(agreeButton));
        content.put("disagreeButton", getTextSafe(disagreeButton));
        return content;
    }
}
