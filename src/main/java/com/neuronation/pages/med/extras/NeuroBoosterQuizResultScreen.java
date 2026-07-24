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
 * NeuroBooster quiz result screen ({@code NeuroBoosterQuizResultActivity}).
 * Pass and fail use the same ids but different copy; <b>pass adds a
 * {@code fireworks} overlay</b> that is absent on fail (the reliable pass signal).
 *
 * <ul>
 *   <li>PASS: {@code result_title}="Great job!", {@code fireworks} present,
 *       {@code action_button}="Continue" → returns to the Extras listing.</li>
 *   <li>FAIL: {@code result_title}="Let's try again", no fireworks,
 *       {@code action_button}="Try again" → returns to the NB detail page.</li>
 * </ul>
 * Either outcome marks the tile discovered (attempting, not passing, is what counts).
 */
public class NeuroBoosterQuizResultScreen extends BaseScreen {

    static final String ID_FIREWORKS = "nn.mobile.app.med:id/fireworks";
    static final String ID_TITLE     = "nn.mobile.app.med:id/result_title";
    static final String ID_IMAGE     = "nn.mobile.app.med:id/result_image";
    static final String ID_RIBBON    = "nn.mobile.app.med:id/quiz_ribbon";
    static final String ID_SCORE     = "nn.mobile.app.med:id/score_label";
    static final String ID_CONTENT   = "nn.mobile.app.med:id/result_content";
    static final String ID_ACTION    = "nn.mobile.app.med:id/action_button";

    // iOS: no ids. Ribbon Image `quiz_ribbon` is present on both outcomes (anchor). Result
    // image `quiz_result_success`/`quiz_result_fail`; action Button `Continue`(pass)/`Try again`(fail).
    static final String IOS_RIBBON        = "quiz_ribbon";
    static final String IOS_IMAGE_PASS    = "quiz_result_pass";
    static final String IOS_IMAGE_FAIL    = "quiz_result_fail";
    static final String IOS_ACTION_PASS   = "Continue";
    static final String IOS_ACTION_FAIL   = "Try again";
    private static final java.util.regex.Pattern SCORE = java.util.regex.Pattern.compile("\\d+\\s*/\\s*\\d+\\s*points");

    @Step("Wait for quiz result screen")
    public void waitForScreen() {
        By anchor = isIOS() ? AppiumBy.accessibilityId(IOS_RIBBON) : AppiumBy.id(ID_ACTION);
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.presenceOfElementLocated(anchor));
    }

    @Step("Check quiz result screen displayed")
    public boolean isDisplayed() {
        if (isIOS()) return !driver.findElements(AppiumBy.accessibilityId(IOS_RIBBON)).isEmpty();
        return !driver.findElements(AppiumBy.id(ID_ACTION)).isEmpty();
    }

    /** Pass detected by the fireworks overlay (Android) / the Continue action button (iOS). */
    @Step("Check quiz result is a pass")
    public boolean isPass() {
        if (isIOS()) {
            return !driver.findElements(AppiumBy.accessibilityId(IOS_IMAGE_PASS)).isEmpty()
                    || !driver.findElements(AppiumBy.accessibilityId(IOS_ACTION_PASS)).isEmpty();
        }
        return !driver.findElements(AppiumBy.id(ID_FIREWORKS)).isEmpty();
    }

    @Step("Get quiz result title")
    public String getResultTitle() {
        if (isIOS()) return topStaticTextIOS(); // the title sits at the top of the result card
        return getTextByPlatformId(ID_TITLE, ID_TITLE);
    }

    @Step("Get quiz score label")
    public String getScoreLabel() {
        if (isIOS()) {
            // The result screen plays a confetti / "tada" animation that keeps iOS from quiescing, so a
            // single accessibility snapshot often returns BEFORE the score StaticText ("5/5 points") is
            // settled in the tree (the confetti burst is right over the score ribbon) → empty read →
            // false "result score present" even though the score is clearly visible (build #82 flow4).
            // POLL until the score appears — the confetti thins within a few seconds — instead of
            // reading once and racing the animation.
            try {
                return new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(400)).until(d -> {
                    for (WebElement t : d.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
                        String n = t.getAttribute("name");
                        if (n != null && SCORE.matcher(n).find()) return n;
                    }
                    return null;
                });
            } catch (Exception e) {
                return "";
            }
        }
        return getTextByPlatformId(ID_SCORE, ID_SCORE);
    }

    @Step("Get quiz result content")
    public String getResultContent() {
        if (isIOS()) {
            // No stable id; content is the longest StaticText that isn't the title, the score, or the
            // action-button label (title/score/action are short; the message is a full sentence).
            String title = getResultTitle(), action = getActionLabel(), best = "";
            for (WebElement t : driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
                String n = nz(t.getAttribute("name"));
                if (n.isEmpty() || SCORE.matcher(n).find() || n.equals(title) || n.equals(action)) continue;
                if (n.length() > best.length()) best = n;
            }
            return best;
        }
        return getTextByPlatformId(ID_CONTENT, ID_CONTENT);
    }

    @Step("Get quiz result action-button label")
    public String getActionLabel() {
        if (isIOS()) { WebElement a = actionButtonIOS(); return a == null ? "" : nz(a.getAttribute("name")); }
        return getTextByPlatformId(ID_ACTION, ID_ACTION);
    }

    @Step("Check quiz result image present")
    public boolean hasImage() {
        if (isIOS()) return !driver.findElements(AppiumBy.accessibilityId(IOS_IMAGE_PASS)).isEmpty()
                || !driver.findElements(AppiumBy.accessibilityId(IOS_IMAGE_FAIL)).isEmpty();
        return !driver.findElements(AppiumBy.id(ID_IMAGE)).isEmpty();
    }

    @Step("Check quiz ribbon present")
    public boolean hasRibbon() {
        if (isIOS()) return !driver.findElements(AppiumBy.accessibilityId(IOS_RIBBON)).isEmpty();
        return !driver.findElements(AppiumBy.id(ID_RIBBON)).isEmpty();
    }

    /** "Continue" (pass) → Extras listing, or "Try again" (fail) → NB detail. */
    @Step("Tap quiz result action button")
    public void tapAction() {
        if (isIOS()) { WebElement a = actionButtonIOS(); if (a != null) a.click(); return; }
        driver.findElement(AppiumBy.id(ID_ACTION)).click();
    }

    // ---- iOS helpers ----
    private WebElement actionButtonIOS() {
        var pass = driver.findElements(AppiumBy.accessibilityId(IOS_ACTION_PASS));
        if (!pass.isEmpty()) return pass.get(0);
        var fail = driver.findElements(AppiumBy.accessibilityId(IOS_ACTION_FAIL));
        return fail.isEmpty() ? null : fail.get(0);
    }

    private String topStaticTextIOS() {
        WebElement best = null; int bestY = Integer.MAX_VALUE;
        for (WebElement t : driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
            int y = t.getLocation().getY();
            if (y < bestY) { best = t; bestY = y; }
        }
        return best == null ? "" : nz(best.getAttribute("name"));
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
