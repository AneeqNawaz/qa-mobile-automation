package com.neuronation.tests.med.onboarding;

import com.neuronation.base.BaseTest;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Epic("NeuroNation MED App")
@Feature("Exercises")
public class MedExerciseTest extends BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void navigateToExercises() {
        medFlow.loadFlow("flow1_password_morning_skip");
        medFlow.generateFreshMciCode();
        medFlow.completeRegistration();
        medFlow.completeEmailVerification();
        medFlow.completeDoctorInfo();
        medFlow.completeNewsletter();
        medFlow.completeTipsScreens();
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("Exercise intro loads after tips")
    public void testExercise_introScreenLoads() {
        screens.exerciseIntro().waitForScreen();
        log.info("Exercise intro visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Tap Start loads exercise game")
    public void testExercise_tapStart_loadsGame() {
        screens.exerciseIntro().tapStart();
        screens.exerciseInGame().waitForScreen();
        log.info("Exercise game loaded");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("Pause button shows menu with Succeed option")
    public void testExercise_pause_showsMenu() {
        screens.exerciseIntro().tapStart();
        screens.exerciseInGame().tapPause();
        log.info("Pause menu visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Succeed exercise advances forward")
    public void testExercise_succeed_advances() {
        screens.exerciseIntro().tapStart();
        screens.exerciseInGame().pauseAndSucceed();
        log.info("Exercise succeeded — advanced forward");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.REGRESSION})
    @Severity(SeverityLevel.CRITICAL)
    @Story("All 4 exercises complete successfully")
    public void testExercise_allFourComplete() {
        medFlow.completeExercises();
        log.info("All 4 exercises completed");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Exercise intro content snapshot")
    public void testExercise_contentSnapshot() {
        verifyOrRecordContent(screens.exerciseIntro(), "ExerciseIntro_1");
        softAssert.assertAll();
    }
}
