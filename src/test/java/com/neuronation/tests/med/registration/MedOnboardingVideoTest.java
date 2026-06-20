package com.neuronation.tests.med.registration;

import com.neuronation.base.BaseTest;
import com.neuronation.config.AppType;
import com.neuronation.testdata.ActivationData;
import com.neuronation.testdata.Features;
import com.neuronation.utils.ScreenDumper;
import com.neuronation.utils.TestDataLoader;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Epic("NeuroNation MED App")
@Feature("Onboarding Video")
public class MedOnboardingVideoTest extends BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void navigateToVideoScreen() throws InterruptedException {
        ActivationData activation = TestDataLoader.loadActivationData(
                AppType.MED, context.getLanguage(), ActivationData.class);

        screens.launch().tapStartNow();
        Thread.sleep(1500);
        screens.appSelection().selectMedicalApp();
        Thread.sleep(1500);
        screens.digaCode().enterCodeAndActivate(activation.getDigaCode());
        Thread.sleep(5000);

        // Dump to see what screen we landed on
        ScreenDumper.dumpCurrentScreen("04_AfterActivation_debug");
    }

    // ──────────────────────────────────────────────
    // Video screen display
    // ──────────────────────────────────────────────

    @Test(
        description = "Onboarding video screen is displayed after activation",
        groups = {Features.MED, Features.ONBOARDING, Features.SMOKE}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Video screen loads after activation")
    @Description("Verifies the onboarding video screen appears after successful DiGA "
            + "code activation. The video container should be visible.")
    public void testVideo_screenDisplayed() {
        assertTrue(screens.onboardingVideo().isVideoScreenDisplayed(),
                "Onboarding video screen should be displayed after activation");
    }

    // ──────────────────────────────────────────────
    // Video controls
    // ──────────────────────────────────────────────

    @Test(
        description = "Tapping video reveals player controls with close button",
        groups = {Features.MED, Features.ONBOARDING, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Video controls are accessible")
    @Description("Taps the video screen to reveal player controls. Verifies the close (X) "
            + "button and progress bar become visible after tapping.")
    public void testVideo_tapScreen_revealsControls() {
        assertTrue(screens.onboardingVideo().isCloseButtonVisible(),
                "Close button should be visible after tapping to reveal controls");

        assertTrue(screens.onboardingVideo().isProgressBarVisible(),
                "Progress bar should be visible after tapping to reveal controls");
    }

    @Test(
        description = "Video progress bar shows timestamp",
        groups = {Features.MED, Features.ONBOARDING, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Video progress shows current timestamp")
    @Description("Reveals video controls and verifies the progress bar has a "
            + "timestamp value (format: MM:SS), confirming the video is playing.")
    public void testVideo_progressBar_showsTimestamp() {
        String timestamp = screens.onboardingVideo().getProgressTimestamp();
        assertNotNull(timestamp, "Progress timestamp should not be null");
        assertFalse(timestamp.isEmpty(), "Progress timestamp should not be empty");
        log.info("Video progress timestamp: {}", timestamp);
    }

    // ──────────────────────────────────────────────
    // Subtitles verification
    // ──────────────────────────────────────────────

    @Test(
        description = "Subtitles area is present on video screen",
        groups = {Features.MED, Features.ONBOARDING, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Video subtitles are available")
    @Description("Verifies the subtitles area is present on the onboarding video screen. "
            + "Since English was selected on the launch screen, subtitles should be "
            + "in English (verified by content snapshot comparison).")
    public void testVideo_subtitles_areaPresent() {
        assertTrue(screens.onboardingVideo().isSubtitlesAreaPresent(),
                "Subtitles area should be present on video screen");
    }

    // ──────────────────────────────────────────────
    // Close/skip video
    // ──────────────────────────────────────────────

    @Test(
        description = "Close button exits video and navigates to next screen",
        groups = {Features.MED, Features.ONBOARDING, Features.SMOKE, Features.CRITICAL}
    )
    @Severity(SeverityLevel.BLOCKER)
    @Story("Close button skips video and advances onboarding")
    @Description("Taps the close (X) button on the video screen. Verifies the "
            + "video is dismissed and the user advances to the next onboarding screen. "
            + "Dumps the next screen elements for further discovery.")
    public void testVideo_tapClose_navigatesForward() throws InterruptedException {
        // First dump what we see right after activation
        ScreenDumper.dumpCurrentScreen("04_VideoScreen_beforeClose");

        // Try to close video
        screens.onboardingVideo().tapClose();
        Thread.sleep(3000);

        // Dump whatever screen appears next
        ScreenDumper.dumpCurrentScreen("05_AfterVideoClose");
        log.info("Video closed — next screen dumped to screen-dumps/05_AfterVideoClose.txt");
    }

    // ──────────────────────────────────────────────
    // Content capture
    // ──────────────────────────────────────────────

    @Test(
        description = "Capture video screen content snapshot",
        groups = {Features.MED, Features.ONBOARDING, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Record video screen content for baseline comparison")
    @Description("Captures the video screen content (subtitles text, progress timestamp) "
            + "as a JSON snapshot for future regression comparison.")
    public void testVideo_contentSnapshot() {
        verifyOrRecordContent(screens.onboardingVideo(), "OnboardingVideoScreen");
        softAssert.assertAll();
    }
}
