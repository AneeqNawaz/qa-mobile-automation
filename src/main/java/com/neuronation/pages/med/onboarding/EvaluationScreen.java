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
 * Evaluation screen — "Evaluation"
 * Shows performance results: area for improvement, performance profile
 * (Memory %, Attention %, Reasoning %, Speed %), and peer group comparison.
 * Contains a "Create Personalised Training Plan" button.
 */
public class EvaluationScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Evaluation")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/content")
    @iOSXCUITFindBy(accessibility = "evaluation_content")
    private WebElement content;

    @AndroidFindBy(id = "nn.mobile.app.med:id/subtitle")
    @iOSXCUITFindBy(accessibility = "evaluation_subtitle")
    private WebElement subtitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/storyHolder")
    @iOSXCUITFindBy(accessibility = "evaluation_story_holder")
    private WebElement storyHolder;

    @AndroidFindBy(id = "nn.mobile.app.med:id/assessmentTopWeaknessTitle")
    @iOSXCUITFindBy(accessibility = "evaluation_weakness_title")
    private WebElement weaknessTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/weakness_banner")
    @iOSXCUITFindBy(accessibility = "evaluation_weakness_banner")
    private WebElement weaknessBanner;

    @Step("Tap 'Create Personalised Training Plan' on Evaluation screen")
    public void tapCreatePlan() {
        log.info("Tapping 'Create Personalised Training Plan' on Evaluation screen");
        findByPlatformId("nn.mobile.app.med:id/continueAssessmentButton", "continueAssessmentButton").click();
    }

    @Step("Get weakness title from Evaluation screen")
    public String getWeaknessTitle() {
        log.info("Getting weakness title from Evaluation screen");
        return getText(weaknessTitle);
    }

    @Step("Get performance text from Evaluation screen")
    public String getPerformanceText() {
        log.info("Getting performance text from Evaluation screen");
        return getText(content);
    }

    @Step("Wait for Evaluation screen to load")
    public void waitForScreen() {
        if (isIOS()) {
            // iOS: wait for the "Create Personalised Training Plan" button
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                    io.appium.java_client.AppiumBy.accessibilityId("continueAssessmentButton")));
        } else {
            waitForVisible(weaknessBanner);
        }
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> captured = new LinkedHashMap<>();
        captured.put("toolbarTitle", getTextSafe(toolbarTitle));
        captured.put("subtitle", getTextByPlatformId("nn.mobile.app.med:id/subtitle", "evaluation_subtitle"));
        captured.put("content", getTextByPlatformId("nn.mobile.app.med:id/content", "evaluation_content"));
        captured.put("weaknessTitle", getTextSafe(weaknessTitle));
        return captured;
    }
}
