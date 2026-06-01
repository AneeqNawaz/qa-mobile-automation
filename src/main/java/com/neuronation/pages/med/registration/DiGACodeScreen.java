package com.neuronation.pages.med.registration;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DiGA Activation Code screen — "Enter activation code"
 * Appears after selecting "Medical app" on the App Selection screen.
 * User enters their health insurance activation code or navigates to info website.
 */
public class DiGACodeScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Activation")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/redeem_description_title")
    @iOSXCUITFindBy(accessibility = "Enter activation code")
    private WebElement descriptionTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/redeem_description")
    @iOSXCUITFindBy(accessibility = "redeem_description")
    private WebElement descriptionText;

    @AndroidFindBy(id = "nn.mobile.app.med:id/redeemCode")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField")
    private WebElement codeInput;

    @AndroidFindBy(id = "nn.mobile.app.med:id/cardButton")
    @iOSXCUITFindBy(accessibility = "Activate")
    private WebElement activateButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/redeem_2_description_title")
    @iOSXCUITFindBy(accessibility = "redeem_2_description_title")
    private WebElement noCodeTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/websiteButton")
    @iOSXCUITFindBy(accessibility = "Open info website")
    private WebElement websiteButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/faqButton")
    @iOSXCUITFindBy(accessibility = "faqButton")
    private WebElement faqButton;

    @AndroidFindBy(accessibility = "Navigate up")
    @iOSXCUITFindBy(accessibility = "BackButton")
    private WebElement backButton;

    @Step("Wait for DiGA Code screen to load")
    public void waitForScreen() {
        waitForVisible(descriptionTitle);
    }

    @Step("Verify Activation screen is displayed")
    public boolean isActivationScreenDisplayed() {
        return isDisplayed(descriptionTitle);
    }

    @Step("Enter activation code: {code}")
    public void enterCode(String code) {
        log.info("Entering activation code");
        type(codeInput, code);
    }

    @Step("Tap Activate button")
    public void tapActivate() {
        log.info("Tapping Activate");
        tap(activateButton);
    }

    @Step("Enter code and activate: {code}")
    public void enterCodeAndActivate(String code) {
        enterCode(code);
        if (isIOS()) {
            // Dismiss keyboard so Activate button is accessible
            hideKeyboardSafe();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        tapActivate();
    }

    @Step("Tap Open info website")
    public void tapWebsite() {
        log.info("Tapping Open info website");
        tap(websiteButton);
    }

    @Step("Tap FAQ button")
    public void tapFaq() {
        log.info("Tapping FAQ");
        tap(faqButton);
    }

    @Step("Tap back button on Activation screen")
    public void tapBack() {
        log.info("Tapping Back");
        tap(backButton);
    }

    public String getToolbarTitle() {
        return getText(toolbarTitle);
    }

    public String getDescriptionTitle() {
        return getText(descriptionTitle);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("descriptionTitle", getTextSafe(descriptionTitle));
        content.put("descriptionText", getTextSafe(descriptionText));
        content.put("activateButton", getTextSafe(activateButton));
        content.put("noCodeTitle", getTextSafe(noCodeTitle));
        content.put("websiteButton", getTextSafe(websiteButton));
        content.put("faqButton", getTextSafe(faqButton));
        return content;
    }

    @io.qameta.allure.Step("Enter invalid code and verify error: {code}")
    public String enterInvalidCodeAndGetError(String code) {
        enterCode(code);
        tapActivate();
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                .until(d -> isAlertPresent());
            String error = getAlertMessage();
            dismissAlert();
            return error;
        } catch (Exception e) { return null; }
    }

}
