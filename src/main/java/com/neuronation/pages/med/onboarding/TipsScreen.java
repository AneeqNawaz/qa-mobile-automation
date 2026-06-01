package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tips screen — appears multiple times with varying toolbar titles
 * ("Tips", "Important information", "Attention") and different content.
 * Each instance shows a tip with an image and an "Understood" button.
 */
public class TipsScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "tips_toolbar_title")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/subtitle")
    @iOSXCUITFindBy(accessibility = "tips_subtitle")
    private WebElement subtitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/image")
    @iOSXCUITFindBy(accessibility = "tips_image")
    private WebElement image;

    @AndroidFindBy(id = "nn.mobile.app.med:id/content")
    @iOSXCUITFindBy(accessibility = "tips_content")
    private WebElement content;

    @AndroidFindBy(id = "nn.mobile.app.med:id/cta_button_inner")
    @iOSXCUITFindBy(accessibility = "Understood")
    private WebElement understoodButton;

    @Step("Tap 'Understood' on Tips screen")
    public void tapUnderstood() {
        log.info("Tapping 'Understood' on Tips screen");
        tap(understoodButton);
    }

    @Step("Get title from Tips screen")
    public String getTitle() {
        log.info("Getting toolbar title from Tips screen");
        return getText(toolbarTitle);
    }

    @Step("Get content text from Tips screen")
    public String getContent() {
        log.info("Getting content text from Tips screen");
        return getText(content);
    }

    @Step("Check if tip image is visible")
    public boolean isImageDisplayed() {
        return isImageVisible(image);
    }

    @Step("Verify tip content is displayed (image + text)")
    public boolean verifyContentDisplayed() {
        boolean imgVisible = isImageVisible(image);
        boolean textPresent = !getTextSafe(content).isEmpty() || !getTextSafe(subtitle).isEmpty();
        log.info("Tips content check: image={}, text={}", imgVisible, textPresent);
        return imgVisible || textPresent;
    }

    @Step("Wait for Tips screen to load")
    public void waitForScreen() {
        waitForVisible(understoodButton);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> captured = new LinkedHashMap<>();
        captured.put("toolbarTitle", getTextSafe(toolbarTitle));
        captured.put("subtitle", getTextSafe(subtitle));
        captured.put("content", getTextSafe(content));
        captured.put("imageVisible", String.valueOf(isImageVisible(image)));
        captured.put("understoodButton", getTextSafe(understoodButton));
        return captured;
    }
}
