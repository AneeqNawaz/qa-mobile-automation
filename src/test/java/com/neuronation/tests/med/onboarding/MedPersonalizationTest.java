package com.neuronation.tests.med.onboarding;

import com.neuronation.base.BaseTest;
import com.neuronation.helpers.ContentTestHelper;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Combined test class for sequential personalization screens:
 * Training Complexity → Special Needs → Training Time → Schedule Review → Notification
 * → NeuroBooster → Promise
 *
 * These are combined because they're fast screens that require deep navigation
 * (exercises + age group + evaluation to reach). Testing them individually would
 * waste 3+ minutes of navigation per test.
 */
@Epic("NeuroNation MED App")
@Feature("Personalization")
public class MedPersonalizationTest extends BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void navigateToPersonalization() {
        medFlow.loadFlow("flow1_password_morning_skip");
        medFlow.generateFreshMciCode();
        medFlow.completeRegistration();
        medFlow.completeEmailVerification();
        medFlow.completeDoctorInfo();
        medFlow.completeNewsletter();
        medFlow.completeTipsScreens();
        medFlow.completeExercises();
        medFlow.completeAgeSelection();
        medFlow.completeEvaluation();
    }

    // ── Training Complexity ──

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.SMOKE})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Training Complexity screen loads")
    public void testComplexity_screenLoads() {
        screens.trainingComplexity().waitForScreen();
        log.info("Training Complexity visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Activate advances to Special Needs")
    public void testComplexity_tapActivate_advances() {
        medFlow.completeTrainingComplexity();
        screens.specialNeeds().waitForScreen();
        log.info("Complexity activated — Special Needs visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Training Complexity content snapshot")
    public void testComplexity_contentSnapshot() {
        verifyOrRecordContent(screens.trainingComplexity(), "TrainingComplexityScreen");
        softAssert.assertAll();
    }

    // ── Special Needs ──

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.SMOKE})
    @Severity(SeverityLevel.NORMAL)
    @Story("Special Needs screen loads")
    public void testSpecialNeeds_screenLoads() {
        medFlow.completeTrainingComplexity();
        screens.specialNeeds().waitForScreen();
        log.info("Special Needs visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Standard selection advances to Training Time")
    public void testSpecialNeeds_selectStandard_advances() {
        medFlow.completeTrainingComplexity();
        medFlow.completeSpecialNeeds();
        screens.trainingTime().waitForScreen();
        log.info("Standard selected — Training Time visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Special Needs content snapshot")
    public void testSpecialNeeds_contentSnapshot() {
        medFlow.completeTrainingComplexity();
        verifyOrRecordContent(screens.specialNeeds(), "SpecialNeedsScreen");
        softAssert.assertAll();
    }

    // ── Training Time ──

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.SMOKE})
    @Severity(SeverityLevel.NORMAL)
    @Story("Training Time screen loads")
    public void testTrainingTime_screenLoads() {
        medFlow.completeTrainingComplexity();
        medFlow.completeSpecialNeeds();
        screens.trainingTime().waitForScreen();
        log.info("Training Time visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Morning selection advances to Schedule Review")
    public void testTrainingTime_selectMorning_advances() {
        medFlow.completeTrainingComplexity();
        medFlow.completeSpecialNeeds();
        medFlow.completeTrainingTime();
        screens.scheduleReview().waitForScreen();
        log.info("Morning selected — Schedule Review visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Training Time content snapshot")
    public void testTrainingTime_contentSnapshot() {
        medFlow.completeTrainingComplexity();
        medFlow.completeSpecialNeeds();
        verifyOrRecordContent(screens.trainingTime(), "TrainingTimeScreen");
        softAssert.assertAll();
    }

    // ── Schedule Review ──

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.SMOKE})
    @Severity(SeverityLevel.NORMAL)
    @Story("Schedule Review screen loads")
    public void testScheduleReview_screenLoads() {
        medFlow.completeTrainingComplexity();
        medFlow.completeSpecialNeeds();
        medFlow.completeTrainingTime();
        screens.scheduleReview().waitForScreen();
        log.info("Schedule Review visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Confirm advances past notification")
    public void testScheduleReview_tapConfirm_advances() {
        medFlow.completeTrainingComplexity();
        medFlow.completeSpecialNeeds();
        medFlow.completeTrainingTime();
        medFlow.completeScheduleReview();
        medFlow.completeNotificationPermission();
        screens.neuroBooster().waitForScreen();
        log.info("Schedule confirmed — NeuroBooster visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Schedule Review content snapshot")
    public void testScheduleReview_contentSnapshot() {
        medFlow.completeTrainingComplexity();
        medFlow.completeSpecialNeeds();
        medFlow.completeTrainingTime();
        verifyOrRecordContent(screens.scheduleReview(), "ScheduleReviewScreen");
        softAssert.assertAll();
    }

    // ── NeuroBooster ──

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.SMOKE})
    @Severity(SeverityLevel.NORMAL)
    @Story("NeuroBooster screen loads")
    public void testNeuroBooster_screenLoads() {
        medFlow.completeTrainingComplexity();
        medFlow.completeSpecialNeeds();
        medFlow.completeTrainingTime();
        medFlow.completeScheduleReview();
        medFlow.completeNotificationPermission();
        screens.neuroBooster().waitForScreen();
        log.info("NeuroBooster visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("NeuroBooster Yes shows Promise")
    public void testNeuroBooster_tapYes_showsPromise() {
        medFlow.completeTrainingComplexity();
        medFlow.completeSpecialNeeds();
        medFlow.completeTrainingTime();
        medFlow.completeScheduleReview();
        medFlow.completeNotificationPermission();
        medFlow.completeNeuroBooster();
        screens.promise().waitForScreen();
        log.info("NeuroBooster yes — Promise visible");
    }

    // ── Promise ──

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Promise Yes advances to final tips")
    public void testPromise_tapYes_advances() {
        medFlow.completeTrainingComplexity();
        medFlow.completeSpecialNeeds();
        medFlow.completeTrainingTime();
        medFlow.completeScheduleReview();
        medFlow.completeNotificationPermission();
        medFlow.completeNeuroBooster();
        medFlow.completePromise();
        screens.tips().waitForScreen();
        log.info("Promise yes — final tips visible");
    }
}
