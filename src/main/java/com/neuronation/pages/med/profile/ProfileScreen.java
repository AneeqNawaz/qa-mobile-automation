package com.neuronation.pages.med.profile;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Profile screen — "User access"
 * Shows account details, level, session progress, and all account management options.
 *
 * All discovered IDs stored (Android resource-id):
 *   main_toolbar_title = "User access"
 *   accountIdLabel = "User ID: xxx"
 *   accountValidDateLabel = "Account for MCI valid until DD.MM.YYYY"
 *   level_headline = "Level 1 - Newcomer"
 *   session_progress = "Next level in 1 session"
 *   accountTitle = "Account"
 *   menu_username_title = "Change Name" / "Change Password" (shared ID)
 *   menu_email_title = "Change Email"
 *   menu_settings_title = "Settings"
 *   main_menu_logout_title = "Log out"
 *   menu_logout_other_title = "Log out from other devices"
 *   menu_tos_title = "Terms of Use"
 *   menu_policy_title = "Privacy Policy"
 *   menu_faq_title = "Help"
 *   menu_tips_title = "Tips and important information"
 *   menu_export_title = "Data export"
 *   menu_manual_title = "Download user manual"
 *   menu_delete_account_title = "Delete account"
 *   menu_bug_report_title = "Your feedback"
 *   menu_privacy_title = "Privacy Settings"
 *   menu_consent_history_title = "Consent History"
 *   menu_login_history_title = "Login history"
 *   menu_about_title = "About us"
 *   menu_healthID_title = "Link to Gesundheits-ID"
 *   menu_renew_recovery_codes_title = "Renew recovery codes"
 *   menu_unbind_device_title = "Unbind device"
 *   menu_passkey_title = "Passkeys"
 *   menu_debug_title = "Non Patient Servises"
 *   supportInformationTitle = "Support Information"
 *   dataInformationTitle = "Data information"
 *   version_text = "v 2.2.42uns"
 *   account_non_patient_label = test account warning (conditional)
 */
public class ProfileScreen extends BaseScreen {

