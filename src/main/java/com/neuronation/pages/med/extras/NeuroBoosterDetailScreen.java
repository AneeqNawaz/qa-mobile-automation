package com.neuronation.pages.med.extras;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * NeuroBooster detail page ({@code NeuroBoosterActivity}). One page object for all
 * three content types (EXERCISE, KNOWLEDGE, COGNITIVE_HEALTH) — same activity +
 * ids; they differ only in the primary-CTA label (asserted from data, not here).
 *
 * <p>The carousel dots and countdown/step badge on EXERCISE tiles are custom-drawn
 * inside the single {@code image} view and are NOT accessibility nodes — only
 * {@link #hasImage()} is assertable (see extras-neurobooster-locators.md).
 */
public class NeuroBoosterDetailScreen extends BaseScreen {

    static final String ID_HEADING       = "nn.mobile.app.med:id/main_toolbar_title";
    static final String ID_IMAGE         = "nn.mobile.app.med:id/image";
    static final String ID_BODY          = "nn.mobile.app.med:id/content";
    static final String ID_CTA_PRIMARY   = "nn.mobile.app.med:id/cta_button";
    static final String ID_CTA_SECONDARY = "nn.mobile.app.med:id/cta_button_secondary";
    static final String DESC_BACK        = "Navigate up";

    static final String IOS_HEADING = "neurobooster_detail_heading"; // placeholder
    static final String IOS_BODY    = "neurobooster_detail_body";    // placeholder
    // iOS: no stable ids — CTAs are Buttons whose accessibility name == the label text.
    // Secondary is a fixed label; the primary is the bottom-most content Button (below it).
    static final String IOS_SECONDARY_LABEL = "Let me do this later";
    static final String IOS_BACK_BUTTON     = "BackButton";

    @Step("Wait for NeuroBooster detail page to load")
    public void waitForScreen() {
        // iOS anchor = the always-present secondary CTA ("Let me do this later").
        By anchor = isIOS() ? AppiumBy.accessibilityId(IOS_SECONDARY_LABEL) : AppiumBy.id(ID_CTA_PRIMARY);
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.presenceOfElementLocated(anchor));
    }

    @Step("Check if NeuroBooster detail page is displayed")
    public boolean isDisplayed() {
        if (isIOS()) return !driver.findElements(AppiumBy.accessibilityId(IOS_SECONDARY_LABEL)).isEmpty();
        return !findAllByPlatformId(ID_CTA_PRIMARY, ID_CTA_PRIMARY).isEmpty();
    }

    @Step("Get NeuroBooster detail heading")
    public String getHeading() {
        if (isIOS()) { // heading == the NavigationBar title (the tile subtitle)
            var bars = driver.findElements(AppiumBy.className("XCUIElementTypeNavigationBar"));
            return bars.isEmpty() ? "" : (bars.get(0).getAttribute("name") == null ? "" : bars.get(0).getAttribute("name"));
        }
        return getTextByPlatformId(ID_HEADING, IOS_HEADING);
    }

    /**
     * iOS primary CTA = the bottom-most content Button (it sits below the secondary
     * "Let me do this later"); excludes the top BackButton. Its accessibility name is the
     * CTA label ("Continue to video and quiz" / "I've completed this, continue").
     */
    private WebElement primaryCtaIOS() {
        WebElement best = null; int bestY = -1;
        for (WebElement b : driver.findElements(AppiumBy.className("XCUIElementTypeButton"))) {
            String name = b.getAttribute("name");
            if (name == null || name.equals(IOS_SECONDARY_LABEL) || name.equals(IOS_BACK_BUTTON)) continue;
            int y = b.getLocation().getY();
            if (y > bestY) { best = b; bestY = y; }
        }
        return best;
    }

    /** Body text can render async; wait up to 10s for non-empty, then read. */
    @Step("Get NeuroBooster detail body text")
    public String getBodyText() {
        if (isIOS()) {
            // No stable id on iOS; the body is the longest StaticText (a paragraph — the
            // heading and CTA labels are short). Wait up to 10s for it to render.
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> longestStaticTextIOS().length() > 20);
            } catch (Exception ignored) { }
            return longestStaticTextIOS();
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
                var els = findAllByPlatformId(ID_BODY, IOS_BODY);
                return !els.isEmpty() && els.get(0).getText() != null && !els.get(0).getText().isEmpty();
            });
        } catch (Exception ignored) { /* return whatever is there */ }
        return getTextByPlatformId(ID_BODY, IOS_BODY);
    }

    /** The longest StaticText on screen (iOS detail body has no stable id). */
    private String longestStaticTextIOS() {
        String best = "";
        for (WebElement t : driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
            String s = t.getAttribute("value");
            if (s == null || s.isEmpty()) s = t.getAttribute("name");
            if (s != null && s.length() > best.length()) best = s;
        }
        return best;
    }

    @Step("Get NeuroBooster primary CTA text")
    public String getPrimaryCtaText() {
        if (isIOS()) {
            WebElement cta = primaryCtaIOS();
            String n = cta == null ? null : cta.getAttribute("name");
            return n == null ? "" : n;
        }
        return getTextByPlatformId(ID_CTA_PRIMARY, ID_CTA_PRIMARY);
    }

    @Step("Get NeuroBooster secondary CTA text")
    public String getSecondaryCtaText() {
        if (isIOS()) return IOS_SECONDARY_LABEL;
        return getTextByPlatformId(ID_CTA_SECONDARY, ID_CTA_SECONDARY);
    }

    @Step("Check NeuroBooster detail image is present")
    public boolean hasImage() { return !findAllByPlatformId(ID_IMAGE, ID_IMAGE).isEmpty(); }

    /** Read-only exit — does NOT increment the discovered count. */
    @Step("Go back from NeuroBooster detail page")
    public void goBack() {
        driver.findElement(AppiumBy.accessibilityId(DESC_BACK)).click();
    }

    /**
     * EXERCISE/KNOWLEDGE: marks the tile discovered and returns to the listing.
     * COGNITIVE_HEALTH: launches the video + quiz flow.
     */
    @Step("Tap NeuroBooster primary CTA")
    public void tapPrimaryCta() {
        if (isIOS()) { WebElement c = primaryCtaIOS(); if (c != null) c.click(); return; }
        findByPlatformId(ID_CTA_PRIMARY, ID_CTA_PRIMARY).click();
    }

    /** "Let me do this later" — exits without completing. */
    @Step("Tap NeuroBooster secondary CTA")
    public void tapSecondaryCta() {
        if (isIOS()) { driver.findElement(AppiumBy.accessibilityId(IOS_SECONDARY_LABEL)).click(); return; }
        findByPlatformId(ID_CTA_SECONDARY, ID_CTA_SECONDARY).click();
    }
}
