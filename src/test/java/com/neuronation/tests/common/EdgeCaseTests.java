package com.neuronation.tests.common;

import com.neuronation.base.BaseTest;
import com.neuronation.driver.DriverManager;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;

@Epic("NeuroNation MED App")
@Feature("Edge Cases")
public class EdgeCaseTests extends BaseTest {

    @Test(
        description = "App handles back navigation from App Selection without crash",
        groups = {Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Back navigation does not crash the app")
    @Description("Navigates to App Selection and presses the device back button. "
            + "Verifies the app returns to the launch screen without crashing.")
    public void testBackNavigationFromAppSelection() {
        assertTrue(screens.launch().isStartNowDisplayed(), "Launch screen should be visible");
        screens.launch().tapStartNow();

        // Press device back button
        DriverManager.getDriver().navigate().back();

        // Should return to launch screen without crash
        assertTrue(screens.launch().isStartNowDisplayed(),
                "Launch screen should be visible after back navigation");
    }

    @Test(
        description = "App launch screen is correctly displayed on cold start",
        groups = {Features.SMOKE, Features.REGRESSION}
    )
    @Severity(SeverityLevel.BLOCKER)
    @Story("App cold start shows launch screen")
    @Description("Verifies the app launches successfully and displays the "
            + "launch screen with Start Now and Login buttons visible.")
    public void testAppColdStart() {
        // Verify Start Now button is visible
        assertTrue(screens.launch().isStartNowDisplayed(),
                "Start Now button should be visible on app launch");

        // Verify screen text content
        String title = screens.launch().getTitle();
        assertEquals(title, "Fitness For Your Brain", "Title text should match");
        log.info("Launch screen title: {}", title);

        String language = screens.launch().getLanguage();
        assertNotNull(language, "Language dropdown should have text");
        log.info("Launch screen language: {}", language);

        // Record/verify content snapshot
        verifyOrRecordContent(screens.launch(), "LaunchScreen");

        // Tap Start Now and verify navigation works
        screens.launch().tapStartNow();
        assertTrue(screens.appSelection().isDisplayed(),
                "App Selection screen should be visible after tapping Start Now");

        softAssert.assertAll();
    }
}
