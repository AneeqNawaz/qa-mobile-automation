package com.neuronation.pages.med.onboarding;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Schedule Review screen — "Personalise schedule" / "Review your settings"
 * Shows the selected schedule with weekday labels.
 * The "Confirm" button may require scrolling to be visible.
 */
public class ScheduleReviewScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Personalise schedule")
    private WebElement toolbarTitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/subtitle")
    @iOSXCUITFindBy(accessibility = "Review your settings")
    private WebElement subtitle;

    @AndroidFindBy(id = "nn.mobile.app.med:id/enable_switch")
    @iOSXCUITFindBy(accessibility = "enable_switch")
    private WebElement trainingReminderToggle;

    @Step("Tap 'Confirm' on Schedule Review screen")
    public void tapConfirm() {
        log.info("Swiping up and tapping 'Confirm' on Schedule Review screen");
        swipeUp();
        WebElement confirmButton = findByPlatformId("nn.mobile.app.med:id/confirm_button", "confirm_button");
        confirmButton.click();
    }

    @Step("Toggle training reminder switch on Schedule Review screen")
    public void tapTrainingReminderToggle() {
        log.info("Tapping training reminder toggle on Schedule Review screen");
        tap(trainingReminderToggle);
    }

    /** Per-day reminder times shown on this onboarding screen, as day→"HH:MM". Each day is a
     *  row: editcycleCellTitle (day name) + editcycleCellSubTitle (time). Scrolls to collect
     *  all 7 days (some sit below the fold). Only rows whose subtitle is HH:MM are kept, which
     *  excludes the "Training Reminder" header row. */
    @Step("Read per-day reminder times shown on the Schedule Review screen")
    public Map<String, String> getScheduleTimes() {
        Map<String, String> times = new LinkedHashMap<>();
        int prevSize = -1;
        for (int i = 0; i < 6; i++) {
            var titles = driver.findElements(AppiumBy.id("nn.mobile.app.med:id/editcycleCellTitle"));
            var subs = driver.findElements(AppiumBy.id("nn.mobile.app.med:id/editcycleCellSubTitle"));
            for (WebElement title : titles) {
                try {
                    String day = title.getText();
                    if (day == null || day.isEmpty() || times.containsKey(day)) continue;
                    int ty = title.getLocation().getY();
                    for (WebElement sub : subs) {
                        try {
                            String t = sub.getText();
                            // Subtitle is a 12-hour time like "09:00 AM" (sometimes a U+202F
                            // space before AM/PM). Match it loosely and store as 24h "HH:MM".
                            if (t != null && t.matches(".*\\d{1,2}:\\d{2}.*")
                                    && Math.abs(sub.getLocation().getY() - ty) < 80) {
                                times.put(day, to24h(t));
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
            if (times.size() >= 7 || times.size() == prevSize) break;
            prevSize = times.size();
            swipeUp();
        }
        log.info("Schedule Review per-day times read: {}", times);
        return times;
    }

    /** Normalize a displayed time ("09:00 AM", "9:00 PM", or already "21:00") to 24h "HH:MM". */
    private static String to24h(String raw) {
        String s = raw.replaceAll("[\\u00A0\\u202F\\u2007\\u2009]", " ").trim();
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("(\\d{1,2}):(\\d{2})\\s*([AaPp][Mm])?").matcher(s);
        if (!m.find()) return s;
        int h = Integer.parseInt(m.group(1));
        int min = Integer.parseInt(m.group(2));
        String ampm = m.group(3);
        if (ampm != null) {
            if (ampm.equalsIgnoreCase("AM")) { if (h == 12) h = 0; }
            else { if (h != 12) h += 12; }
        }
        return String.format("%02d:%02d", h, min);
    }

    @Step("Wait for Schedule Review screen to load")
    public void waitForScreen() {
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
            .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                platformLocator("nn.mobile.app.med:id/editListView", "Review your settings")));
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("subtitle", getTextByPlatformId("nn.mobile.app.med:id/subtitle", "Review your settings"));
        return content;
    }
}
