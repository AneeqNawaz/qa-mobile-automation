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
        if (isAndroid()) {
            if (!driver.findElements(AppiumBy.id("nn.mobile.app.med:id/accessibiletySwitch")).isEmpty()) {
                return; // already expanded
            }
            tapSetting("Special needs in training");
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                            AppiumBy.id("nn.mobile.app.med:id/accessibiletySwitch")));
            return;
        }
        // iOS: expanding reveals the 'Color Vision Deficiency' / 'Arithmetic Impairment' labels (visible).
        if (!driver.findElements(iosConstraintLabel("Color Vision Deficiency")).isEmpty()) return;
        tapSetting("Special needs in training");
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                    .until(d -> !d.findElements(iosConstraintLabel("Color Vision Deficiency")).isEmpty());
        } catch (Exception ignored) {}
    }

    /** iOS predicate for a visible special-needs constraint label StaticText. */
    private org.openqa.selenium.By iosConstraintLabel(String name) {
        return AppiumBy.iOSNsPredicateString(
                "type == \"XCUIElementTypeStaticText\" AND name == \"" + name + "\" AND visible == 1");
    }

    /** iOS: the constraint Switch for a label is the Switch nearest that label by Y (the
     *  special-needs switches sit on the same rows as their labels; exercise/NB switches are far). */
    private boolean iosConstraintOn(String labelName) {
        expandSpecialNeeds();
        // ONE page-source parse (vs a getLocation() round-trip per switch, which was slow): find the
        // label's Y and the nearest Switch's value.
        try {
            org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(
                            driver.getPageSource().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList all = doc.getElementsByTagName("*");
            int ly = Integer.MIN_VALUE;
            java.util.List<int[]> switches = new java.util.ArrayList<>();   // {y, value}
            for (int i = 0; i < all.getLength(); i++) {
                org.w3c.dom.Element e = (org.w3c.dom.Element) all.item(i);
                if (!"true".equals(e.getAttribute("visible"))) continue;
                int y;
                try { y = Integer.parseInt(e.getAttribute("y")); } catch (Exception ex) { continue; }
                String tag = e.getTagName();
                if ("XCUIElementTypeStaticText".equals(tag) && labelName.equals(e.getAttribute("name"))) {
                    ly = y;
                } else if ("XCUIElementTypeSwitch".equals(tag)) {
                    switches.add(new int[]{y, "1".equals(e.getAttribute("value")) ? 1 : 0});
                }
            }
            if (ly == Integer.MIN_VALUE) return false;
            int bestDy = Integer.MAX_VALUE, bestVal = -1;
            for (int[] s : switches) {
                int dy = Math.abs(s[0] - ly);
                if (dy < bestDy) { bestDy = dy; bestVal = s[1]; }
            }
            return bestVal == 1;
        } catch (Exception e) {
            log.warn("iOS special-needs parse failed: {}", e.getMessage());
            return false;
        }
    }

    @Step("Is Color Vision Deficiency switch ON")
    public boolean isColorVisionEnabled() {
        if (!isAndroid()) return iosConstraintOn("Color Vision Deficiency");
        expandSpecialNeeds();
        return "true".equals(colorVisionSwitch.getAttribute("checked"));
    }

    @Step("Is Arithmetic Impairment switch ON")
    public boolean isArithmeticEnabled() {
        if (!isAndroid()) return iosConstraintOn("Arithmetic Impairment");
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
        if (isAndroid()) {
            if (!driver.findElements(AppiumBy.className("android.widget.CheckBox")).isEmpty()) {
                return; // already expanded
            }
            tapSetting("Available Exercises");
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                            AppiumBy.className("android.widget.CheckBox")));
            return;
        }
        // iOS: expanded reveals the game list (StaticTexts like "Memobox").
        if (!driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == \"XCUIElementTypeStaticText\" AND name == \"Memobox\"")).isEmpty()) return;
        tapSetting("Available Exercises");
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                    .until(d -> !d.findElements(AppiumBy.iOSNsPredicateString(
                            "type == \"XCUIElementTypeStaticText\" AND name == \"Memobox\"")).isEmpty());
        } catch (Exception ignored) {}
    }

    /** Read every exercise's checked state in a SINGLE downward sweep: capture all visible
     *  checkboxes (name → checked) at each position, swipe down, repeat. Stops the instant all
     *  {@code expected} names have been seen (no extra confirming scrolls) — or when a swipe
     *  reveals nothing new (bottom reached). No per-item scrollIntoView (which thrashes
     *  top↔item). Text is the documented exception — the checkboxes expose no resource-id. */
    @Step("Read exercise checkbox states (single downward pass)")
    public java.util.Map<String, Boolean> getExerciseStates(java.util.Collection<String> expected) {
        if (!isAndroid()) return iosExerciseStates(expected);
        expandAvailableExercises();
        java.util.Map<String, Boolean> states = new java.util.LinkedHashMap<>();
        int prevSize = -1;
        for (int i = 0; i < 10; i++) {
            for (WebElement cb : driver.findElements(AppiumBy.className("android.widget.CheckBox"))) {
                try {
                    String name = cb.getText();
                    if (name != null && !name.isEmpty()) {
                        states.putIfAbsent(name, "true".equals(cb.getAttribute("checked")));
                    }
                } catch (Exception ignored) {}
            }
            boolean haveAll = expected != null && !expected.isEmpty()
                    && states.keySet().containsAll(expected);
            boolean noProgress = states.size() == prevSize; // last swipe revealed nothing new
            if (haveAll || noProgress) break;
            prevSize = states.size();
            swipeUp(); // scroll the list down to reveal more rows
        }
        log.info("Exercise states read ({} exercises): {}", states.size(), states);
        return states;
    }

    /** iOS exercise reader: expand, then scroll down reading every game→checked pair (mirrors the
     *  Android sweep), scroll back up. Each game is a StaticText; its checkbox is the same-row Switch
     *  (paired by Y). Uses ONE page-source parse per scroll position instead of dozens of per-element
     *  round-trips (those are slow on iOS). */
    private java.util.Map<String, Boolean> iosExerciseStates(java.util.Collection<String> expected) {
        expandAvailableExercises();
        java.util.Map<String, Boolean> states = new java.util.LinkedHashMap<>();
        int dry = 0;                       // consecutive passes that revealed no new game
        for (int i = 0; i < 14 && dry < 2; i++) {
            int before = states.size();
            parseVisibleExerciseRows(states, expected);
            if (expected != null && !expected.isEmpty() && states.keySet().containsAll(expected)) break;
            dry = (states.size() == before) ? dry + 1 : 0;   // tolerate one unrendered pass
            swipeUp();                     // scroll the list up to reveal the next rows
        }
        // scroll back up to the row header so the follow-up collapse tap lands cleanly
        for (int i = 0; i < 8 && driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == \"XCUIElementTypeStaticText\" AND name == \"Available Exercises\" AND visible == 1")).isEmpty(); i++) {
            swipeDown();
        }
        log.info("iOS exercise states read ({}): {}", states.size(), states);
        return states;
    }

    /** Parse the current page source once; for each on-screen game name (in {@code expected}) pair
     *  the nearest same-row Switch by Y and record checked (value == "1"). */
    private void parseVisibleExerciseRows(java.util.Map<String, Boolean> states,
                                          java.util.Collection<String> expected) {
        if (expected == null) return;
        try {
            org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(
                            driver.getPageSource().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            org.w3c.dom.NodeList all = doc.getElementsByTagName("*");
            java.util.List<int[]> switchYV = new java.util.ArrayList<>();          // {y, value}
            java.util.List<Object[]> gameY = new java.util.ArrayList<>();          // {name, y}
            for (int i = 0; i < all.getLength(); i++) {
                org.w3c.dom.Element e = (org.w3c.dom.Element) all.item(i);
                if (!"true".equals(e.getAttribute("visible"))) continue;
                int y;
                try { y = Integer.parseInt(e.getAttribute("y")); } catch (Exception ex) { continue; }
                // Appium's getPageSource() encodes the element type as the XML TAG NAME
                // (e.g. <XCUIElementTypeSwitch>), NOT a "type" attribute.
                String type = e.getTagName();
                if ("XCUIElementTypeSwitch".equals(type)
                        && e.getAttribute("name").startsWith("Available Exercises")) {
                    switchYV.add(new int[]{y, "1".equals(e.getAttribute("value")) ? 1 : 0});
                } else if ("XCUIElementTypeStaticText".equals(type)) {
                    String n = e.getAttribute("name");
                    if (expected.contains(n)) gameY.add(new Object[]{n, y});
                }
            }
            for (Object[] g : gameY) {
                String name = (String) g[0];
                int gy = (int) g[1];
                int bestDy = Integer.MAX_VALUE, bestVal = -1;
                for (int[] s : switchYV) {
                    int dy = Math.abs(s[0] - gy);
                    if (dy < bestDy) { bestDy = dy; bestVal = s[1]; }
                }
                if (bestVal >= 0 && bestDy < 45) states.putIfAbsent(name, bestVal == 1);
            }
        } catch (Exception e) {
            log.warn("iOS exercise parse failed: {}", e.getMessage());
        }
    }

    /** Names from {@code allNames} whose checkbox is unchecked (i.e. locked). */
    @Step("Collect locked (unchecked) exercises")
    public java.util.Set<String> getLockedExercises(java.util.List<String> allNames) {
        java.util.Map<String, Boolean> states = getExerciseStates(allNames);
        java.util.Set<String> locked = new java.util.LinkedHashSet<>();
        for (String name : allNames) {
            Boolean checked = states.get(name);
            if (checked != null && !checked) locked.add(name);
        }
        return locked;
    }

    /** Collapse the "Special needs in training" accordion by tapping its row (arrow toggle). */
    @Step("Collapse 'Special needs in training' accordion")
    public void collapseSpecialNeeds() {
        if (isAndroid()) {
            if (driver.findElements(AppiumBy.id("nn.mobile.app.med:id/accessibiletySwitch")).isEmpty()) {
                return; // already collapsed
            }
        } else if (driver.findElements(iosConstraintLabel("Color Vision Deficiency")).isEmpty()) {
            return; // already collapsed
        }
        tapSetting("Special needs in training");
    }

    /** Collapse the "Available Exercises" accordion. After a downward read its header is
     *  scrolled off the top, so bring the list back to the top first, then tap to collapse. */
    @Step("Collapse 'Available Exercises' accordion")
    public void collapseAvailableExercises() {
        if (isAndroid()) {
            if (driver.findElements(AppiumBy.className("android.widget.CheckBox")).isEmpty()) {
                return; // already collapsed
            }
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).scrollToBeginning(10)"));
            } catch (Exception ignored) {}
            tapSetting("Available Exercises");
            return;
        }
        // iOS: collapsed once the game list is gone (Memobox absent).
        if (driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == \"XCUIElementTypeStaticText\" AND name == \"Memobox\"")).isEmpty()) {
            return;
        }
        // bring the row title back into view (the sweep may have scrolled it off), then tap to collapse
        for (int i = 0; i < 8 && driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == \"XCUIElementTypeStaticText\" AND name == \"Available Exercises\" AND visible == 1")).isEmpty(); i++) {
            swipeDown();
        }
        tapSetting("Available Exercises");
    }

    // ── Generic expandable single-select row (Comparison Group, Training Adaptation, Language) ──
    // These rows share the same UI: tapping the row expands an inline list of clickable options
    // (TextViews with no resource-id). The selected option is highlighted in light green only —
    // there is NO checked/selected/color attribute exposed to Appium — so selection is verified
    // via the row subtitle (getSettingValue), and here we just read the offered option labels.

    /** Option labels currently shown inside an expanded row's content holder. The options are
     *  id-less TextViews under editcycleContentHolder; editcycleContentTitle (the description)
    /** Is an element with exactly this text on screen? Driver-level UiAutomator text() query —
     *  reliable, unlike element-scoped findElements which proved flaky here. */
    public boolean isOptionVisible(String label) {
        if (isAndroid()) {
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + label + "\")")).isEmpty();
        }
        // iOS: expanded options render as Buttons (the selected value is a StaticText). Match by name.
        return !driver.findElements(AppiumBy.iOSNsPredicateString(
                "name == \"" + label + "\" AND visible == 1")).isEmpty();
    }

    /** Expand {@code rowTitle}, then report which of {@code expected} option labels are visible.
     *  Selection isn't asserted (green highlight is visual-only) — the selected value comes from
     *  the subtitle. Expansion shows multiple options; a collapsed row shows only the selected. */
    @Step("Expand row {rowTitle} and read its options")
    public java.util.List<String> expandRowAndReadOptions(String rowTitle, java.util.List<String> expected) {
        tapSetting(rowTitle);
        // Wait for the accordion to render at least one option.
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(6))
                    .until(d -> expected.stream().anyMatch(this::isOptionVisible));
        } catch (Exception ignored) {}
        if (!isAndroid()) {
            // iOS exposes all expanded options in the tree at once (no scrolling). Read every
            // visible Button/StaticText name in ONE query and intersect with expected — far faster
            // than one predicate query per label + swipe-sweep (which thrashed and timed out before).
            java.util.Set<String> names = new java.util.LinkedHashSet<>();
            for (WebElement el : driver.findElements(AppiumBy.iOSNsPredicateString(
                    "(type == \"XCUIElementTypeButton\" OR type == \"XCUIElementTypeStaticText\") AND visible == 1"))) {
                try {
                    String n = el.getAttribute("name");
                    if (n != null && !n.isEmpty()) names.add(n);
                } catch (Exception ignored) {}
            }
            java.util.List<String> present = new java.util.ArrayList<>();
            for (String e : expected) if (names.contains(e)) present.add(e);
            log.info("Row '{}' options present {}/{}: {}", rowTitle, present.size(), expected.size(), present);
            return present;
        }
        // Scroll-sweep: some options (e.g. "Age group 80+") sit below the fold. Collect visible
        // labels, swipe down, repeat until all expected are seen or no new ones appear.
        java.util.LinkedHashSet<String> present = new java.util.LinkedHashSet<>();
        int prevSize = -1;
        for (int i = 0; i < 8; i++) {
            for (String label : expected) {
                if (isOptionVisible(label)) present.add(label);
            }
            if (present.containsAll(expected) || present.size() == prevSize) break;
            prevSize = present.size();
            swipeUp();
        }
        log.info("Row '{}' options present {}/{}: {}", rowTitle, present.size(), expected.size(), present);
        return new java.util.ArrayList<>(present);
    }

    /** Collapse an expandable row. The options sweep may have scrolled the row title off-screen,
     *  so scroll back to the top first, then tap the title. */
    @Step("Collapse row {rowTitle}")
    public void collapseRow(String rowTitle) {
        if (isAndroid()) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).scrollToBeginning(10)"));
            } catch (Exception ignored) {}
        } else {
            // iOS: the option sweep may have scrolled the row title off-screen — swipe back up to
            // it before tapping (UiScrollable doesn't exist on iOS, which threw before).
            for (int i = 0; i < 6; i++) {
                if (!driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == \"XCUIElementTypeStaticText\" AND name == \"" + rowTitle + "\" AND visible == 1")).isEmpty()) {
                    break;
                }
                swipeDown();
            }
        }
        tapSetting(rowTitle);
    }

    // ── Training Priorities (Attention popup → Understood → 4 domains) ──

    @Step("Open 'Training Priorities' (taps row → Attention popup OR direct domain expand)")
    public void openTrainingPriorities() {
        tapSetting("Training Priorities");
        // On some accounts an "Attention" popup (button1) appears first; on others the row expands
        // directly to the domains (domainTitle). Wait for EITHER so callers can branch.
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(6))
                    .until(d -> !d.findElements(AppiumBy.id("android:id/button1")).isEmpty()
                            || !d.findElements(AppiumBy.id("nn.mobile.app.med:id/domainTitle")).isEmpty());
        } catch (Exception ignored) {}
    }

    @Step("Is the 'Attention' popup shown")
    public boolean isAttentionPopupShown() {
        if (isAndroid()) {
            // Detect by the Understood button (android:id/button1). On a freshly-onboarded account
            // the dialog has a message + button but NO android:id/alertTitle, so we do NOT require
            // the title here. Presence-based to avoid isDisplayed() races during the entrance anim.
            return !driver.findElements(AppiumBy.id("android:id/button1")).isEmpty();
        }
        // iOS: native Alert ("Attention:") with an "Understood" button (verified via live session).
        return !driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == \"XCUIElementTypeButton\" AND name == \"Understood\"")).isEmpty();
    }

    @Step("Get Attention popup title")
    public String getAttentionTitle() {
        var els = driver.findElements(AppiumBy.id("android:id/alertTitle"));
        return els.isEmpty() ? "" : els.get(0).getText();
    }

    @Step("Get Attention popup message")
    public String getAttentionMessage() {
        if (isAndroid()) {
            var els = driver.findElements(AppiumBy.id("android:id/message"));
            return els.isEmpty() ? "" : els.get(0).getText();
        }
        var els = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == \"XCUIElementTypeStaticText\" AND name BEGINSWITH \"Your benefits\""));
        return els.isEmpty() ? "" : els.get(0).getText();
    }

    /** Dismiss the Attention popup by tapping the dimmed scrim just above the dialog
     *  (cancelable dialog). The dialog starts ~1/3 down the screen, so ~0.22·height lands in
     *  the scrim — clear of the status bar / toolbar above and the dialog below. Verified on
     *  device: tapping at 0.06·height instead hit the toolbar and misfired. */
    @Step("Dismiss Attention popup by tapping outside")
    public void dismissAttentionPopupOutside() {
        var size = driver.manage().window().getSize();
        tapAt(size.getWidth() / 2, (int) (size.getHeight() * 0.22));
        // Wait for the dialog to actually go away so a follow-up isAttentionPopupShown() is reliable.
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                    .until(d -> d.findElements(AppiumBy.id("android:id/button1")).isEmpty());
        } catch (Exception ignored) {}
    }

    @Step("Tap 'Understood' on the Attention popup")
    public void tapUnderstood() {
        if (isAndroid()) {
            driver.findElement(AppiumBy.id("android:id/button1")).click();
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                            AppiumBy.id("nn.mobile.app.med:id/domainTitle")));
            return;
        }
        // iOS: tap the alert's "Understood" button, then wait for a domain (Speed) to render.
        driver.findElement(AppiumBy.iOSNsPredicateString(
                "type == \"XCUIElementTypeButton\" AND name == \"Understood\"")).click();
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                    .until(d -> !d.findElements(AppiumBy.iOSNsPredicateString(
                            "type == \"XCUIElementTypeStaticText\" AND name == \"Speed\"")).isEmpty());
        } catch (Exception ignored) {}
    }

    /** Collapse the expanded Training Priorities accordion (tap its row/arrow). */
    @Step("Collapse 'Training Priorities' accordion")
    public void collapseTrainingPriorities() {
        if (isAndroid()) {
            if (driver.findElements(AppiumBy.id("nn.mobile.app.med:id/domainTitle")).isEmpty()) {
                return; // not expanded
            }
        } else if (driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == \"XCUIElementTypeStaticText\" AND name == \"Speed\"")).isEmpty()) {
            return; // not expanded
        }
        tapSetting("Training Priorities");
    }

    /** Domain titles shown after Understood (Speed / Attention / Memory / Reasoning). */
    @Step("Read Training Priorities domains")
    public java.util.List<String> getTrainingPriorityDomains() {
        java.util.List<String> domains = new java.util.ArrayList<>();
        if (isAndroid()) {
            for (WebElement el : driver.findElements(AppiumBy.id("nn.mobile.app.med:id/domainTitle"))) {
                try {
                    String t = el.getText();
                    if (t != null && !t.isEmpty()) domains.add(t);
                } catch (Exception ignored) {}
            }
        } else {
            // iOS: the four domains are StaticTexts inside the expanded row.
            for (String d : new String[]{"Speed", "Attention", "Memory", "Reasoning"}) {
                if (!driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == \"XCUIElementTypeStaticText\" AND name == \"" + d + "\"")).isEmpty()) {
                    domains.add(d);
                }
            }
        }
        log.info("Training Priorities domains ({}): {}", domains.size(), domains);
        return domains;
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
