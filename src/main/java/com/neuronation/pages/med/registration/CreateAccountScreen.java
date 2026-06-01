package com.neuronation.pages.med.registration;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Create Account screen — appears after onboarding video.
 * Two registration options: Email or Gesundheits-ID (Health ID).
 */
public class CreateAccountScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Create account")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/LoginMailButtonSimple")
    @iOSXCUITFindBy(accessibility = "LoginMailButtonSimple")
    private WebElement registerViaEmailButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/LoginHealthIdButton")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeButton[`label == 'Via Gesundheits-ID'`]")
    private WebElement healthIdButton;

    @AndroidFindBy(accessibility = "Navigate up")
    @iOSXCUITFindBy(accessibility = "BackButton")
    private WebElement backButton;

    @Step("Wait for Create Account screen to load")
    public void waitForScreen() {
        waitForVisible(registerViaEmailButton);
    }

    @Step("Verify Create Account screen is displayed")
    public boolean isDisplayed() {
        return isDisplayed(registerViaEmailButton);
    }

    @Step("Tap 'Register via Email' on Create Account screen")
    public void tapRegisterViaEmail() {
        log.info("Tapping Register via Email");
        tap(registerViaEmailButton);
    }

    @Step("Tap 'Via Gesundheits-ID' on Create Account screen")
    public void tapHealthId() {
        log.info("Tapping Via Gesundheits-ID");
        tap(healthIdButton);
    }

    @Step("Tap back button on Create Account screen")
    public void tapBack() {
        log.info("Tapping Back");
        tap(backButton);
    }

    public String getToolbarTitle() {
        return getText(toolbarTitle);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("registerViaEmailButton", getTextSafe(registerViaEmailButton));
        content.put("healthIdButton", getTextSafe(healthIdButton));
        return content;
    }
}
