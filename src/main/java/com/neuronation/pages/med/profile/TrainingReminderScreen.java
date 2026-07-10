package com.neuronation.pages.med.profile;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Training Reminder detail screen — Profile → Settings → Training Reminder.
 * Per-day reminder times + "Personalised training times" toggle.
 *
 * iOS: each day's row has a TextView whose value is "Every <Day> at HH:MM" — we
 * scope by that prefix and parse the time after " at ". One HTTP call per day.
 */
public class TrainingReminderScreen extends BaseScreen {

    public static final List<String> DAYS = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday",
            "Friday", "Saturday", "Sunday");

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeNavigationBar[`name == \"Training Reminder\"`]/XCUIElementTypeStaticText")
    private WebElement toolbarTitle;

    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeSwitch[`name == \"Personalised training times\"`]")
    private WebElement personalisedSwitch;

    // Per-day description TextViews (iOS only — Android uses text-based lookup)
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextView[`value BEGINSWITH \"Every Monday at\"`]")
    private WebElement mondayDesc;
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextView[`value BEGINSWITH \"Every Tuesday at\"`]")
    private WebElement tuesdayDesc;
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextView[`value BEGINSWITH \"Every Wednesday at\"`]")
    private WebElement wednesdayDesc;
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextView[`value BEGINSWITH \"Every Thursday at\"`]")
    private WebElement thursdayDesc;
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextView[`value BEGINSWITH \"Every Friday at\"`]")
    private WebElement fridayDesc;
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextView[`value BEGINSWITH \"Every Saturday at\"`]")
    private WebElement saturdayDesc;
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeTextView[`value BEGINSWITH \"Every Sunday at\"`]")
    private WebElement sundayDesc;

    @Step("Wait for Training Reminder screen to load")
    public void waitForScreen() {
        waitForVisible(toolbarTitle);
    }

    public boolean isDisplayed() {
        return isDisplayed(toolbarTitle);
    }

    @Step("Get 'Personalised training times' toggle state")
    public boolean isPersonalisedTimesOn() {
        if (isAndroid()) {
            var switches = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.Switch\")"));
            if (switches.isEmpty()) return false;
            try {
                return "true".equals(switches.get(0).getAttribute("checked"));
            } catch (Exception e) {
                return false;
            }
        }
        try { return "1".equals(personalisedSwitch.getAttribute("value")); }
        catch (Exception e) { return false; }
    }

    /** Get the reminder time HH:MM (24h) for a given day. "" if not found. */
    @Step("Get reminder time for: {day}")
    public String getReminderTime(String day) {
        if (isAndroid()) {
            var dayEls = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + day + "\")"));
            if (dayEls.isEmpty()) return "";
            int dayY = dayEls.get(0).getLocation().getY();
            var allTexts = driver.findElements(AppiumBy.className("android.widget.TextView"));
            for (var t : allTexts) {
                try {
                    String txt = t.getText();
                    // Match "HH:MM" or "HH:MM AM/PM" — return normalized 24h HH:MM
                    if (txt != null && txt.matches("\\d{1,2}:\\d{2}(\\s*[APap][Mm])?")) {
                        int ty = t.getLocation().getY();
                        if (Math.abs(ty - dayY) < 60) return normalizeTo24h(txt);
                    }
                } catch (Exception ignored) {}
            }
            return "";
        }
        return parseTimeFromDescription(descForDay(day));
    }

    /** Convert "06:00 PM" → "18:00", "9:00 AM" → "09:00", "09:00" → "09:00".
     *  iOS renders a U+202F narrow no-break space before AM/PM which Java's \s does NOT match, so
     *  normalize the unicode spaces to plain spaces first. */
    private String normalizeTo24h(String t) {
        t = t.replaceAll("[\\u00A0\\u202F\\u2007\\u2009]", " ").trim();
        var m = java.util.regex.Pattern.compile("^(\\d{1,2}):(\\d{2})\\s*([APap][Mm])?$").matcher(t);
        if (!m.matches()) return t;
        int h = Integer.parseInt(m.group(1));
        int mm = Integer.parseInt(m.group(2));
        String ampm = m.group(3);
        if (ampm != null) {
            ampm = ampm.toUpperCase();
            if (ampm.equals("PM") && h < 12) h += 12;
            else if (ampm.equals("AM") && h == 12) h = 0;
        }
        return String.format("%02d:%02d", h, mm);
    }

    /** Read all 7 days' reminder times. */
    @Step("Get all reminder times")
    public Map<String, String> getAllReminderTimes() {
        Map<String, String> times = new LinkedHashMap<>();
        for (String day : DAYS) times.put(day, getReminderTime(day));
        return times;
    }

    @Step("Tap back from Training Reminder")
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

    // ── Helpers ──

    private WebElement descForDay(String day) {
        switch (day) {
            case "Monday":    return mondayDesc;
            case "Tuesday":   return tuesdayDesc;
            case "Wednesday": return wednesdayDesc;
            case "Thursday":  return thursdayDesc;
            case "Friday":    return fridayDesc;
            case "Saturday":  return saturdayDesc;
            case "Sunday":    return sundayDesc;
            default:          throw new IllegalArgumentException("Unknown day: " + day);
        }
    }

    /** "Every Monday at 9:00 AM" → "09:00" (iOS renders 12h; normalize to 24h to match Android). */
    private String parseTimeFromDescription(WebElement el) {
        try {
            String v = el.getAttribute("value");
            if (v == null) return "";
            int idx = v.lastIndexOf(" at ");
            return idx < 0 ? "" : normalizeTo24h(v.substring(idx + 4).trim());
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("personalisedTimesOn", String.valueOf(isPersonalisedTimesOn()));
        for (String day : DAYS) content.put("time_" + day, getReminderTime(day));
        return content;
    }
}
