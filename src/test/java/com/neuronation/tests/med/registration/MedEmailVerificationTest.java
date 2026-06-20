package com.neuronation.tests.med.registration;

import com.neuronation.base.BaseTest;
import com.neuronation.helpers.ContentTestHelper;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.*;

@Epic("NeuroNation MED App")
@Feature("Email Verification")
public class MedEmailVerificationTest extends BaseTest {

    private Map<String, Object> expected;

    @BeforeMethod(alwaysRun = true)
    public void navigateToEmailVerification() {
        medFlow.loadFlow("flow1_password_morning_skip");
        medFlow.generateFreshMciCode();
        medFlow.completeRegistrationWithPassword();
        expected = ContentTestHelper.loadRegistration(context.getLanguage(), "emailVerification");
    }

    // ──────────────────────────────────────────────
    // Smoke / Critical
    // ──────────────────────────────────────────────

    @Test(
        description = "Email Verification screen loads after password registration",
        groups = {Features.MED, Features.SMOKE, Features.CRITICAL}
    )
    @Severity(SeverityLevel.BLOCKER)
    @Story("Email Verification screen is displayed after account creation")
    @Description("Verifies the Email Verification ('Protect your account') screen is "
            + "displayed after completing the password registration flow.")
    public void testEmailVerification_screenLoads() {
        assertTrue(screens.emailVerification().isDisplayed(),
                "Email Verification screen should be visible after password registration");
    }

    // ──────────────────────────────────────────────
    // Content verification
    // ──────────────────────────────────────────────

    @Test(
        description = "Toolbar title shows 'Protect your account'",
        groups = {Features.MED, Features.CONTENT, Features.SANITY}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Email Verification toolbar title matches expected content")
    @Description("Verifies the toolbar title text matches the expected 'Protect your account' "
            + "from screen-content.json.")
    public void testEmailVerification_content_toolbarTitle() {
        String actual = screens.emailVerification().getToolbarTitle();
        ContentTestHelper.assertText(actual, expected.get("toolbarTitle"), "toolbarTitle", softAssert);
        softAssert.assertAll();
    }

    @Test(
        description = "Resend email button is visible with correct text",
        groups = {Features.MED, Features.CONTENT, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Resend email button text matches expected content")
    @Description("Verifies the 'Resend email' button is visible and its text matches "
            + "the expected value from screen-content.json.")
    public void testEmailVerification_content_resendButton() {
        Map<String, String> content = screens.emailVerification().captureContent();
        ContentTestHelper.assertText(content.get("resendButton"),
                expected.get("resendButton"), "resendButton", softAssert);
        softAssert.assertAll();
    }

    @Test(
        description = "Change email address button is visible with correct text",
        groups = {Features.MED, Features.CONTENT, Features.REGRESSION}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Change email address button text matches expected content")
    @Description("Verifies the 'Change email address' button is visible and its text matches "
            + "the expected value from screen-content.json.")
    public void testEmailVerification_content_changeEmailButton() {
        Map<String, String> content = screens.emailVerification().captureContent();
        ContentTestHelper.assertText(content.get("changeEmailButton"),
                expected.get("changeEmailButton"), "changeEmailButton", softAssert);
        softAssert.assertAll();
    }

    // ──────────────────────────────────────────────
    // Content snapshot
    // ──────────────────────────────────────────────

    @Test(
        description = "Full content snapshot for Email Verification screen",
        groups = {Features.MED, Features.CONTENT, Features.REGRESSION}
    )
    @Severity(SeverityLevel.MINOR)
    @Story("Email Verification content snapshot baseline")
    @Description("Captures all visible text on the Email Verification screen and "
            + "either records a new baseline or verifies against the existing one.")
    public void testEmailVerification_contentSnapshot() {
        verifyOrRecordContent(screens.emailVerification(), "EmailVerificationScreen");
        softAssert.assertAll();
    }
}
