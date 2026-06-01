package com.neuronation.tests.med.onboarding;

import com.neuronation.base.BaseTest;
import com.neuronation.helpers.ContentTestHelper;
import com.neuronation.listeners.AllureScreenshotListener;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;




@Listeners(AllureScreenshotListener.class)
@Epic("NeuroNation MED App")
@Feature("Tips Screens")
public class MedTipsTest extends BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void navigateToTips() {
        medFlow.loadFlow("flow1_password_morning_skip");
        medFlow.generateFreshMciCode();
        medFlow.completeRegistration();
        medFlow.completeEmailVerification();
        medFlow.completeDoctorInfo();
        medFlow.completeNewsletter();
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("First tips screen loads")
    public void testTips_firstTipLoads() {
        screens.tips().waitForScreen();
        log.info("First tip screen visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Tip 1 title is 'Tips'")
    public void testTips_content_tip1Title() {
        var expected = ContentTestHelper.loadOnboarding(context.getLanguage(), "tips1");
        ContentTestHelper.assertText(screens.tips().getTitle(),
                expected.get("toolbarTitle"), "tip1Title", softAssert);
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Tip 2 title is 'Important information'")
    public void testTips_content_tip2Title() {
        screens.tips().tapUnderstood(); // dismiss tip 1
        var expected = ContentTestHelper.loadOnboarding(context.getLanguage(), "tips2");
        ContentTestHelper.assertText(screens.tips().getTitle(),
                expected.get("toolbarTitle"), "tip2Title", softAssert);
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Tip 3 title is 'Attention'")
    public void testTips_content_tip3Title() {
        screens.tips().tapUnderstood(); // dismiss tip 1
        screens.tips().tapUnderstood(); // dismiss tip 2
        var expected = ContentTestHelper.loadOnboarding(context.getLanguage(), "tips3");
        ContentTestHelper.assertText(screens.tips().getTitle(),
                expected.get("toolbarTitle"), "tip3Title", softAssert);
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.CRITICAL)
    @Story("All tips dismissed navigates to exercise intro")
    public void testTips_allDismissed_navigatesToExercise() {
        medFlow.completeTipsScreens();
        screens.exerciseIntro().waitForScreen();
        log.info("All tips dismissed — exercise intro visible");
    }
}
