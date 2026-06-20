package com.neuronation.tests.med.onboarding;

import com.neuronation.base.BaseTest;
import com.neuronation.helpers.ContentTestHelper;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

@Epic("NeuroNation MED App")
@Feature("Age Group Selection")
public class MedAgeGroupTest extends BaseTest {

    private Map<String, Object> expected;

    @BeforeMethod(alwaysRun = true)
    public void navigateToAgeGroup() {
        medFlow.loadFlow("flow1_password_morning_skip");
        medFlow.generateFreshMciCode();
        medFlow.completeRegistration();
        medFlow.completeEmailVerification();
        medFlow.completeDoctorInfo();
        medFlow.completeNewsletter();
        medFlow.completeTipsScreens();
        medFlow.completeExercises();
        expected = ContentTestHelper.loadOnboarding(context.getLanguage(), "ageGroup");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("Age Group popup loads after exercises")
    public void testAgeGroup_screenLoads() {
        screens.ageGroup().waitForScreen();
        assertTrue(screens.ageGroup().isDisplayed(), "Age Group should be visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Select age group advances to evaluation")
    public void testAgeGroup_selectOption_advances() {
        var profile = com.neuronation.utils.TestDataLoader.loadProfileData(
                com.neuronation.config.AppType.MED, context.getLanguage(),
                com.neuronation.testdata.ProfileData.class);
        screens.ageGroup().selectAgeGroup(profile.getAgeGroup());
        screens.evaluation().waitForScreen();
        log.info("Age group selected — evaluation visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Age Group content snapshot")
    public void testAgeGroup_contentSnapshot() {
        verifyOrRecordContent(screens.ageGroup(), "AgeGroupScreen");
        softAssert.assertAll();
    }
}
