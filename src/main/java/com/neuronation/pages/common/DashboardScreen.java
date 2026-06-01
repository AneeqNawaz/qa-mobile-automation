package com.neuronation.pages.common;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dashboard screen — main hub after onboarding.
 * Shows level, session progress, story content, and bottom navigation tabs.
 */
public class DashboardScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/brain_level")
    @iOSXCUITFindBy(accessibility = "dashboard_level_headline")
    private WebElement levelHeadline;

    @AndroidFindBy(id = "nn.mobile.app.med:id/session_progress")
    @iOSXCUITFindBy(accessibility = "dashboard_session_progress")
    private WebElement sessionProgress;

    @AndroidFindBy(id = "nn.mobile.app.med:id/content")
    @iOSXCUITFindBy(accessibility = "dashboard_content")
    private WebElement storyContent;

    @Step("Wait for Dashboard screen to load")
    public void waitForScreen() {
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(60))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                        platformLocator("nn.mobile.app.med:id/navigation_profile", "Profile")));
    }

    @Step("Check if Dashboard screen is displayed")
    public boolean isDashboardDisplayed() {
        log.info("Checking if Dashboard screen is displayed");
        return !findAllByPlatformId("nn.mobile.app.med:id/navigation_profile", "Profile").isEmpty();
    }

    @Step("Tap 'Start' on Dashboard screen")
    public void tapStart() {
        log.info("Tapping 'Start' on Dashboard screen");
        findByPlatformId("nn.mobile.app.med:id/continue_button", "Start").click();
    }

    @Step("Tap 'Training' tab on Dashboard")
    public void tapTrainingTab() {
        log.info("Tapping 'Training' tab on Dashboard");
        findByPlatformId("nn.mobile.app.med:id/dashboard", "Training").click();
    }

    @Step("Tap 'Extras' tab on Dashboard")
    public void tapExtrasTab() {
        log.info("Tapping 'Extras' tab on Dashboard");
        findByPlatformId("nn.mobile.app.med:id/teasers", "Extras").click();
    }

    @Step("Tap 'Evaluation' tab on Dashboard")
    public void tapEvaluationTab() {
        log.info("Tapping 'Evaluation' tab on Dashboard");
        findByPlatformId("nn.mobile.app.med:id/evaluation", "Evaluation").click();
    }

    @Step("Tap 'Profile' tab on Dashboard")
    public void tapProfileTab() {
        log.info("Tapping 'Profile' tab on Dashboard");
        findByPlatformId("nn.mobile.app.med:id/navigation_profile", "Profile").click();
    }

    @Step("Get level headline text from Dashboard")
    public String getLevel() {
        log.info("Getting level headline from Dashboard");
        return getText(levelHeadline);
    }

    @Step("Get session progress text from Dashboard")
    public String getSessionProgress() {
        log.info("Getting session progress from Dashboard");
        return getText(sessionProgress);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("levelHeadline", getTextSafe(levelHeadline));
        content.put("sessionProgress", getTextSafe(sessionProgress));
        // storyContent excluded — dynamic/randomized content changes each session
        return content;
    }
}
