package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Schedule Review screen — "Personalise schedule" / "Review your settings"
 * Shows the selected schedule with weekday labels.
 * The "Confirm" button may require scrolling to be visible.
 */
public class ScheduleReviewScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Personalise schedule")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/subtitle")
    @iOSXCUITFindBy(accessibility = "Review your settings")
    private WebElement subtitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/enable_switch")
    @iOSXCUITFindBy(accessibility = "enable_switch")
    private WebElement trainingReminderToggle;

    @Step("Tap 'Confirm' on Schedule Review screen")
    public void tapConfirm() {
        log.info("Swiping up and tapping 'Confirm' on Schedule Review screen");
        swipeUp();
        WebElement confirmButton = findByPlatformId("nn.mobile.app.med:id/confirm_button", "confirm_button");
        confirmButton.click();
    }

    @Step("Toggle training reminder switch on Schedule Review screen")
    public void tapTrainingReminderToggle() {
        log.info("Tapping training reminder toggle on Schedule Review screen");
        tap(trainingReminderToggle);
    }

    @Step("Wait for Schedule Review screen to load")
    public void waitForScreen() {
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
            .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                platformLocator("nn.mobile.app.med:id/editListView", "Review your settings")));
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("subtitle", getTextByPlatformId("nn.mobile.app.med:id/subtitle", "Review your settings"));
        return content;
    }
}
