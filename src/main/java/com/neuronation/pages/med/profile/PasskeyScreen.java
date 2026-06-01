package com.neuronation.pages.med.profile;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Passkey / Passwordless Login screen — "Use Passwordless Login?"
 *
 * Belongs to the Profile → Passkey feature. Appears in two contexts:
 *   1. After email registration (Set Password flow) — buttons: Yes / Later / No, use password
 *   2. After re-login (returning user) — buttons: Yes / Not now / No, use password
 *
 * Tapping "No, use password" prevents the screen from appearing again for that account
 * on subsequent logins, so it's the safe dismissal in non-passkey-focused tests.
 *
 * iOS structure (verified live, post-login variant):
 *   NavigationBar name="Passkey"
 *   Image name="passkey"                                      (logo)
 *   StaticText "Use Passwordless Login?"                      (heading)
 *   StaticText  long privacy/feature description              (content)
 *   Image name="ic_passkey_face"  + StaticText "Unlock with facial recognition"
 *   Image name="ic_passkey_finger"+ StaticText "Unlock with fingerprint"
 *   Image name="ic_passkey_pattern"+ StaticText "Unlock using device pin or pattern"
 *   Button "Yes" / "Not now" / "No, use password"
 */
public class PasskeyScreen extends BaseScreen {

    // ── Header ──
    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeNavigationBar[`name == \"Passkey\"`]/XCUIElementTypeStaticText")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/passkey_image")
    @iOSXCUITFindBy(accessibility = "passkey")
    private WebElement passkeyImage;

    @AndroidFindBy(id = "nn.mobile.app.med:id/title")
    @iOSXCUITFindBy(accessibility = "Use Passwordless Login?")
    private WebElement title;

    @AndroidFindBy(id = "nn.mobile.app.med:id/content")
    @iOSXCUITFindBy(accessibility = "passkey_content")
    private WebElement contentText;

    // ── Feature rows (icon + label) ──
    @AndroidFindBy(id = "nn.mobile.app.med:id/ic_passkey_face")
    @iOSXCUITFindBy(accessibility = "ic_passkey_face")
    private WebElement faceIcon;

    @AndroidFindBy(id = "nn.mobile.app.med:id/passkey_face_text")
    @iOSXCUITFindBy(accessibility = "Unlock with facial recognition")
    private WebElement faceText;

    @AndroidFindBy(id = "nn.mobile.app.med:id/ic_passkey_finger")
    @iOSXCUITFindBy(accessibility = "ic_passkey_finger")
    private WebElement fingerIcon;

    @AndroidFindBy(id = "nn.mobile.app.med:id/passkey_finger_text")
    @iOSXCUITFindBy(accessibility = "Unlock with fingerprint")
    private WebElement fingerText;

    @AndroidFindBy(id = "nn.mobile.app.med:id/ic_passkey_pattern")
    @iOSXCUITFindBy(accessibility = "ic_passkey_pattern")
    private WebElement patternIcon;

    @AndroidFindBy(id = "nn.mobile.app.med:id/passkey_pattern_text")
    @iOSXCUITFindBy(accessibility = "Unlock using device pin or pattern")
    private WebElement patternText;

    // ── Buttons ──
    @AndroidFindBy(id = "nn.mobile.app.med:id/yesButton")
    @iOSXCUITFindBy(accessibility = "Yes")
    private WebElement yesButton;

    /** Registration-flow button (post-registration screen) */
    @AndroidFindBy(id = "nn.mobile.app.med:id/noButton")
    @iOSXCUITFindBy(accessibility = "Later")
    private WebElement laterButton;

    /** Post-login screen button (returning user) */
    @AndroidFindBy(id = "nn.mobile.app.med:id/notNowButton")
    @iOSXCUITFindBy(accessibility = "Not now")
    private WebElement notNowButton;

    @AndroidFindBy(id = "nn.mobile.app.med:id/neverButton")
    @iOSXCUITFindBy(accessibility = "No, use password")
    private WebElement noUsePasswordButton;

    // ── Lifecycle ──

    @Step("Wait for Passkey screen to load")
    public void waitForScreen() {
        waitForVisible(yesButton);
    }

    @Step("Verify Passkey screen is displayed")
    public boolean isDisplayed() {
        return isDisplayed(yesButton);
    }

    // ── Actions ──

    @Step("Tap 'Yes' to enable Passkey login")
    public void tapYes() {
        log.info("Selecting Yes for Passkey login");
        tap(yesButton);
    }

    @Step("Tap 'Later' (registration flow)")
    public void tapLater() {
        log.info("Selecting Later — skipping Passkey");
        tap(laterButton);
    }

    @Step("Tap 'Not now' (post-login flow)")
    public void tapNotNow() {
        log.info("Selecting Not now — deferring Passkey setup");
        tap(notNowButton);
    }

    @Step("Tap 'No, use password' (won't show again for this account)")
    public void tapNoUsePassword() {
        log.info("Selecting No, use password");
        tap(noUsePasswordButton);
    }

    // ── Getters ──

    public String getTitle() { return getText(title); }
    public String getContentText() { return getText(contentText); }
    public String getFaceText() { return getTextSafe(faceText); }
    public String getFingerText() { return getTextSafe(fingerText); }
    public String getPatternText() { return getTextSafe(patternText); }

    @Step("Is top passkey image displayed")
    public boolean isImageDisplayed() { return isImageVisible(passkeyImage); }

    @Step("Is facial-recognition icon displayed")
    public boolean isFaceIconDisplayed() { return isImageVisible(faceIcon); }

    @Step("Is fingerprint icon displayed")
    public boolean isFingerIconDisplayed() { return isImageVisible(fingerIcon); }

    @Step("Is pin/pattern icon displayed")
    public boolean isPatternIconDisplayed() { return isImageVisible(patternIcon); }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("imageDisplayed", String.valueOf(isImageDisplayed()));
        content.put("title", getTextSafe(title));
        content.put("contentText", getTextSafe(contentText));
        content.put("faceIconDisplayed", String.valueOf(isFaceIconDisplayed()));
        content.put("faceText", getTextSafe(faceText));
        content.put("fingerIconDisplayed", String.valueOf(isFingerIconDisplayed()));
        content.put("fingerText", getTextSafe(fingerText));
        content.put("patternIconDisplayed", String.valueOf(isPatternIconDisplayed()));
        content.put("patternText", getTextSafe(patternText));
        content.put("yesButton", getTextSafe(yesButton));
        content.put("laterButton", getTextSafe(laterButton));
        content.put("notNowButton", getTextSafe(notNowButton));
        content.put("noUsePasswordButton", getTextSafe(noUsePasswordButton));
        return content;
    }
}
