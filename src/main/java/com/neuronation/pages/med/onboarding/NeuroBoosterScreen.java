package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NeuroBooster screen — "Personalise schedule"
 * Asks if the user wants to try the NeuroBooster feature.
 * Options: "Yes, let me try it out" / "No, I don't want to feel more focused"
 */
public class NeuroBoosterScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Personalise schedule")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/content")
    @iOSXCUITFindBy(accessibility = "neurobooster_content")
    private WebElement content;

    @AndroidFindBy(id = "nn.mobile.app.med:id/yes_button")
    @iOSXCUITFindBy(accessibility = "Yes, let me try it out")
    private WebElement yesButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/no_button")
    @iOSXCUITFindBy(accessibility = "setup_mini_teaser_button_no")
    private WebElement noButton;

    @Step("Tap 'Yes, let me try it out' on NeuroBooster screen")
    public void tapYes() {
        log.info("Tapping 'Yes, let me try it out' on NeuroBooster screen");
        tap(yesButton);
    }

    @Step("Tap 'No, I don't want to feel more focused' on NeuroBooster screen")
    public void tapNo() {
        log.info("Tapping 'No' on NeuroBooster screen");
        tap(noButton);
    }

    @Step("Wait for NeuroBooster screen to load")
    public void waitForScreen() {
        waitForVisible(yesButton);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> captured = new LinkedHashMap<>();
        captured.put("toolbarTitle", getTextSafe(toolbarTitle));
        captured.put("content", getTextByPlatformId("nn.mobile.app.med:id/content", "neurobooster_content"));
        captured.put("yesButton", getTextSafe(yesButton));
        captured.put("noButton", getTextSafe(noButton));
        return captured;
    }
}
