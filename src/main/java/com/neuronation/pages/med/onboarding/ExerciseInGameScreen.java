package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exercise In-Game screen — active during gameplay.
 * Pause button: nn.mobile.app.med:id/hudPauseButton
 * After pausing, debug menu appears with Succeed/Fail/Continue/Quit options.
 *
 * NOTE: Pause menu options use text-based locators because
 * the debug/dev menu does not have resource IDs. This is an accepted
 * exception to the "use IDs not text" rule. If the dev team adds IDs
 * to the pause menu, these should be updated.
 */
public class ExerciseInGameScreen extends BaseScreen {

    private static final String PAUSE_ID_ANDROID = "nn.mobile.app.med:id/hudPauseButton";
    private static final String PAUSE_ID_IOS = "hudPauseButton";

    private By pauseLocator() {
        return platformLocator(PAUSE_ID_ANDROID, PAUSE_ID_IOS);
    }

    private By succeedLocator() {
        if (isAndroid()) {
            return AppiumBy.androidUIAutomator("new UiSelector().text(\"Succeed exercise\")");
        } else {
            return AppiumBy.accessibilityId("Succeed exercise");
        }
    }

    private By continueLocator() {
        if (isAndroid()) {
            return AppiumBy.androidUIAutomator("new UiSelector().text(\"CONTINUE\")");
        } else {
            return AppiumBy.accessibilityId("CONTINUE");
        }
    }

    private By quitLocator() {
        if (isAndroid()) {
            return AppiumBy.androidUIAutomator("new UiSelector().text(\"QUIT\")");
        } else {
            return AppiumBy.accessibilityId("QUIT");
        }
    }

    @Step("Wait for Exercise In-Game screen to load")
    public void waitForScreen() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(pauseLocator()));
    }

    @Step("Tap pause button")
    public void tapPause() {
        log.info("Tapping pause button");
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.elementToBeClickable(pauseLocator())).click();
    }

    @Step("Pause and succeed exercise")
    public void pauseAndSucceed() {
        log.info("Pausing and succeeding exercise");

        // Tap pause — wait for menu with retry
        tapPause();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(succeedLocator())).click();
            log.info("Exercise succeeded");
            return;
        } catch (Exception e) {
            log.info("Pause menu not shown — retrying");
        }
        // Retry once
        tapPause();
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(succeedLocator())).click();
        log.info("Exercise succeeded on retry");
    }

    @Step("Tap 'Succeed exercise' in pause menu")
    public void tapSucceedExercise() {
        log.info("Tapping 'Succeed exercise' in pause menu");
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.presenceOfElementLocated(succeedLocator())).click();
    }

    @Step("Tap 'CONTINUE' in pause menu")
    public void tapContinueGame() {
        log.info("Tapping 'CONTINUE' in pause menu");
        driver.findElement(continueLocator()).click();
    }

    @Step("Tap 'QUIT' in pause menu")
    public void tapQuit() {
        log.info("Tapping 'QUIT' in pause menu");
        driver.findElement(quitLocator()).click();
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("screen", "ExerciseInGameScreen");
        return content;
    }
}
