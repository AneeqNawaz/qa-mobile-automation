package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Doctor Info screen — "Who has prescribed you NeuroNation MED?"
 * User enters prescribing doctor details or checks "no prescription" checkbox.
 * Appears after email verification.
 */
public class DoctorInfoScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Who has prescribed you NeuroNation MED?")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/doctorInfo_contactDetails")
    @iOSXCUITFindBy(accessibility = "Contact details of the doctor")
    private WebElement contactDetailsLabel;

    // iOS: match on placeholderValue (stable) NOT value — a TextField's `value` equals the
    // placeholder ONLY while empty; once text is typed `value` becomes that text, so a
    // `value == 'City'` locator can no longer re-resolve the field mid-fill (stale/"not present").
    @AndroidFindBy(id = "nn.mobile.app.med:id/textPostalCode")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField[`placeholderValue == 'Postal code'`]")
    private WebElement postalCodeInput;

    @AndroidFindBy(id = "nn.mobile.app.med:id/textCity")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField[`placeholderValue == 'City'`]")
    private WebElement cityInput;

    @AndroidFindBy(id = "nn.mobile.app.med:id/textFirstName")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField[`placeholderValue == 'First name'`]")
    private WebElement firstNameInput;

    @AndroidFindBy(id = "nn.mobile.app.med:id/textLastName")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField[`placeholderValue == 'Last name'`]")
    private WebElement lastNameInput;

    @AndroidFindBy(id = "nn.mobile.app.med:id/doctorInfo_prescriptionCheckbox")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeSwitch")
    private WebElement noPrescriptionCheckbox;

    @AndroidFindBy(id = "nn.mobile.app.med:id/sendButton")
    @iOSXCUITFindBy(accessibility = "Continue")
    private WebElement continueButton;

    @AndroidFindBy(accessibility = "Navigate up")
    @iOSXCUITFindBy(accessibility = "backButton")
    private WebElement backButton;

    @Step("Verify Doctor Info screen is displayed")
    public boolean isDisplayed() {
        return isDisplayed(continueButton);
    }

    @Step("Enter doctor postal code: {postalCode}")
    public void enterPostalCode(String postalCode) {
        log.info("Entering postal code: {}", postalCode);
        typeIntoField(postalCodeInput, postalCode);
    }

    @Step("Enter doctor city: {city}")
    public void enterCity(String city) {
        log.info("Entering city: {}", city);
        typeIntoField(cityInput, city);
    }

    @Step("Enter doctor first name: {firstName}")
    public void enterFirstName(String firstName) {
        log.info("Entering first name: {}", firstName);
        typeIntoField(firstNameInput, firstName);
    }

    @Step("Enter doctor last name: {lastName}")
    public void enterLastName(String lastName) {
        log.info("Entering last name: {}", lastName);
        typeIntoField(lastNameInput, lastName);
    }

    /** iOS: sending keys without first focusing the target field lets them append to the
     *  previously-focused field (verified on device: the city "Berlin" landed in the postal-code
     *  field as "10115Berlin"). So tap the field to focus it, clear, type, then verify the value
     *  and retry once on mismatch. Android's type() is already reliable here. */
    private void typeIntoField(WebElement field, String text) {
        if (isAndroid()) { type(field, text); return; }
        for (int attempt = 0; attempt < 2; attempt++) {
            tap(field);                                  // focus THIS field first
            try { field.clear(); } catch (Exception ignored) {}
            field.sendKeys(text);
            String v = "";
            try { v = field.getAttribute("value"); } catch (Exception ignored) {}
            if (v != null && v.trim().equals(text.trim())) return;   // typed cleanly
            log.info("Field value '{}' != expected '{}' (attempt {}) — clearing and retrying", v, text, attempt);
            try { field.clear(); } catch (Exception ignored) {}
        }
    }

    @Step("Fill all doctor details")
    public void fillDoctorDetails(String postalCode, String city, String firstName, String lastName) {
        enterPostalCode(postalCode);
        enterCity(city);
        enterFirstName(firstName);
        enterLastName(lastName);
    }

    @Step("Check 'no prescription' checkbox (skip doctor details)")
    public void checkNoPrescription() {
        log.info("Checking no-prescription checkbox");
        swipeUp();
        boolean checked;
        if (isIOS()) {
            checked = "1".equals(noPrescriptionCheckbox.getAttribute("value"));
        } else {
            checked = noPrescriptionCheckbox.isSelected();
        }
        if (!checked) {
            tap(noPrescriptionCheckbox);
        }
    }

    @Step("Tap Continue button")
    public void tapContinue() {
        log.info("Tapping Continue");
        tap(continueButton);
    }

    @Step("Skip doctor info with 'no prescription' and continue")
    public void skipAndContinue() {
        checkNoPrescription();
        tapContinue();
    }

    @Step("Fill doctor details and continue")
    public void fillAndContinue(String postalCode, String city, String firstName, String lastName) {
        fillDoctorDetails(postalCode, city, firstName, lastName);
        dismissKeyboardIos();   // the last field's keyboard covers Continue → dismiss before tapping
        swipeUp();
        tapContinue();
    }

    /** iOS: after the last text field the on-screen keyboard covers the Continue button, so
     *  elementToBeClickable(Continue) times out ("element was not visible"). Tap the non-editable
     *  header to resign first-responder (dismiss the keyboard). No-op on Android (its type() here
     *  doesn't leave a blocking keyboard over Continue). */
    private void dismissKeyboardIos() {
        if (isAndroid()) return;
        try {
            contactDetailsLabel.click();
        } catch (Exception e) {
            try {
                var s = driver.manage().window().getSize();
                tapAt(s.getWidth() / 2, (int) (s.getHeight() * 0.15));
            } catch (Exception ignored) {}
        }
    }

    @Step("Tap back button")
    public void tapBack() {
        log.info("Tapping Back");
        tap(backButton);
    }

    public String getToolbarTitle() {
        return getText(toolbarTitle);
    }

    public String getNoPrescriptionText() {
        return getTextSafe(noPrescriptionCheckbox);
    }

    @Step("Wait for Doctor Info screen to load")
    public void waitForScreen() {
        waitForVisible(continueButton);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("contactDetailsLabel", getTextSafe(contactDetailsLabel));
        content.put("postalCodePlaceholder", getTextSafe(postalCodeInput));
        content.put("cityPlaceholder", getTextSafe(cityInput));
        content.put("firstNamePlaceholder", getTextSafe(firstNameInput));
        content.put("lastNamePlaceholder", getTextSafe(lastNameInput));
        content.put("noPrescriptionCheckbox", getTextSafe(noPrescriptionCheckbox));
        content.put("continueButton", getTextSafe(continueButton));
        return content;
    }
}
