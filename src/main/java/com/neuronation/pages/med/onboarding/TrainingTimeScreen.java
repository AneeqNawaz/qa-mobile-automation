package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Training Time screen — "Personalise schedule" / "At what time of day..."
 * Options: "In the morning" (9:00 AM), "At noon" (2:00 PM),
 * "In the evening" (6:00 PM), "At night" (9:00 PM).
 */
public class TrainingTimeScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Personalise schedule")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/subtitle")
    @iOSXCUITFindBy(accessibility = "training_time_subtitle")
    private WebElement subtitle;

    @AndroidFindBy(accessibility = "setup_cycle_reminder_0")
    @iOSXCUITFindBy(accessibility = "setup_cycle_reminder_0")
    private WebElement morningCard;

    @AndroidFindBy(accessibility = "setup_cycle_reminder_1")
    @iOSXCUITFindBy(accessibility = "setup_cycle_reminder_1")
    private WebElement noonCard;

    @AndroidFindBy(accessibility = "setup_cycle_reminder_2")
    @iOSXCUITFindBy(accessibility = "setup_cycle_reminder_2")
    private WebElement eveningCard;

    @AndroidFindBy(accessibility = "setup_cycle_reminder_3")
    @iOSXCUITFindBy(accessibility = "setup_cycle_reminder_3")
    private WebElement nightCard;

    @Step("Tap 'In the morning' option")
    public void tapMorning() {
        log.info("Tapping 'In the morning' option");
        tap(morningCard);
    }

    @Step("Tap 'At noon' option")
    public void tapNoon() {
        log.info("Tapping 'At noon' option");
        tap(noonCard);
    }

    @Step("Tap 'In the evening' option")
    public void tapEvening() {
        log.info("Tapping 'In the evening' option");
        tap(eveningCard);
    }

    @Step("Tap 'At night' option")
    public void tapNight() {
        log.info("Tapping 'At night' option");
        tap(nightCard);
    }

    @Step("Verify all 4 training time options are visible")
    public boolean verifyAllOptionsVisible() {
        boolean m = isDisplayed(morningCard);
        boolean n = isDisplayed(noonCard);
        boolean e = isDisplayed(eveningCard);
        boolean ni = isDisplayed(nightCard);
        log.info("Training Time options: morning={}, noon={}, evening={}, night={}", m, n, e, ni);
        return m && n && e && ni;
    }

    @Step("Wait for Training Time screen to load")
    public void waitForScreen() {
        waitForVisible(morningCard);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("subtitle", getTextByPlatformId("nn.mobile.app.med:id/subtitle", "training_time_subtitle"));
        return content;
    }
}
