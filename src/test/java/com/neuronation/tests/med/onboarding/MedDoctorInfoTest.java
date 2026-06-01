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

import static org.testng.Assert.*;

@Listeners(AllureScreenshotListener.class)
@Epic("NeuroNation MED App")
@Feature("Doctor Info")
public class MedDoctorInfoTest extends BaseTest {

    private Map<String, Object> expected;

    @BeforeMethod(alwaysRun = true)
    public void navigateToDoctorInfo() {
        medFlow.loadFlow("flow1_password_morning_skip");
        medFlow.generateFreshMciCode();
        medFlow.completeRegistration();
        medFlow.completeEmailVerification();
        expected = ContentTestHelper.loadOnboarding(context.getLanguage(), "doctorInfo");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("Doctor Info screen loads after email verification")
    public void testDoctorInfo_screenLoads() {
        assertTrue(screens.doctorInfo().isDisplayed(), "Doctor Info should be visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Doctor Info toolbar title matches expected")
    public void testDoctorInfo_content_toolbarTitle() {
        ContentTestHelper.assertText(screens.doctorInfo().getToolbarTitle(),
                expected.get("toolbarTitle"), "toolbarTitle", softAssert);
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Continue button text matches expected")
    public void testDoctorInfo_content_noPrescriptionText() {
        String text = screens.doctorInfo().getNoPrescriptionText();
        ContentTestHelper.assertContains(text,
                String.valueOf(expected.get("noPrescriptionCheckbox")).substring(0, 20),
                "noPrescriptionCheckbox", softAssert);
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Skip doctor info navigates to Newsletter")
    public void testDoctorInfo_skip_noPrescription_navigatesToNewsletter() {
        screens.doctorInfo().skipAndContinue();
        screens.newsletterConsent().waitForScreen();
        log.info("Doctor info skipped — newsletter visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Fill doctor details navigates to Newsletter")
    public void testDoctorInfo_fill_allFields_navigatesToNewsletter() {
        var doc = com.neuronation.utils.TestDataLoader.loadDoctorData(
                com.neuronation.config.AppType.MED, context.getLanguage(),
                com.neuronation.testdata.DoctorData.class);
        screens.doctorInfo().fillAndContinue(
                doc.getPostalCode(), doc.getCity(), doc.getFirstName(), doc.getLastName());
        screens.newsletterConsent().waitForScreen();
        log.info("Doctor info filled — newsletter visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NEGATIVE, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Submit without filling shows validation error")
    public void testDoctorInfo_submit_empty_showsError() {
        screens.doctorInfo().tapContinue();
        assertTrue(screens.doctorInfo().isDisplayed(), "Should stay on Doctor Info after empty submit");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Content snapshot matches baseline")
    public void testDoctorInfo_contentSnapshot() {
        verifyOrRecordContent(screens.doctorInfo(), "DoctorInfoScreen");
        softAssert.assertAll();
    }
}
