package com.neuronation.tests.med.onboarding;

import com.neuronation.base.BaseTest;
import com.neuronation.helpers.ContentTestHelper;
import com.neuronation.listeners.AllureScreenshotListener;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Map;

@Listeners(AllureScreenshotListener.class)
@Epic("NeuroNation MED App")
@Feature("Evaluation")
public class MedEvaluationTest extends BaseTest {

    private Map<String, Object> expected;

    @BeforeMethod(alwaysRun = true)
    public void navigateToEvaluation() {
        medFlow.loadFlow("flow1_password_morning_skip");
        medFlow.generateFreshMciCode();
        medFlow.completeRegistration();
        medFlow.completeEmailVerification();
        medFlow.completeDoctorInfo();
        medFlow.completeNewsletter();
        medFlow.completeTipsScreens();
        medFlow.completeExercises();
        medFlow.completeAgeSelection();
        expected = ContentTestHelper.loadOnboarding(context.getLanguage(), "evaluation");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("Evaluation screen loads after age group")
    public void testEvaluation_screenLoads() {
        screens.evaluation().waitForScreen();
        log.info("Evaluation screen visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Create Plan button advances to Training Complexity")
    public void testEvaluation_tapCreatePlan_advances() {
        medFlow.completeEvaluation();
        screens.trainingComplexity().waitForScreen();
        log.info("Create Plan tapped — complexity screen visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Evaluation content snapshot")
    public void testEvaluation_contentSnapshot() {
        verifyOrRecordContent(screens.evaluation(), "EvaluationScreen");
        softAssert.assertAll();
    }
}
