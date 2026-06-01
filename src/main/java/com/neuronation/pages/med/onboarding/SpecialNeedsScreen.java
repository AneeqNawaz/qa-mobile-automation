package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Special Needs screen — "Personalise schedule" / "Special needs in training"
 * Allows the user to select accessibility constraints:
 * "Standard", "Color Vision Deficiency", "Arithmetic Impairment", "Both constraints".
 */
public class SpecialNeedsScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Personalise schedule")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/subtitle")
    @iOSXCUITFindBy(accessibility = "Special needs in training")
    private WebElement subtitle;

    @AndroidFindBy(accessibility = "setup_cycle_accessiblety_0")
    @iOSXCUITFindBy(accessibility = "setup_cycle_accessiblety_0")
    private WebElement standardCard;

    @AndroidFindBy(accessibility = "setup_cycle_accessiblety_1")
    @iOSXCUITFindBy(accessibility = "setup_cycle_accessiblety_1")
    private WebElement colorVisionCard;

    @AndroidFindBy(accessibility = "setup_cycle_accessiblety_2")
    @iOSXCUITFindBy(accessibility = "setup_cycle_accessiblety_2")
    private WebElement arithmeticCard;

    @AndroidFindBy(accessibility = "setup_cycle_accessiblety_3")
    @iOSXCUITFindBy(accessibility = "setup_cycle_accessiblety_3")
    private WebElement bothCard;

    @Step("Tap 'Standard' option on Special Needs screen")
    public void tapStandard() {
        log.info("Tapping 'Standard' option on Special Needs screen");
        tap(standardCard);
    }

    @Step("Tap 'Color Vision Deficiency' option on Special Needs screen")
    public void tapColorVision() {
        log.info("Tapping 'Color Vision Deficiency' option on Special Needs screen");
        tap(colorVisionCard);
    }

    @Step("Tap 'Arithmetic Impairment' option on Special Needs screen")
    public void tapArithmetic() {
        log.info("Tapping 'Arithmetic Impairment' option on Special Needs screen");
        tap(arithmeticCard);
    }

    @Step("Tap 'Both constraints' option on Special Needs screen")
    public void tapBoth() {
        log.info("Tapping 'Both constraints' option on Special Needs screen");
        tap(bothCard);
    }

    @Step("Verify all 4 special needs options are visible")
    public boolean verifyAllOptionsVisible() {
        boolean std = isDisplayed(standardCard);
        boolean cv = isDisplayed(colorVisionCard);
        boolean arith = isDisplayed(arithmeticCard);
        boolean both = isDisplayed(bothCard);
        log.info("Special Needs options: standard={}, colorVision={}, arithmetic={}, both={}", std, cv, arith, both);
        return std && cv && arith && both;
    }

    @Step("Wait for Special Needs screen to load")
    public void waitForScreen() {
        waitForVisible(standardCard);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("subtitle", getTextByPlatformId("nn.mobile.app.med:id/subtitle", "Special needs in training"));
        return content;
    }
}
