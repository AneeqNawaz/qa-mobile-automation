package com.neuronation.pages.common;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

public class LaunchScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/headline")
    @iOSXCUITFindBy(accessibility = "Fitness For Your Brain")
    private WebElement titleText;

    @AndroidFindBy(id = "nn.mobile.app.med:id/newUserButton")
    @iOSXCUITFindBy(accessibility = "newUserButton")
    private WebElement startNowButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/returningUserButton")
    @iOSXCUITFindBy(accessibility = "returningUserButton")
    private WebElement continueTrainingButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/languageText")
    @iOSXCUITFindBy(accessibility = "English (United Kingdom)")
    private WebElement languageDropdown;

    @AndroidFindBy(id = "nn.mobile.app.med:id/arrowIcon")
    @iOSXCUITFindBy(accessibility = "back")
    private WebElement languageArrow;

    @Step("Wait for Launch screen to load")
    public void waitForScreen() {
        waitForVisible(startNowButton);
    }

    @Step("Tap Start Now button on Launch screen")
    public void tapStartNow() {
        log.info("Tapping Start Now");
        tap(startNowButton);
    }

    @Step("Tap Continue Training button on Launch screen")
    public void tapContinueTraining() {
        log.info("Tapping Continue Training");
        tap(continueTrainingButton);
    }

    @Step("Check if Start Now button is displayed on Launch screen")
    public boolean isStartNowDisplayed() {
        return isDisplayed(startNowButton);
    }

    public String getTitle() {
        return getText(titleText);
    }

    public String getLanguage() {
        return getText(languageDropdown);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("titleText", getTextSafe(titleText));
        content.put("startNowButton", getTextSafe(startNowButton));
        content.put("continueTrainingButton", getTextSafe(continueTrainingButton));
        content.put("languageDropdown", getTextSafe(languageDropdown));
        return content;
    }

    @io.qameta.allure.Step("Verify all launch screen elements are visible")
    public boolean verifyAllElementsVisible() {
        boolean title = isDisplayed(titleText);
        boolean startNow = isDisplayed(startNowButton);
        boolean cont = isDisplayed(continueTrainingButton);
        boolean lang = isDisplayed(languageDropdown);
        log.info("Launch: title={}, startNow={}, continue={}, language={}", title, startNow, cont, lang);
        return title && startNow && cont && lang;
    }

}
