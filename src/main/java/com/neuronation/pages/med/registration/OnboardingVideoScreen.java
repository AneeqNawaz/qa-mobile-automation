package com.neuronation.pages.med.registration;

import com.neuronation.base.BaseScreen;
import com.neuronation.driver.DriverManager;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Onboarding Video screen — plays after successful DiGA activation.
 * Has video player with subtitles, progress bar, and a close (X) button.
 * Tap the screen to reveal player controls.
 */
public class OnboardingVideoScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/onboardingContainer")
    @iOSXCUITFindBy(accessibility = "CloseButtonBlack")
    private WebElement onboardingContainer;

    @AndroidFindBy(id = "nn.mobile.app.med:id/exoplayervideoview")
    @iOSXCUITFindBy(accessibility = "video_player")
    private WebElement videoPlayer;

    @AndroidFindBy(id = "nn.mobile.app.med:id/exo_subtitles")
    @iOSXCUITFindBy(accessibility = "subtitles")
    private WebElement subtitles;

    @AndroidFindBy(id = "nn.mobile.app.med:id/exo_controller")
    @iOSXCUITFindBy(accessibility = "video_controller")
    private WebElement videoController;

    @AndroidFindBy(id = "nn.mobile.app.med:id/exo_progress")
    @iOSXCUITFindBy(accessibility = "video_progress")
    private WebElement progressBar;

    @AndroidFindBy(id = "nn.mobile.app.med:id/closeVideoButton")
    @iOSXCUITFindBy(accessibility = "CloseButtonBlack")
    private WebElement closeButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/videoCover")
    @iOSXCUITFindBy(accessibility = "video_cover")
    private WebElement videoCover;

    @Step("Wait for Onboarding Video screen to load")
    public void waitForScreen() {
        waitForVisible(onboardingContainer);
    }

    @Step("Verify onboarding video screen is displayed")
    public boolean isVideoScreenDisplayed() {
        return isDisplayed(onboardingContainer);
    }

    @Step("Tap screen to reveal video controls")
    public void revealControls() {
        log.info("Tapping screen to reveal video controls");
        if (isIOS()) {
            // Reveal the AVPlayer controls with a REAL TOUCH tap in a neutral UPPER area — the SAME
            // approach the cognitive video's revealControlsIOS uses (and which works there). The old
            // `driver.findElement("Video").click()` is an ACCESSIBILITY-synthesised tap that AVPlayer
            // IGNORES on BrowserStack: the controls never appear, so "Skip Forward" is never found and
            // the skip loop bails while the video keeps playing (the iOS "failed to reveal → video not
            // skipped" bug). Upper 0.22h avoids the centre Play/Pause and the top-right CloseButtonBlack.
            var dimensions = driver.manage().window().getSize();
            tapAt(dimensions.getWidth() / 2, (int) (dimensions.getHeight() * 0.22));
        } else {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence tap = new Sequence(finger, 0);
            tap.addAction(finger.createPointerMove(Duration.ZERO,
                    PointerInput.Origin.viewport(), 540, 1170));
            tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            DriverManager.getDriver().perform(Collections.singletonList(tap));
        }
    }

    @Step("Tap close (X) button to exit video")
    public void tapClose() {
        log.info("Tapping close button to exit video");
        if (isAndroid()) {
            revealControls();
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(
                            io.appium.java_client.AppiumBy.id("nn.mobile.app.med:id/closeVideoButton"))).click();
        } else {
            // iOS: try the close X. It works locally, but in CI (BrowserStack) the X tap frequently
            // does NOT dismiss the player — the app stays on the video and the next screen never
            // loads. So verify we actually left the video and, if we're still on it, fast-forward to
            // the end (the explanatory video auto-transitions to Create Account when it finishes).
            try {
                new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                        .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(
                                io.appium.java_client.AppiumBy.accessibilityId("CloseButtonBlack"))).click();
                log.info("Tapped iOS video close X");
            } catch (Exception e) {
                log.info("iOS video close X not clickable ({}) — will fast-forward", e.getMessage());
            }
            if (!iosLeftVideo(6) && iosOnVideo()) {
                log.info("iOS still on video after close tap — fast-forwarding to end");
                skipVideoViaForward();
            }
        }
    }

    /** iOS: wait up to {@code sec}s for the Create Account screen (its "register via email" button,
     *  accessibilityId {@code LoginMailButtonSimple}) — i.e. we successfully left the video. */
    private boolean iosLeftVideo(int sec) {
        try {
            return new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(sec))
                    .until(d -> !d.findElements(
                            io.appium.java_client.AppiumBy.accessibilityId("LoginMailButtonSimple")).isEmpty());
        } catch (Exception e) {
            return false;
        }
    }

    /** iOS: true while the video player is still on screen (player / its close control present). */
    private boolean iosOnVideo() {
        return !driver.findElements(io.appium.java_client.AppiumBy.accessibilityId("Video")).isEmpty()
                || !driver.findElements(io.appium.java_client.AppiumBy.accessibilityId("video_player")).isEmpty()
                || !driver.findElements(io.appium.java_client.AppiumBy.accessibilityId("CloseButtonBlack")).isEmpty();
    }

    /**
     * Skip inter-exercise video by fast-forwarding.
     * iOS: close button is disabled during inter-exercise videos — must fast-forward.
     * TODO: iOS mismatch — close button disabled unlike Android. Reported to team.
     * Video auto-transitions to next screen when it ends.
     */
    @Step("Skip inter-exercise video via fast-forward")
    public void skipVideoViaForward() {
        log.info("Skipping video via fast-forward");
        fastForward();
        // Video auto-transitions when it ends — try close if still on video screen
        try {
            var closeBtn = driver.findElements(
                    io.appium.java_client.AppiumBy.accessibilityId("CloseButtonBlack"));
            if (!closeBtn.isEmpty() && closeBtn.get(0).isEnabled()) {
                closeBtn.get(0).click();
            }
        } catch (Exception ignored) {}
    }

    /**
     * Fast-forward video to the end.
     * iOS: tap video to reveal controls, then tap "Skip Forward" (10s) multiple times.
     * Android: uses progress bar seek.
     */
    @Step("Fast-forward video to end")
    public void fastForward() {
        log.info("Fast-forwarding video");
        if (isIOS()) {
            int taps = getRequiredSkipTaps(10);
            iosSkipForward(taps);
        } else {
            var dimensions = driver.manage().window().getSize();
            int endX = (int) (dimensions.getWidth() * 0.95);
            int centerY = dimensions.getHeight() / 2;
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence tap = new Sequence(finger, 0);
            tap.addAction(finger.createPointerMove(Duration.ZERO,
                    PointerInput.Origin.viewport(), endX, centerY));
            tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(tap));
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        }
    }

    /**
     * Get video duration in seconds from the "Remaining Time" element on iOS.
     * Reveals controls if needed. Returns 0 if unable to determine.
     */
    @Step("Get video duration in seconds")
    public int getVideoDuration() {
        if (isIOS()) {
            // Controls should already be visible from ensurePlaying or prior interaction
            try {
                var remaining = driver.findElement(
                        io.appium.java_client.AppiumBy.accessibilityId("Remaining Time"));
                var elapsed = driver.findElement(
                        io.appium.java_client.AppiumBy.accessibilityId("Elapsed Time"));
                int remainSec = parseTimeLabel(remaining.getText());
                int elapsedSec = parseTimeLabel(elapsed.getText());
                int total = remainSec + elapsedSec;
                log.info("Video duration: {}s (elapsed: {}s, remaining: {}s)", total, elapsedSec, remainSec);
                return total;
            } catch (Exception e) {
                log.warn("Could not get video duration: {}", e.getMessage());
                return 0;
            }
        }
        return 0;
    }

    /**
     * Parse time label like "-0:28" or "0:05" to seconds.
     */
    private int parseTimeLabel(String timeText) {
        try {
            String cleaned = timeText.replace("-", "").trim();
            String[] parts = cleaned.split(":");
            if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    /**
     * Calculate required skip taps based on video duration and skip interval.
     * @param skipSeconds seconds per skip tap (default 10)
     * @return number of taps needed, minimum 1
     */
    public int getRequiredSkipTaps(int skipSeconds) {
        int duration = getVideoDuration();
        if (duration <= 0) return 3; // fallback
        int taps = (int) Math.ceil((double) duration / skipSeconds);
        log.info("Video {}s / {}s skip = {} taps needed", duration, skipSeconds, taps);
        return taps;
    }

    /**
     * iOS: skip forward by tapping "Skip Forward" button N times.
     * Ensures video is playing first (skip only works during playback).
     */
    @Step("iOS: Skip forward {taps} times")
    public void iosSkipForward(int taps) {
        log.info("iOS: skipping forward {} times", taps);
        // 1. Tap video to reveal controls (this pauses the video)
        revealControls();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        // 2. Resume playback — skip only works during playback
        ensurePlaying();
        var forwardLocator = io.appium.java_client.AppiumBy.accessibilityId("Skip Forward");
        for (int i = 0; i < taps; i++) {
            try {
                driver.findElement(forwardLocator).click();
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            } catch (Exception e) {
                // Controls hidden or video ended — stop skipping
                log.info("Skip Forward not found at tap {} — video may have ended", i);
                break;
            }
        }
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
    }

    /**
     * iOS: skip backward by tapping "Skip Backward" button N times.
     */
    @Step("iOS: Skip backward {taps} times")
    public void iosSkipBackward(int taps) {
        log.info("iOS: skipping backward {} times", taps);
        revealControls();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        ensurePlaying();
        var backLocator = io.appium.java_client.AppiumBy.accessibilityId("Skip Backward");
        for (int i = 0; i < taps; i++) {
            try {
                driver.findElement(backLocator).click();
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            } catch (Exception e) {
                revealControls();
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                try { driver.findElement(backLocator).click(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Ensure video is playing (not paused). If paused, tap Play/Pause to resume.
     */
    private void ensurePlaying() {
        try {
            var playPause = driver.findElements(
                    io.appium.java_client.AppiumBy.accessibilityId("Play/Pause"));
            if (!playPause.isEmpty() && "Play".equals(playPause.get(0).getAttribute("label"))) {
                playPause.get(0).click();
                log.info("Resumed video playback");
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        } catch (Exception ignored) {}
    }

    /**
     * Rewind video to the start.
     * iOS: tap "Skip Backward" (10s) multiple times.
     */
    @Step("Rewind video to start")
    public void rewind() {
        log.info("Rewinding video");
        if (isIOS()) {
            int taps = getRequiredSkipTaps(10);
            iosSkipBackward(taps);
        } else {
            var dimensions = driver.manage().window().getSize();
            int startX = (int) (dimensions.getWidth() * 0.05);
            int centerY = dimensions.getHeight() / 2;
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence tap = new Sequence(finger, 0);
            tap.addAction(finger.createPointerMove(Duration.ZERO,
                    PointerInput.Origin.viewport(), startX, centerY));
            tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Collections.singletonList(tap));
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
    }

    @Step("Verify video progress bar is visible")
    public boolean isProgressBarVisible() {
        revealControls();
        try {
            return new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(3))
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                            platformLocator("nn.mobile.app.med:id/exo_progress", "video_progress"))).isDisplayed();
        } catch (Exception e) { return false; }
    }

    @Step("Get video progress timestamp")
    public String getProgressTimestamp() {
        revealControls();
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        return driver.findElement(
                platformLocator("nn.mobile.app.med:id/exo_progress", "video_progress"))
                .getAttribute("content-desc");
    }

    @Step("Verify subtitles area is present")
    public boolean isSubtitlesAreaPresent() {
        return isDisplayed(subtitles);
    }

    @Step("Get subtitles text")
    public String getSubtitlesText() {
        return getTextSafe(subtitles);
    }

    @Step("Verify close button is visible after revealing controls")
    public boolean isCloseButtonVisible() {
        revealControls();
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        try {
            return driver.findElement(
                    platformLocator("nn.mobile.app.med:id/closeVideoButton", "CloseButtonBlack")).isDisplayed();
        } catch (Exception e) { return false; }
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("subtitles", getTextSafe(subtitles));
        // Reveal controls to capture progress
        revealControls();
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        String progress = "";
        try { progress = progressBar.getAttribute("content-desc"); } catch (Exception ignored) {}
        content.put("progressTimestamp", progress);
        return content;
    }

    @io.qameta.allure.Step("Verify video controls are functional")
    public boolean verifyVideoControls() {
        revealControls();
        boolean hasProgress = isProgressBarVisible();
        boolean hasClose = isCloseButtonVisible();
        log.info("Video controls: progress={}, close={}", hasProgress, hasClose);
        return hasProgress || hasClose;
    }

}
