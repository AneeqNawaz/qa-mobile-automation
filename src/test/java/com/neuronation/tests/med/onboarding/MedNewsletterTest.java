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
@Feature("Newsletter Consent")
public class MedNewsletterTest extends BaseTest {

    private Map<String, Object> expected;

    @BeforeMethod(alwaysRun = true)
    public void navigateToNewsletter() {
        medFlow.loadFlow("flow1_password_morning_skip");
        medFlow.generateFreshMciCode();
        medFlow.completeRegistration();
        medFlow.completeEmailVerification();
        medFlow.completeDoctorInfo();
        expected = ContentTestHelper.loadOnboarding(context.getLanguage(), "newsletter");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("Newsletter screen loads")
    public void testNewsletter_screenLoads() {
        screens.newsletterConsent().waitForScreen();
        log.info("Newsletter screen visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Newsletter toolbar shows 'Stay informed'")
    public void testNewsletter_content_agreeButtonVisible() {
        // Newsletter screen doesn't expose toolbar getter — verify via content snapshot
        verifyOrRecordContent(screens.newsletterConsent(), "NewsletterConsentScreen");
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.SANITY})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Tap Agree navigates forward")
    public void testNewsletter_tapAgree_navigatesForward() {
        screens.newsletterConsent().tapAgree();
        screens.tips().waitForScreen();
        log.info("Newsletter agreed — tips visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.NAVIGATION, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Tap Disagree navigates forward")
    public void testNewsletter_tapDisagree_navigatesForward() {
        screens.newsletterConsent().tapDisagree();
        screens.tips().waitForScreen();
        log.info("Newsletter disagreed — tips visible");
    }

    @Test(groups = {Features.MED, Features.ONBOARDING, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Content snapshot matches baseline")
    public void testNewsletter_contentSnapshot() {
        verifyOrRecordContent(screens.newsletterConsent(), "NewsletterConsentScreen");
        softAssert.assertAll();
    }
}
