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
 * Settings screen — shows onboarding selections and training preferences.
 * Accessible from Profile → Settings.
 *
 * Each row has a stable title text on the left and a value text on the right.
 * iOS values follow stable patterns (Age group X-Y, HH:MM, X/Y, Ask me / Don't ask me, …)
 * so we declare each value with a predicate-scoped iOSClassChain — one round-trip per read.
 *
 * Android side uses the existing editcycleCellTitle / editcycleCellSubTitle pair.
 */
public class SettingsScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/main_toolbar_title")
    @iOSXCUITFindBy(accessibility = "Settings")
    private WebElement toolbarTitle;

    // ── Per-row VALUE locators (iOS uses pattern predicates) ──

    // "Age group 31-40", "Age group 41-50", …
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name BEGINSWITH \"Age group\" AND visible == 1`]")
    private WebElement comparisonGroupValue;

    // "English (United Kingdom)" or "Deutsch"
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`(name BEGINSWITH \"English\" OR name == \"Deutsch\") AND visible == 1`]")
    private WebElement languageValue;

    // "Ask me" or "Don't ask me"
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`(name == \"Ask me\" OR name == \"Don't ask me\") AND visible == 1`]")
    private WebElement trainingAdaptationValue;

    // "X/Y" — only "Available Exercises" matches this on Settings
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name MATCHES \"[0-9]+/[0-9]+\" AND visible == 1`]")
    private WebElement availableExercisesValue;

    // "HH:MM" — only NeuroBooster shows time on Settings
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`name MATCHES \"[0-9]{1,2}:[0-9]{2}\" AND visible == 1`]")
    private WebElement neuroBoosterTimeValue;

    // "Recommended" or "Custom"
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeStaticText[`(name == \"Recommended\" OR name == \"Custom\") AND visible == 1`]")
    private WebElement trainingPrioritiesValue;

    // ── Switches ──
    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeSwitch[`name BEGINSWITH \"Special needs in training\"`]")
    private WebElement specialNeedsSwitch;

    @iOSXCUITFindBy(iOSClassChain = "**/XCUIElementTypeSwitch[`name BEGINSWITH \"NeuroBooster\"`]")
    private WebElement neuroBoosterSwitch;

    // ── Special-needs accordion (expanded under "Special needs in training") ──
    @AndroidFindBy(id = "nn.mobile.app.med:id/accessibiletySwitch")
    @iOSXCUITFindBy(accessibility = "accessibiletySwitch")   // iOS unverified (WDA blocker)
    private WebElement colorVisionSwitch;

    @AndroidFindBy(id = "nn.mobile.app.med:id/dyscalculiaSwitch")
    @iOSXCUITFindBy(accessibility = "dyscalculiaSwitch")     // iOS unverified (WDA blocker)
    private WebElement arithmeticSwitch;

    // ── Lifecycle ──

    @Step("Wait for Settings screen to load")
    public void waitForScreen() {
        waitForVisible(toolbarTitle);
    }

    @Step("Check if Settings screen is displayed")
    public boolean isDisplayed() {
        return isDisplayed(toolbarTitle);
    }

    @Step("Get toolbar title")
    public String getToolbarTitle() {
        return getText(toolbarTitle);
    }

    // ── Generic readers (Android uses ID pairing; iOS reads named field) ──

    @Step("Get setting value for: {settingTitle}")
    public String getSettingValue(String settingTitle) {
        log.info("Getting setting value for: {}", settingTitle);
        if (isAndroid()) {
            return readAndroidValue(settingTitle);
        }
        // iOS: route to the named field for known titles, else generic "" (no fallback search).
        switch (settingTitle) {
            case "Comparison Group":   return getComparisonGroup();
            case "Language":           return getLanguage();
            case "Training Adaptation":return getTrainingAdaptation();
            case "Available Exercises":return getAvailableExercises();
            case "NeuroBooster":       return getNeuroBoosterTime();
            case "Training Priorities":return getTrainingPriorities();
            default:                   return "";
        }
    }

    @Step("Get switch state for: {settingName}")
    public boolean isSwitchOn(String settingName) {
        if (isAndroid()) {
            var switches = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().className(\"android.widget.Switch\")"));
            var titles = driver.findElements(AppiumBy.id("nn.mobile.app.med:id/editcycleCellTitle"));
            for (var t : titles) {
                try {
                    if (settingName.equals(t.getText())) {
                        int ty = t.getLocation().getY();
                        for (var sw : switches) {
                            int sy = sw.getLocation().getY();
                            if (Math.abs(sy - ty) < 60) {
                                return "true".equals(sw.getAttribute("checked"));
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            return false;
        }
        switch (settingName) {
            case "Special needs in training": return isSpecialNeedsEnabled();
            case "NeuroBooster":              return isNeuroBoosterEnabled();
            default:                          return false;
        }
    }

    // ── Per-setting getters (iOS reads named field directly = 1 HTTP call each) ──

    @Step("Get Comparison Group from Settings")
    public String getComparisonGroup() {
        return isAndroid() ? readAndroidValue("Comparison Group") : safeText(comparisonGroupValue);
    }

    @Step("Get Language from Settings")
    public String getLanguage() {
        return isAndroid() ? readAndroidValue("Language") : safeText(languageValue);
    }

    @Step("Get Training Adaptation from Settings")
    public String getTrainingAdaptation() {
        return isAndroid() ? readAndroidValue("Training Adaptation") : safeText(trainingAdaptationValue);
    }

    @Step("Get Available Exercises count from Settings")
    public String getAvailableExercises() {
        return isAndroid() ? readAndroidValue("Available Exercises") : safeText(availableExercisesValue);
    }

    @Step("Get NeuroBooster time from Settings")
    public String getNeuroBoosterTime() {
        return isAndroid() ? readAndroidValue("NeuroBooster") : safeText(neuroBoosterTimeValue);
    }

    @Step("Get Training Priorities from Settings")
    public String getTrainingPriorities() {
        return isAndroid() ? readAndroidValue("Training Priorities") : safeText(trainingPrioritiesValue);
    }

    /** "Special needs in training" is an inline accordion. Expand it (idempotent) so the
     *  Color Vision / Arithmetic switches are present. */
    @Step("Expand 'Special needs in training' accordion")
    public void expandSpecialNeeds() {
        if (!driver.findElements(AppiumBy.id("nn.mobile.app.med:id/accessibiletySwitch")).isEmpty()) {
            return; // already expanded
        }
        tapSetting("Special needs in training");
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                        AppiumBy.id("nn.mobile.app.med:id/accessibiletySwitch")));
    }

    @Step("Is Color Vision Deficiency switch ON")
    public boolean isColorVisionEnabled() {
        expandSpecialNeeds();
        return "true".equals(colorVisionSwitch.getAttribute("checked"));
    }

    @Step("Is Arithmetic Impairment switch ON")
    public boolean isArithmeticEnabled() {
        expandSpecialNeeds();
        return "true".equals(arithmeticSwitch.getAttribute("checked"));
    }

    @Step("Is 'Special needs in training' enabled (either constraint on)")
    public boolean isSpecialNeedsEnabled() {
        if (isAndroid()) {
            return isColorVisionEnabled() || isArithmeticEnabled();
        }
        try { return "1".equals(specialNeedsSwitch.getAttribute("value")); }
        catch (Exception e) { return false; }
    }

    /** "Available Exercises" is an inline accordion of CheckBox rows (no resource-id; the
     *  exercise name is the only stable identifier). Expand it (idempotent). */
    @Step("Expand 'Available Exercises' accordion")
    public void expandAvailableExercises() {
        if (!driver.findElements(AppiumBy.className("android.widget.CheckBox")).isEmpty()) {
            return; // already expanded
        }
        tapSetting("Available Exercises");
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                        AppiumBy.className("android.widget.CheckBox")));
    }

    /** Read every exercise's checked state in a SINGLE downward sweep: capture all visible
     *  checkboxes (name → checked) at each position, swipe down, repeat until two passes add
     *  nothing new (bottom reached). No per-item scrollIntoView (which thrashes top↔item).
     *  Text is the documented exception — the checkboxes expose no resource-id. */
    @Step("Read all exercise checkbox states (single downward pass)")
    public java.util.Map<String, Boolean> getExerciseStates() {
        expandAvailableExercises();
        java.util.Map<String, Boolean> states = new java.util.LinkedHashMap<>();
        int lastSize = -1, stableRounds = 0;
        for (int i = 0; i < 8 && stableRounds < 2; i++) {
            for (WebElement cb : driver.findElements(AppiumBy.className("android.widget.CheckBox"))) {
                try {
                    String name = cb.getText();
                    if (name != null && !name.isEmpty()) {
                        states.putIfAbsent(name, "true".equals(cb.getAttribute("checked")));
                    }
                } catch (Exception ignored) {}
            }
            if (states.size() == lastSize) stableRounds++;
            else { stableRounds = 0; lastSize = states.size(); }
            swipeUp(); // scroll the list down to reveal more rows
        }
        log.info("Exercise states read ({} exercises): {}", states.size(), states);
        return states;
    }

    /** Names from {@code allNames} whose checkbox is unchecked (i.e. locked). */
    @Step("Collect locked (unchecked) exercises")
    public java.util.Set<String> getLockedExercises(java.util.List<String> allNames) {
        java.util.Map<String, Boolean> states = getExerciseStates();
        java.util.Set<String> locked = new java.util.LinkedHashSet<>();
        for (String name : allNames) {
            Boolean checked = states.get(name);
            if (checked != null && !checked) locked.add(name);
        }
        return locked;
    }

    @Step("Is 'NeuroBooster' switch ON")
    public boolean isNeuroBoosterEnabled() {
        if (isAndroid()) {
            // NB row has no subtitle — tap row, read enable_switch, then back.
            // Back from NB detail lands on Profile (not Settings), which is fine since NB is the
            // last Settings check — caller can proceed directly to logout from Profile.
            tapSetting("NeuroBooster");
            boolean checked = false;
            try {
                new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                        .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                                AppiumBy.id("nn.mobile.app.med:id/enable_switch")));
                checked = "true".equals(driver.findElement(
                        AppiumBy.id("nn.mobile.app.med:id/enable_switch")).getAttribute("checked"));
                log.info("Android: NB enable_switch checked={}", checked);
            } catch (Exception e) {
                log.warn("Could not read NeuroBooster switch: {}", e.getMessage());
            }
            try { driver.navigate().back(); } catch (Exception ignored) {}
            return checked;
        }
        // iOS: tap NB row → detail screen → find named switch → read → back.
        // The Settings-row Switch with name "NeuroBooster" represents enable state but is wrapped
        // with the row click target. Tapping the StaticText "NeuroBooster" navigates to detail.
        log.info("iOS: tapping NeuroBooster row");
        try {
            // Capture current state first as fallback (Settings-row switch)
            String settingsValue = "0";
            try { settingsValue = neuroBoosterSwitch.getAttribute("value"); } catch (Exception ignored) {}

            tapSetting("NeuroBooster");
            // On NB detail, look for a Switch with a name containing "NeuroBooster" or near "Enable" label
            var switches = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                    .until(d -> {
                        var list = d.findElements(AppiumBy.className("XCUIElementTypeSwitch"));
                        return list.isEmpty() ? null : list;
                    });
            org.openqa.selenium.WebElement target = null;
            for (var s : switches) {
                String name = s.getAttribute("name");
                if (name != null && name.toLowerCase().contains("neurobooster")) { target = s; break; }
            }
            if (target == null && !switches.isEmpty()) target = switches.get(0);
            String detailValue = target != null ? target.getAttribute("value") : settingsValue;
            log.info("iOS: NB detail switch (name={}) value={}", target != null ? target.getAttribute("name") : "n/a", detailValue);

            // Back to Settings
            try {
                driver.findElement(AppiumBy.iOSClassChain(
                        "**/XCUIElementTypeNavigationBar/XCUIElementTypeButton[`name == \"BackButton\"`]")).click();
            } catch (Exception e) {
                try { driver.navigate().back(); } catch (Exception ignored) {}
            }
            return "1".equals(detailValue);
        } catch (Exception e) {
            log.warn("iOS NB tap failed, falling back to Settings switch: {}", e.getMessage());
            try { return "1".equals(neuroBoosterSwitch.getAttribute("value")); }
            catch (Exception ex) { return false; }
        }
    }

    // ── Row navigation ──

    @Step("Tap settings row: {settingTitle}")
    public void tapSetting(String settingTitle) {
        log.info("Tapping settings row: {}", settingTitle);
        if (isAndroid()) {
            var titles = driver.findElements(AppiumBy.id("nn.mobile.app.med:id/editcycleCellTitle"));
            for (var t : titles) {
                try {
                    if (settingTitle.equals(t.getText())) { t.click(); return; }
                } catch (Exception ignored) {}
            }
            throw new org.openqa.selenium.NoSuchElementException("Settings row not found: " + settingTitle);
        } else {
            driver.findElement(AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeStaticText[`name == \"" + settingTitle + "\"`]")).click();
        }
    }

    @Step("Tap 'Training Reminder' row")
    public void tapTrainingReminder() {
        tapSetting("Training Reminder");
    }

    @Step("Tap back to return to Profile")
    public void tapBack() {
        log.info("Tapping back from Settings");
        driver.navigate().back();
    }

    // ── Helpers ──

    private String safeText(WebElement el) {
        try { return el.getAttribute("name"); }
        catch (Exception e) { return ""; }
    }

    private String readAndroidValue(String settingTitle) {
        // Android title + subtitle share the same row (same Y, different X).
        var titles = driver.findElements(AppiumBy.id("nn.mobile.app.med:id/editcycleCellTitle"));
        var subtitles = driver.findElements(AppiumBy.id("nn.mobile.app.med:id/editcycleCellSubTitle"));
        for (var title : titles) {
            try {
                if (settingTitle.equals(title.getText())) {
                    int titleY = title.getLocation().getY();
                    for (var sub : subtitles) {
                        try {
                            String subText = sub.getText();
                            if (subText != null && !subText.isEmpty()) {
                                int subY = sub.getLocation().getY();
                                if (Math.abs(subY - titleY) <= 80) return subText;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }
        return "";
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("toolbarTitle", getTextSafe(toolbarTitle));
        content.put("comparisonGroup", getComparisonGroup());
        content.put("language", getLanguage());
        content.put("trainingAdaptation", getTrainingAdaptation());
        content.put("availableExercises", getAvailableExercises());
        content.put("neuroBoosterTime", getNeuroBoosterTime());
        content.put("neuroBoosterEnabled", String.valueOf(isNeuroBoosterEnabled()));
        content.put("specialNeedsEnabled", String.valueOf(isSpecialNeedsEnabled()));
        content.put("trainingPriorities", getTrainingPriorities());
        return content;
    }
}
