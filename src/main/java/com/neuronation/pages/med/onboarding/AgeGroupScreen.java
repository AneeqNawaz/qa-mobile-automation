package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Age Group Comparison screen — "Comparison"
 * Allows the user to select their age group for peer comparison.
 * Options: "Age group 18-20", "Age group 21-30", "Age group 31-40", etc.
 */
public class AgeGroupScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Comparison Group")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/content")
    @iOSXCUITFindBy(accessibility = "age_group_content")
    private WebElement description;

    @AndroidFindBy(id = "nn.mobile.app.med:id/popupContentHolder")
    @iOSXCUITFindBy(accessibility = "popupContentHolder")
    private WebElement popupContentHolder;

    @Step("Select age group: {ageGroupText}")
    public void selectAgeGroup(String ageGroupText) {
        log.info("Selecting age group: {}", ageGroupText);
        WebElement option;
        if (isAndroid()) {
            option = driver.findElement(
                    AppiumBy.androidUIAutomator("new UiSelector().text(\"" + ageGroupText + "\")"));
        } else {
            option = driver.findElement(AppiumBy.accessibilityId(ageGroupText));
        }
        option.click();
        // Popup takes 1–2s to close after the tap. Wait for popupContentHolder to go away
        // so the next action doesn't accidentally tap the row at the same coordinate.
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated(
                            platformLocator("nn.mobile.app.med:id/popupContentHolder", "popupContentHolder")));
        } catch (Exception ignored) {}
    }

    @Step("Verify Age Group screen is displayed")
    public boolean isDisplayed() {
        log.info("Checking if Age Group popup is displayed");
        return isDisplayed(popupContentHolder);
    }

    @Step("Wait for Age Group screen to load")
    public void waitForScreen() {
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
            .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                platformLocator("nn.mobile.app.med:id/popupContentHolder", "Comparison Group")));
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("description", getTextSafe(description));
        return content;
    }
}
