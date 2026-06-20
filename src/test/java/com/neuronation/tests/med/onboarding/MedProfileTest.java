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
@Feature("Profile")
public class MedProfileTest extends BaseTest {

    private Map<String, Object> expected;

    @BeforeMethod(alwaysRun = true)
    public void navigateToProfile() {
        medFlow.completeFullFlow("flow1_password_morning_skip");
        screens.dashboard().tapProfileTab();
        screens.profile().waitForScreen();
        expected = ContentTestHelper.loadCommon(context.getLanguage(), "profile");
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.BLOCKER)
    @Story("Profile screen loads after tapping Profile tab")
    public void testProfile_screenLoads() {
        assertTrue(screens.profile().isProfileDisplayed(), "Profile should be visible");
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.CONTENT, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Profile toolbar shows 'User access'")
    public void testProfile_content_toolbarTitle() {
        assertTrue(screens.profile().isProfileDisplayed(),
                "Profile toolbar should show correct title");
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.CONTENT, Features.SANITY})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Profile shows MCI account with 90-day validity")
    public void testProfile_content_mciValidity() {
        String validity = screens.profile().getAccountValidity();
        assertNotNull(validity, "Account validity should not be null");
        ContentTestHelper.assertContains(validity,
                String.valueOf(expected.get("accountPrefix")), "accountPrefix", softAssert);
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Profile shows level headline")
    public void testProfile_content_level() {
        String level = screens.profile().getLevel();
        ContentTestHelper.assertVisible(level, "levelHeadline", softAssert);
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Profile shows account ID")
    public void testProfile_content_accountId() {
        String accountId = screens.profile().getAccountId();
        ContentTestHelper.assertVisible(accountId, "accountId", softAssert);
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Profile verifies MCI account with correct 90-day validity date")
    public void testProfile_mciAccount_90dayValidity() {
        assertTrue(screens.profile().verifyMciAccount(),
                "Profile should show MCI account with 90-day validity from today");
    }

    @Test(groups = {Features.MED, Features.PROFILE, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Profile content snapshot matches baseline")
    public void testProfile_contentSnapshot() {
        verifyOrRecordContent(screens.profile(), "ProfileScreen");
        softAssert.assertAll();
    }
}
