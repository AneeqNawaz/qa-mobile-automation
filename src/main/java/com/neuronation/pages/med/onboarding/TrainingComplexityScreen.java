package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Training Complexity dialog screen.
 * Asks user whether to activate or deactivate training complexity adjustments.
 */
public class TrainingComplexityScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "training_complexity_title")
    private WebElement title;

    @AndroidFindBy(id = "nn.mobile.app.med:id/content")
    @iOSXCUITFindBy(accessibility = "training_complexity_content")
    private WebElement content;

    @AndroidFindBy(id = "nn.mobile.app.med:id/autonomy_selection_manual")
    @iOSXCUITFindBy(accessibility = "autonomy_selection_manual")
    private WebElement activateButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/autonomy_selection_automatic")
    @iOSXCUITFindBy(accessibility = "autonomy_selection_automatic")
    private WebElement deactivateButton;

    @Step("Tap 'Activate' on Training Complexity screen")
    public void tapActivate() {
        log.info("Tapping 'Activate' on Training Complexity screen");
        tap(activateButton);
    }

    @Step("Tap 'Deactivate' on Training Complexity screen")
    public void tapDeactivate() {
        log.info("Tapping 'Deactivate' on Training Complexity screen");
        tap(deactivateButton);
    }

    @Step("Wait for Training Complexity screen to load")
    public void waitForScreen() {
        waitForVisible(activateButton);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> captured = new LinkedHashMap<>();
        captured.put("title", getTextSafe(title));
        captured.put("content", getTextByPlatformId("nn.mobile.app.med:id/content", "training_complexity_content"));
        captured.put("activateButton", getTextSafe(activateButton));
        captured.put("deactivateButton", getTextSafe(deactivateButton));
        return captured;
    }
}
