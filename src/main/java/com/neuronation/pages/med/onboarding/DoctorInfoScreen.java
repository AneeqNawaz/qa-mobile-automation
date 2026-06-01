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

    @AndroidFindBy(id = "nn.mobile.app.med:id/textPostalCode")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField[`value == 'Postal code'`]")
    private WebElement postalCodeInput;

    @AndroidFindBy(id = "nn.mobile.app.med:id/textCity")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField[`value == 'City'`]")
    private WebElement cityInput;

    @AndroidFindBy(id = "nn.mobile.app.med:id/textFirstName")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField[`value == 'First name'`]")
    private WebElement firstNameInput;

    @AndroidFindBy(id = "nn.mobile.app.med:id/textLastName")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField[`value == 'Last name'`]")
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
        type(postalCodeInput, postalCode);
    }

    @Step("Enter doctor city: {city}")
    public void enterCity(String city) {
        log.info("Entering city: {}", city);
        type(cityInput, city);
    }

    @Step("Enter doctor first name: {firstName}")
    public void enterFirstName(String firstName) {
        log.info("Entering first name: {}", firstName);
        type(firstNameInput, firstName);
    }

    @Step("Enter doctor last name: {lastName}")
    public void enterLastName(String lastName) {
        log.info("Entering last name: {}", lastName);
        type(lastNameInput, lastName);
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
        swipeUp();
        tapContinue();
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
