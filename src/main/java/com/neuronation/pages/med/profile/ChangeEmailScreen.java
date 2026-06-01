package com.neuronation.pages.med.profile;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Change Email screen — Profile → Change Email.
 *
 * iOS layout (verified live):
 *   NavigationBar "Change Email" with Back button
 *   StaticText "Change Email"
 *   StaticText "Please enter your new email"
 *   TextField placeholderValue="Current email", value=<user's email>
 *   TextField placeholderValue="New email"
 *   Button "Save"
 *
 * Used in tests to verify that the email shown after login matches
 * the email used to register / log in.
 */
public class ChangeEmailScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeNavigationBar[`name == \"Change Email\"`]/XCUIElementTypeStaticText")
    private WebElement toolbarTitle;

    /** Current email — first TextField on Change Email screen. */
    @AndroidFindBy(uiAutomator = "new UiSelector().className(\"android.widget.EditText\").instance(0)")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField[`placeholderValue == \"Current email\"`]")
    private WebElement currentEmailField;

    @AndroidFindBy(uiAutomator = "new UiSelector().className(\"android.widget.EditText\").instance(1)")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextField[`placeholderValue == \"New email\"`]")
    private WebElement newEmailField;

    @AndroidFindBy(id = "nn.mobile.app.med:id/save_button")
    @iOSXCUITFindBy(accessibility = "Save")
    private WebElement saveButton;

    @Step("Wait for Change Email screen to load")
    public void waitForScreen() {
        waitForVisible(toolbarTitle);
    }

    public boolean isDisplayed() {
        return isDisplayed(toolbarTitle);
    }

    @Step("Get current email shown on Change Email screen")
    public String getCurrentEmail() {
        try {
            // iOS: TextField value carries the actual text; getText() returns "" when label empty.
            String v = isAndroid() ? currentEmailField.getText()
                                   : currentEmailField.getAttribute("value");
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            log.warn("Could not read current email: {}", e.getMessage());
            return "";
        }
    }

    @Step("Type new email: {email}")
    public void typeNewEmail(String email) {
        type(newEmailField, email);
    }

    @Step("Tap Save")
    public void tapSave() {
        tap(saveButton);
    }

    @Step("Tap back from Change Email")
    public void tapBack() {
        if (isAndroid()) {
            driver.navigate().back();
        } else {
            var back = driver.findElements(AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeNavigationBar/XCUIElementTypeButton[`name == \"BackButton\"`]"));
            if (!back.isEmpty()) back.get(0).click();
            else driver.navigate().back();
        }
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("currentEmail", getCurrentEmail());
        return content;
    }
}
