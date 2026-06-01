package com.neuronation.pages.med.registration;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * App Selection screen — "Are you here because of our medical product?"
 * User chooses between Medical app (DiGA) and Non-medical app paths.
 * Appears after tapping "Start now" on the Launch screen.
 */
public class AppSelectionScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Welcome")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/headline")
    @iOSXCUITFindBy(accessibility = "Are you here because of our medical product?")
    private WebElement headline;

    @AndroidFindBy(accessibility = "ic_app_selection_view_icon_med")
    @iOSXCUITFindBy(accessibility = "ic_app_selection_view_icon_med")
    private WebElement medicalAppCard;

    @AndroidFindBy(accessibility = "ic_app_selection_view_icon_default")
    @iOSXCUITFindBy(accessibility = "ic_app_selection_view_icon_default")
    private WebElement nonMedicalAppCard;

    @Step("Wait for App Selection screen to load")
    public void waitForScreen() {
        waitForVisible(headline);
    }

    @Step("Verify App Selection screen is displayed")
    public boolean isDisplayed() {
        return isDisplayed(headline);
    }

    @Step("Select Medical app on App Selection screen")
    public void selectMedicalApp() {
        log.info("Selecting Medical app");
        tap(medicalAppCard);
    }

    @Step("Select Non-medical app on App Selection screen")
    public void selectNonMedicalApp() {
        log.info("Selecting Non-medical app");
        tap(nonMedicalAppCard);
    }

    public String getHeadlineText() {
        return getText(headline);
    }

    public String getToolbarTitle() {
        return getText(toolbarTitle);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("headline", getTextSafe(headline));
        return content;
    }
}
