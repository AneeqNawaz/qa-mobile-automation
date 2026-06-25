package com.neuronation.pages.med.profile;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/**
 * Consent History page — Profile → Consent History (ConsentHistoryActivity).
 *
 * A list (help_recycler_view) of entries, each a title + content pair:
 *   helpItemTitle    e.g. "Newsletter Subscription Consent", "Data Retention Consent",
 *                    "Data Processing Consent"
 *   helpItemContent  e.g. "6/24/2026 3:43:56 PM\nChanged to Consent"  (or "… Dissent")
 *
 * Each consent shows a date/time and whether it was Changed to Consent or Dissent, which
 * reflects whether the matching checkbox was ticked on the email/terms screen during
 * registration.
 */
public class ConsentHistoryScreen extends BaseScreen {

    public static final String NEWSLETTER = "Newsletter";
    public static final String DATA_RETENTION = "Data Retention";
    public static final String DATA_PROCESSING = "Data Processing";

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Consent History")
    private WebElement toolbarTitle;

    @Step("Wait for Consent History screen")
    public void waitForScreen() {
        waitForVisible(toolbarTitle);
    }

    /** The Newsletter consent is written a moment after the others, so its row can render late.
     *  Wait until it appears so all entries are present before we read. */
    @Step("Wait for consent entries to finish loading")
    public void waitForEntriesLoaded() {
        if (isAndroid()) {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                    .until(d -> {
                        for (WebElement t : d.findElements(AppiumBy.id("nn.mobile.app.med:id/helpItemTitle"))) {
                            try {
                                String s = t.getText();
                                if (s != null && s.toLowerCase().contains("newsletter")) return true;
                            } catch (Exception ignored) {}
                        }
                        return false;
                    });
            return;
        }
        // iOS: wait for the Newsletter title StaticText to be present.
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                .until(d -> !d.findElements(AppiumBy.iOSNsPredicateString(
                        "type == \"XCUIElementTypeStaticText\" AND name CONTAINS \"Newsletter\"")).isEmpty());
    }

    /** Content text of the entry whose title contains {@code titleContains}, or "" if none. */
    @Step("Get consent entry content for: {titleContains}")
    public String getEntryContent(String titleContains) {
        if (!isAndroid()) return iosEntryContent(titleContains);
        var titles = driver.findElements(AppiumBy.id("nn.mobile.app.med:id/helpItemTitle"));
        var contents = driver.findElements(AppiumBy.id("nn.mobile.app.med:id/helpItemContent"));
        for (int i = 0; i < titles.size() && i < contents.size(); i++) {
            try {
                String title = titles.get(i).getText();
                if (title != null && title.toLowerCase().contains(titleContains.toLowerCase())) {
                    return contents.get(i).getText();
                }
            } catch (Exception ignored) {}
        }
        return "";
    }

    /** iOS: each entry is a title StaticText followed by a TextView whose value is
     *  "&lt;dd.MM.yyyy HH:mm:ss&gt;\nChanged to Consent|Dissent". Pair by nearest TextView below the title. */
    private String iosEntryContent(String titleContains) {
        var titles = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == \"XCUIElementTypeStaticText\" AND name CONTAINS \"" + titleContains + "\""));
        if (titles.isEmpty()) return "";
        int ty;
        try { ty = titles.get(0).getLocation().getY(); } catch (Exception e) { return ""; }
        WebElement best = null;
        int bestDy = Integer.MAX_VALUE;
        for (WebElement c : driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == \"XCUIElementTypeTextView\" AND value CONTAINS \"Changed to\""))) {
            try {
                int dy = c.getLocation().getY() - ty; // content sits just below its title
                if (dy >= 0 && dy < bestDy) { bestDy = dy; best = c; }
            } catch (Exception ignored) {}
        }
        try { return best != null ? best.getAttribute("value") : ""; }
        catch (Exception e) { return ""; }
    }

    /** true = "Changed to Consent", false = "Changed to Dissent" (or entry missing). */
    @Step("Is consent given for: {titleContains}")
    public boolean isConsentGiven(String titleContains) {
        String content = getEntryContent(titleContains);
        return content.contains("Consent") && !content.contains("Dissent");
    }

    /** Whether the entry's content carries a date (M/D/YYYY …). */
    @Step("Does consent entry show a date/time: {titleContains}")
    public boolean entryHasDateTime(String titleContains) {
        String c = getEntryContent(titleContains);
        // Android: M/D/YYYY h:mm:ss AM/PM  |  iOS: dd.MM.yyyy HH:mm:ss
        return c.matches("(?s).*\\d{1,2}/\\d{1,2}/\\d{4}.*")
                || c.matches("(?s).*\\d{1,2}\\.\\d{2}\\.\\d{4}.*");
    }

    @Step("Go back to Profile")
    public void tapBack() {
        driver.navigate().back();
    }
}
