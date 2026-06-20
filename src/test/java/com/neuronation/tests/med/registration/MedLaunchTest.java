package com.neuronation.tests.med.registration;

import com.neuronation.base.BaseTest;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Epic("NeuroNation MED App")
@Feature("Launch Screen")
public class MedLaunchTest extends BaseTest {

    @Test(
        description = "Launch screen displays all expected elements on cold start",
        groups = {Features.MED, Features.SMOKE, Features.CRITICAL}
    )
    @Severity(SeverityLevel.BLOCKER)
    @Story("App cold start shows correct launch screen")
    @Description("Verifies the MED app launches and displays: title 'Fitness For Your Brain', "
            + "'Start now' button, 'Continue training' button, and language selector. "
            + "All text content is captured as a snapshot baseline.")
    public void testLaunch_allElementsVisible() {
        assertTrue(screens.launch().isStartNowDisplayed(),
                "Start Now button should be visible on launch");

        String title = screens.launch().getTitle();
        assertEquals(title, "Fitness For Your Brain",
                "Title text should match expected");

        String language = screens.launch().getLanguage();
        assertNotNull(language, "Language selector should have text");
        assertTrue(language.contains("English"),
                "Default language should be English, got: " + language);

        verifyOrRecordContent(screens.launch(), "LaunchScreen");
        softAssert.assertAll();
    }

    @Test(
        description = "Start Now button navigates to App Selection screen",
        groups = {Features.MED, Features.SMOKE}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Start Now navigates forward")
    @Description("Taps 'Start now' and verifies navigation to the App Selection screen "
            + "where user chooses between Medical and Non-medical paths.")
    public void testLaunch_tapStartNow_navigatesToAppSelection() {
        screens.launch().tapStartNow();

        assertTrue(screens.appSelection().isDisplayed(),
                "App Selection screen should be visible after tapping Start Now");
        String headline = screens.appSelection().getHeadlineText();
        assertEquals(headline, "Are you here because of our medical product?",
                "App Selection headline should match");
    }

    @Test(
        description = "Continue Training button navigates to login/returning user flow",
        groups = {Features.MED, Features.LOGIN, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Continue Training navigates to returning user flow")
    @Description("Taps 'Continue training' and verifies it navigates to the returning user "
            + "login flow instead of the new user onboarding.")
    public void testLaunch_tapContinueTraining_navigatesToLogin() {
        screens.launch().tapContinueTraining();

        screens.login().waitForScreen();
        assertTrue(screens.login().isDisplayed(),
                "Login Choice screen should be visible after Continue Training");
        assertEquals(screens.login().getToolbarTitle(), "Login",
                "Login Choice toolbar should read 'Login'");
    }
}
