package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Promise screen — "Promise?"
 * Asks user to commit to receiving notification reminders.
 * Options: "Yes" / "No"
 */
public class PromiseScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/alertTitle")
    @iOSXCUITFindBy(accessibility = "Promise?")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/content")
    @iOSXCUITFindBy(accessibility = "promise_content")
    private WebElement content;

    @AndroidFindBy(id = "android:id/button1")
    @iOSXCUITFindBy(accessibility = "Yes, absolutely!")
    private WebElement yesButton;

    @AndroidFindBy(id = "android:id/button2")
    @iOSXCUITFindBy(accessibility = "No")
    private WebElement noButton;

    @Step("Tap 'Yes' on Promise screen")
    public void tapYes() {
        log.info("Tapping 'Yes' on Promise screen");
        tap(yesButton);
    }

    @Step("Tap 'No' on Promise screen")
    public void tapNo() {
        log.info("Tapping 'No' on Promise screen");
        tap(noButton);
    }

    @Step("Wait for Promise screen to load")
    public void waitForScreen() {
        waitForVisible(toolbarTitle);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> captured = new LinkedHashMap<>();
        captured.put("toolbarTitle", getTextSafe(toolbarTitle));
        captured.put("content", getTextSafe(content));
        captured.put("yesButton", getTextSafe(yesButton));
        captured.put("noButton", getTextSafe(noButton));
        return captured;
    }
}