    // ── Header section ──
    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "User access")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/accountIdLabel")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name BEGINSWITH 'User ID:'`]")
    private WebElement accountId;

    @AndroidFindBy(id = "nn.mobile.app.med:id/accountValidDateLabel")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name BEGINSWITH 'Account for'`]")
    private WebElement accountValidity;

    @AndroidFindBy(id = "nn.mobile.app.med:id/account_non_patient_label")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name CONTAINS 'test account'`]")
    private WebElement testAccountWarning;

    @AndroidFindBy(id = "nn.mobile.app.med:id/level_headline")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name BEGINSWITH 'Level'`]")
    private WebElement levelHeadline;

    @AndroidFindBy(id = "nn.mobile.app.med:id/session_progress")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name CONTAINS 'session'`]")
    private WebElement sessionProgress;

    // ── Account section ──
    @AndroidFindBy(id = "nn.mobile.app.med:id/accountTitle")
    @iOSXCUITFindBy(accessibility = "Profile")
    private WebElement accountTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_username_title")
    @iOSXCUITFindBy(accessibility = "Change Name")
    private WebElement changeName;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_email_title")
    @iOSXCUITFindBy(accessibility = "Change Email")
    private WebElement changeEmail;

    /** Email value shown on Profile (subtitle of Change Email row).
     *  iOS: only the user's email contains '@' on this screen.
     *  Android: the editcycleCellSubTitle for the Change Email row contains '@'. */
    @AndroidFindBy(uiAutomator = "new UiSelector().resourceId(\"nn.mobile.app.med:id/editcycleCellSubTitle\").textContains(\"@\")")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name CONTAINS \"@\" AND visible == 1`]")
    private WebElement userEmailValue;

    // ── Actions ──
    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_settings_title")
    @iOSXCUITFindBy(accessibility = "Settings")
    private WebElement settingsButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_menu_logout_title")
    @iOSXCUITFindBy(accessibility = "Log out")
    private WebElement logoutButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_logout_other_title")
    @iOSXCUITFindBy(accessibility = "Log out from other devices")
    private WebElement logoutOtherButton;

    // ── Support & Legal ──
    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_tos_title")
    @iOSXCUITFindBy(accessibility = "Terms of Use")
    private WebElement termsOfUse;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_policy_title")
    @iOSXCUITFindBy(accessibility = "Privacy Policy")
    private WebElement privacyPolicy;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_faq_title")
    @iOSXCUITFindBy(accessibility = "Help")
    private WebElement help;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_tips_title")
    @iOSXCUITFindBy(accessibility = "Tips and important information")
    private WebElement tipsButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_export_title")
    @iOSXCUITFindBy(accessibility = "Data export")
    private WebElement dataExport;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_manual_title")
    @iOSXCUITFindBy(accessibility = "Download user manual")
    private WebElement downloadManual;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_delete_account_title")
    @iOSXCUITFindBy(accessibility = "Delete account")
    private WebElement deleteAccount;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_bug_report_title")
    @iOSXCUITFindBy(accessibility = "Your feedback")
    private WebElement feedback;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_privacy_title")
    @iOSXCUITFindBy(accessibility = "Privacy Settings")
    private WebElement privacySettings;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_consent_history_title")
    @iOSXCUITFindBy(accessibility = "Consent History")
    private WebElement consentHistory;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_login_history_title")
    @iOSXCUITFindBy(accessibility = "Login history")
    private WebElement loginHistory;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_about_title")
    @iOSXCUITFindBy(accessibility = "About us")
    private WebElement aboutUs;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_healthID_title")
    @iOSXCUITFindBy(accessibility = "Link to Gesundheits-ID")
    private WebElement linkHealthId;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_renew_recovery_codes_title")
    @iOSXCUITFindBy(accessibility = "Renew recovery codes")
    private WebElement renewRecoveryCodes;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_unbind_device_title")
    @iOSXCUITFindBy(accessibility = "Unbind device")
    private WebElement unbindDevice;

    @AndroidFindBy(id = "nn.mobile.app.med:id/menu_passkey_title")
    @iOSXCUITFindBy(accessibility = "Passkeys")
    private WebElement passkeys;

    @AndroidFindBy(id = "nn.mobile.app.med:id/version_text")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name BEGINSWITH 'v '`]")
    private WebElement versionText;

    // ══════════════════════════════════════════════
    // Methods
    // ══════════════════════════════════════════════

    @Step("Wait for Profile screen to load")
    public void waitForScreen() {
        // Toolbar is always visible regardless of scroll position; accountValidity
        // can be scrolled off when returning from Settings.
        waitForVisible(toolbarTitle);
    }

    @Step("Check if Profile screen is displayed")
    public boolean isProfileDisplayed() {
        return isDisplayed(toolbarTitle);
    }

    // ── Getters ──

    public String getAccountId() { return getText(accountId); }
    public String getAccountValidity() {
        // iOS: element may be in hierarchy but scrolled off-screen (visible=false).
        // Scroll it into view via mobile:scroll, then read text.
        try {
            if (!isAndroid()) {
                try {
                    var args = new java.util.HashMap<String, Object>();
                    args.put("predicateString", "name BEGINSWITH 'Account for'");
                    args.put("toVisible", true);
                    ((io.appium.java_client.AppiumDriver) driver).executeScript("mobile: scroll", args);
                } catch (Exception ignored) {}
            }
            // Use presence (not visibility) — iOS may report visible=false but text is readable
            return new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30))
                    .until(d -> {
                        try {
                            String t = accountValidity.getText();
                            return (t != null && !t.isEmpty()) ? t : null;
                        } catch (Exception e) { return null; }
                    });
        } catch (Exception e) {
            return getText(accountValidity);
        }
    }
    public String getLevel() { return getText(levelHeadline); }
    public String getSessionProgress() { return getTextSafe(sessionProgress); }
    public String getVersionText() { return getTextSafe(versionText); }

    @Step("Get user email shown on Profile")
    public String getUserEmail() {
        // Email lives next to "Change Email" — scroll to that row, then read the value.
        scrollToElement("nn.mobile.app.med:id/menu_email_title", "Change Email");
        try {
            String txt = isAndroid() ? userEmailValue.getText() : userEmailValue.getAttribute("name");
            return txt == null ? "" : txt.trim();
        } catch (Exception e) {
            log.warn("Could not read user email from Profile: {}", e.getMessage());
            return "";
        }
    }

    // ── Actions ──

    @Step("Tap 'Change Name'")
    public void tapChangeName() { tap(changeName); }

    @Step("Tap 'Change Email'")
    public void tapChangeEmail() {
        scrollToElement("nn.mobile.app.med:id/menu_email_title", "Change Email");
        tap(changeEmail);
    }

    @Step("Tap 'Settings'")
    public void tapSettings() {
        scrollToElement("nn.mobile.app.med:id/menu_settings_title", "Settings");
        tap(settingsButton);
    }

    @Step("Tap 'Log out'")
    public void tapLogout() {
        log.info("Tapping Log out");
        scrollToElement("nn.mobile.app.med:id/main_menu_logout_title", "Log out");
        tap(logoutButton);
    }

    @Step("Tap 'Log out from other devices'")
    public void tapLogoutOther() { swipeUp(); tap(logoutOtherButton); }

    @Step("Tap 'Terms of Use'")
    public void tapTermsOfUse() { swipeUp(); tap(termsOfUse); }

    @Step("Tap 'Privacy Policy'")
    public void tapPrivacyPolicy() { swipeUp(); tap(privacyPolicy); }

    @Step("Tap 'Help'")
    public void tapHelp() { swipeUp(); tap(help); }

    @Step("Tap 'Delete account'")
    public void tapDeleteAccount() { swipeUp(); tap(deleteAccount); }

    @Step("Tap 'Privacy Settings'")
    public void tapPrivacySettings() { swipeUp(); tap(privacySettings); }

    @Step("Tap 'About us'")
    public void tapAboutUs() { swipeUp(); tap(aboutUs); }

    @Step("Tap 'Passkeys'")
    public void tapPasskeys() { swipeUp(); tap(passkeys); }

    // ── Verification ──

    @io.qameta.allure.Step("Verify MCI account with 90-day validity")
    public boolean verifyMciAccount() {
        String validity = getAccountValidity();
        boolean isMci = validity != null && validity.contains("MCI");
        java.time.LocalDate expected = java.time.LocalDate.now().plusDays(90);
        String expectedDate = expected.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        boolean validDate = validity != null && validity.contains(expectedDate);
        log.info("Profile MCI={}, expected={}, actual={}", isMci, expectedDate, validity);
        return isMci && validDate;
    }

    @io.qameta.allure.Step("Verify all profile elements visible")
    public boolean verifyAllElementsVisible() {
        boolean id = getAccountId() != null && !getAccountId().isEmpty();
        boolean val = getAccountValidity() != null && !getAccountValidity().isEmpty();
        boolean lvl = getLevel() != null && !getLevel().isEmpty();
        return id && val && lvl;
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("levelHeadline", getTextSafe(levelHeadline));
        content.put("sessionProgress", getTextSafe(sessionProgress));
        content.put("accountTitle", getTextSafe(accountTitle));
        content.put("changeName", getTextSafe(changeName));
        content.put("changeEmail", getTextSafe(changeEmail));
        return content;
    }
}
