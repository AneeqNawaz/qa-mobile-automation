package com.neuronation.tests.med.onboarding;

import com.neuronation.base.BaseTest;
import com.neuronation.helpers.ContentTestHelper;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.*;

@Epic("NeuroNation MED App")
@Feature("Dashboard")
public class MedDashboardTest extends BaseTest {

    private Map<String, Object> expected;

    @BeforeMethod(alwaysRun = true)
    public void navigateToDashboard() {
        medFlow.completeFullFlow("flow1_password_morning_skip");
        expected = ContentTestHelper.loadCommon(context.getLanguage(), "dashboard");
    }

    @Test(groups = {Features.MED, Features.DASHBOARD, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("Dashboard loads after completing onboarding")
    public void testDashboard_screenLoads() {
        assertTrue(screens.dashboard().isDashboardDisplayed(), "Dashboard should be visible");
    }

    @Test(groups = {Features.MED, Features.DASHBOARD, Features.CONTENT, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Dashboard shows correct level")
    public void testDashboard_content_level() {
        ContentTestHelper.assertText(
                screens.dashboard().getLevel(),
                expected.get("defaultLevel"), "defaultLevel", softAssert);
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.DASHBOARD, Features.CONTENT, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Dashboard shows correct session progress")
    public void testDashboard_content_sessionProgress() {
        ContentTestHelper.assertText(
                screens.dashboard().getSessionProgress(),
                expected.get("defaultSessionProgress"), "defaultSessionProgress", softAssert);
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.DASHBOARD, Features.NAVIGATION, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Dashboard Training tab is tappable")
    public void testDashboard_tapTraining_works() {
        screens.dashboard().tapTrainingTab();
        log.info("Training tab tapped");
    }

    @Test(groups = {Features.MED, Features.DASHBOARD, Features.NAVIGATION, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Dashboard Extras tab is tappable")
    public void testDashboard_tapExtras_works() {
        screens.dashboard().tapExtrasTab();
        log.info("Extras tab tapped");
    }

    @Test(groups = {Features.MED, Features.DASHBOARD, Features.NAVIGATION, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Dashboard Evaluation tab is tappable")
    public void testDashboard_tapEvaluation_works() {
        screens.dashboard().tapEvaluationTab();
        log.info("Evaluation tab tapped");
    }

    @Test(groups = {Features.MED, Features.DASHBOARD, Features.NAVIGATION, Features.SMOKE})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Dashboard Profile tab navigates to Profile screen")
    public void testDashboard_tapProfile_navigatesToProfile() {
        screens.dashboard().tapProfileTab();
        assertTrue(screens.profile().isProfileDisplayed(), "Profile screen should be visible");
    }

    @Test(groups = {Features.MED, Features.DASHBOARD, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Dashboard content snapshot matches baseline")
    public void testDashboard_contentSnapshot() {
        verifyOrRecordContent(screens.dashboard(), "DashboardScreen");
        softAssert.assertAll();
    }
}
