package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exercise Intro screen — shown before each exercise begins.
 * Toolbar title varies by exercise type ("Attention", "Memory", etc.).
 * Contains an exercise description and a "Start" button.
 */
public class ExerciseIntroScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/textExerciseName")
    @iOSXCUITFindBy(accessibility = "Attention")
    private WebElement exerciseName;

    @AndroidFindBy(id = "nn.mobile.app.med:id/textExerciseDescription")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[2]")
    private WebElement exerciseDescription;

    @AndroidFindBy(id = "nn.mobile.app.med:id/buttonStartExercise")
    @iOSXCUITFindBy(accessibility = "buttonStartExercise")
    private WebElement startButton;

    @Step("Tap 'Start' on Exercise Intro screen")
    public void tapStart() {
        log.info("Tapping 'Start' on Exercise Intro screen");
        tap(startButton);
    }

    @Step("Get exercise name from Exercise Intro screen")
    public String getTitle() {
        log.info("Getting exercise name from Exercise Intro screen");
        return getText(exerciseName);
    }

    @Step("Wait for Exercise Intro screen to load")
    public void waitForScreen() {
        waitForVisible(startButton);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> captured = new LinkedHashMap<>();
        captured.put("exerciseName", getTextSafe(exerciseName));
        captured.put("exerciseDescription", getTextSafe(exerciseDescription));
        return captured;
    }
}
