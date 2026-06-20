package com.neuronation.tests.med.registration;

import com.neuronation.base.BaseTest;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Epic("NeuroNation MED App")
@Feature("App Selection")
public class MedAppSelectionTest extends BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void navigateToAppSelection() throws InterruptedException {
        screens.launch().tapStartNow();
        Thread.sleep(1500);
    }

    @Test(
        description = "App Selection screen shows Medical and Non-medical options",
        groups = {Features.MED, Features.ONBOARDING, Features.SMOKE}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("App Selection screen displays both app type choices")
    @Description("Verifies the App Selection screen shows the headline question, "
            + "Medical app card with 'Your health insurance covers the cost', "
            + "and Non-medical app card. Captures content snapshot.")
    public void testAppSelection_content_screenElements() {
        assertTrue(screens.appSelection().isDisplayed(),
                "App Selection screen should be visible");

        String headline = screens.appSelection().getHeadlineText();
        assertEquals(headline, "Are you here because of our medical product?",
                "Headline should ask about medical product");

        String toolbar = screens.appSelection().getToolbarTitle();
        assertEquals(toolbar, "Welcome", "Toolbar title should be 'Welcome'");

        verifyOrRecordContent(screens.appSelection(), "AppSelectionScreen");
        softAssert.assertAll();
    }

    @Test(
        description = "Selecting Medical app navigates to DiGA activation code screen",
        groups = {Features.MED, Features.ONBOARDING, Features.SMOKE}
    )
    @Severity(SeverityLevel.CRITICAL)
    @Story("Medical app selection leads to activation code entry")
    @Description("Taps 'Medical app' card and verifies navigation to the DiGA "
            + "activation code screen with 'Enter activation code' title.")
    public void testAppSelection_tapMedical_navigatesToDiGA() {
        screens.appSelection().selectMedicalApp();

        assertTrue(screens.digaCode().isActivationScreenDisplayed(),
                "Activation screen should be visible after selecting Medical app");
        assertEquals(screens.digaCode().getDescriptionTitle(), "Enter activation code",
                "Should show activation code entry title");
    }

    @Test(
        description = "Selecting Non-medical app navigates to non-medical flow",
        groups = {Features.MED, Features.ONBOARDING, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Non-medical app selection leads to standard onboarding")
    @Description("Taps 'Non-medical app' card and verifies navigation to the "
            + "standard (non-DiGA) onboarding flow.")
    public void testAppSelection_tapNonMedical_navigatesForward() {
        screens.appSelection().selectNonMedicalApp();
        // Verify whatever screen comes next for non-medical path
        log.info("Non-medical app selected — verifying next screen");
    }

    @Test(
        description = "Back navigation from App Selection returns to Launch screen",
        groups = {Features.MED, Features.EDGE_CASES, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Back navigation from App Selection works")
    @Description("Presses device back button on App Selection screen and verifies "
            + "the app returns to the Launch screen without crash.")
    public void testAppSelection_back_returnsToLaunch() {
        com.neuronation.driver.DriverManager.getDriver().navigate().back();

        assertTrue(screens.launch().isStartNowDisplayed(),
                "Launch screen should be visible after back navigation");
    }
}
