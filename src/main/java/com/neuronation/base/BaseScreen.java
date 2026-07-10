package com.neuronation.base;

import com.neuronation.config.ConfigManager;
import com.neuronation.config.Platform;
import com.neuronation.driver.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class BaseScreen {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final AppiumDriver driver;
    protected final WebDriverWait wait;

    public BaseScreen() {
        this.driver = DriverManager.getDriver();
        int timeout = ConfigManager.getInstance().getInt("explicit.wait.seconds");
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        PageFactory.initElements(new AppiumFieldDecorator(driver, Duration.ofSeconds(timeout)), this);
    }

    protected void tap(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element)).click();
    }

    protected void type(WebElement element, String text) {
        WebElement el = wait.until(ExpectedConditions.visibilityOf(element));
        el.clear();
        el.sendKeys(text);
    }

    protected String getText(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element)).getText();
    }

    /**
     * Get text from a PageFactory proxy element safely.
     * Uses the proxy directly — suitable for elements that SHOULD exist.
     * For optional/missing elements, use getTextById() to avoid 30s proxy timeout.
     */
    protected String getTextSafe(WebElement element) {
        try {
            return element.getText();
        } catch (Exception e) {
            log.warn("Could not get text for element: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Get text by resource ID — bypasses PageFactory proxy entirely.
     * Returns immediately with "" if element doesn't exist (no 30s wait).
     * Use this in captureContent() for elements that may not be on screen.
     */
    protected String getTextById(String resourceId) {
        var elements = driver.findElements(AppiumBy.id(resourceId));
        if (elements.isEmpty()) return "";
        try {
            return elements.get(0).getText();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if an element is visible by resource ID — no proxy, no 30s wait.
     */
    protected boolean isVisibleById(String resourceId) {
        var elements = driver.findElements(AppiumBy.id(resourceId));
        return !elements.isEmpty() && elements.get(0).isDisplayed();
    }

    /** Like {@link #isVisibleById} but never throws on a stale/!displayed element. */
    protected boolean isDisplayedById(String resourceId) {
        for (var el : driver.findElements(AppiumBy.id(resourceId))) {
            try { if (el.isDisplayed()) return true; } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * Check if an image/visual element is visible on screen.
     * Uses the proxy — suitable for elements that SHOULD exist.
     */
    protected boolean isImageVisible(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected void waitForVisible(WebElement element) {
        try {
            wait.until(ExpectedConditions.visibilityOf(element));
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            // PageFactory proxy will re-locate — retry once
            wait.until(ExpectedConditions.visibilityOf(element));
        }
    }

    protected void waitForVisible(WebElement element, int timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.visibilityOf(element));
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.visibilityOf(element));
        }
    }

    @Step("Swipe up on screen")
    protected void swipeUp() {
        var dimensions = driver.manage().window().getSize();
        int startX = dimensions.getWidth() / 2;
        int startY = (int) (dimensions.getHeight() * 0.8);
        int endY = (int) (dimensions.getHeight() * 0.2);
        performSwipe(startX, startY, startX, endY);
    }

    @Step("Swipe down on screen")
    protected void swipeDown() {
        var dimensions = driver.manage().window().getSize();
        int startX = dimensions.getWidth() / 2;
        int startY = (int) (dimensions.getHeight() * 0.2);
        int endY = (int) (dimensions.getHeight() * 0.8);
        performSwipe(startX, startY, startX, endY);
    }

    /** Tap at an absolute screen coordinate (e.g. the dimmed area outside a dialog). */
    protected void tapAt(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 0);
        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    private void performSwipe(int startX, int startY, int endX, int endY) {
        log.info("SWIPE ({},{})->({},{})", startX, startY, endX, endY);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 0);
        swipe.addAction(finger.createPointerMove(Duration.ZERO,
                PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(500),
                PointerInput.Origin.viewport(), endX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    // ──────────────────────────────────────────────
    // Platform helpers
    // ──────────────────────────────────────────────

    protected boolean isAndroid() {
        return ConfigManager.getInstance().getPlatform() == Platform.ANDROID;
    }

    protected boolean isIOS() {
        return ConfigManager.getInstance().getPlatform() == Platform.IOS;
    }

    /**
     * Find element using platform-specific locator.
     * Android uses resource ID, iOS uses accessibility ID.
     */
    protected WebElement findByPlatformId(String androidId, String iosAccessibilityId) {
        if (isAndroid()) {
            return driver.findElement(AppiumBy.id(androidId));
        } else {
            return driver.findElement(AppiumBy.accessibilityId(iosAccessibilityId));
        }
    }

    /**
     * Find elements using platform-specific locator. Returns empty list if not found.
     */
    protected List<WebElement> findAllByPlatformId(String androidId, String iosAccessibilityId) {
        if (isAndroid()) {
            return driver.findElements(AppiumBy.id(androidId));
        } else {
            return driver.findElements(AppiumBy.accessibilityId(iosAccessibilityId));
        }
    }

    /**
     * Get platform-specific By locator for waits.
     */
    protected By platformLocator(String androidId, String iosAccessibilityId) {
        return isAndroid() ? AppiumBy.id(androidId) : AppiumBy.accessibilityId(iosAccessibilityId);
    }

    /**
     * Cross-platform scroll to element.
     * Android: UiScrollable.scrollIntoView (native).
     * iOS: mobile:scroll with name (native XCUITest scroll — handles both directions,
     *      stops when found, no swipe loops, no coordinates).
     */
    protected void scrollToElement(String androidId, String iosAccessibilityId) {
        if (isAndroid()) {
            // Already on screen — no scroll needed.
            if (isDisplayedById(androidId)) return;
            // Scroll DIRECTLY to the target with UiScrollable.scrollIntoView — it drives the list
            // straight to the element (forward, then backward if needed) and stops ON it. This
            // replaces the old "6 blind forward swipes + reverse fallback", which overshot and then
            // scrolled back (the 4-5-down-then-2-3-up / Samsung back-and-forth churn).
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).setMaxSearchSwipes(12)" +
                        ".scrollIntoView(new UiSelector().resourceId(\"" + androidId + "\"))"));
            } catch (Exception ignored) {}
        } else {
            try {
                ((io.appium.java_client.AppiumDriver) driver).executeScript(
                        "mobile: scroll",
                        java.util.Map.of("name", iosAccessibilityId));
            } catch (Exception e) {
                log.warn("mobile:scroll failed for '{}': {}", iosAccessibilityId, e.getMessage());
            }
        }
    }

    /**
     * Cross-platform keyboard dismissal.
     */
    protected void hideKeyboardSafe() {
        try {
            if (isAndroid()) {
                ((io.appium.java_client.android.AndroidDriver) driver).hideKeyboard();
            } else {
                // iOS: tap outside the text field to dismiss keyboard
                var dimensions = driver.manage().window().getSize();
                PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                Sequence tap = new Sequence(finger, 0);
                // Tap near the top of the screen (above keyboard and text fields)
                tap.addAction(finger.createPointerMove(Duration.ZERO,
                        PointerInput.Origin.viewport(), dimensions.getWidth() / 2, 100));
                tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(Collections.singletonList(tap));
            }
        } catch (Exception ignored) {}
    }

    /**
     * Check if a native alert/dialog is present.
     * Android: android:id/message; iOS: XCUIElementTypeAlert.
     */
    protected boolean isAlertPresent() {
        try {
            if (isAndroid()) {
                return !driver.findElements(AppiumBy.id("android:id/message")).isEmpty();
            } else {
                return !driver.findElements(AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeAlert")).isEmpty();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get alert/dialog message text.
     */
    protected String getAlertMessage() {
        try {
            if (isAndroid()) {
                return driver.findElement(AppiumBy.id("android:id/message")).getText();
            } else {
                var alert = driver.findElements(AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeAlert/**/XCUIElementTypeStaticText"));
                // Second static text is typically the message (first is the title)
                return alert.size() > 1 ? alert.get(1).getText() : alert.get(0).getText();
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Dismiss alert dialog — taps OK/positive button.
     */
    protected void dismissAlert() {
        try {
            if (isAndroid()) {
                var btn = driver.findElements(AppiumBy.id("android:id/button1"));
                if (!btn.isEmpty()) { btn.get(0).click(); return; }
                btn = driver.findElements(AppiumBy.id("android:id/button2"));
                if (!btn.isEmpty()) btn.get(0).click();
            } else {
                var buttons = driver.findElements(AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeAlert/**/XCUIElementTypeButton"));
                if (!buttons.isEmpty()) buttons.get(buttons.size() - 1).click();
            }
        } catch (Exception ignored) {}
    }

    /**
     * Tap a specific button in an alert by label.
     */
    protected void tapAlertButton(String androidButtonId, String iosButtonLabel) {
        try {
            if (isAndroid()) {
                driver.findElement(AppiumBy.id(androidButtonId)).click();
            } else {
                driver.findElement(AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeAlert/**/XCUIElementTypeButton[`label == '" + iosButtonLabel + "'`]")).click();
            }
        } catch (Exception ignored) {}
    }

    /**
     * Find text by accessibility id on iOS, by resource id on Android.
     * Returns "" if not found.
     */
    protected String getTextByPlatformId(String androidId, String iosAccessibilityId) {
        var elements = findAllByPlatformId(androidId, iosAccessibilityId);
        if (elements.isEmpty()) return "";
        try {
            return elements.get(0).getText();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Override in each screen to capture text elements for snapshot system.
     */
    public Map<String, String> captureContent() {
        return Collections.emptyMap();
    }
}
