package com.neuronation.pages.med.extras;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * NeuroBooster video screen ({@code MedNeuroBoosterVideoActivity}, ExoPlayer),
 * reached from a COGNITIVE_HEALTH detail via "Continue to video and quiz".
 *
 * <p>The transport controls ARE real elements in Appium's page source (visible in
 * Appium Inspector) — note {@code adb uiautomator dump} omits them, which is
 * misleading. They only appear while the controller is shown, so we reveal it (click
 * the player view) before reading/clicking. Fast-forward is <b>duration-based</b>:
 * read {@code exo_duration}/{@code exo_position} and click the +15s button
 * {@code ceil(remaining / 15)} times, by locator (no coordinates).
 */
public class NeuroBoosterVideoScreen extends BaseScreen {

    static final String ID_PLAYER    = "nn.mobile.app.med:id/exoplayervideoview";
    static final String ID_SUBTITLES = "nn.mobile.app.med:id/exo_subtitles";
    static final String ID_SHUTTER   = "nn.mobile.app.med:id/exo_shutter"; // loader — present until the video is ready
    static final String ID_CLOSE     = "nn.mobile.app.med:id/closeVideoButton"; // the "cross" (X)

    // Transport controls (resource-id + accessibility id). The app names the skip
    // buttons *_with_amount ("Fast forward 15 seconds" / "Rewind 5 seconds").
    static final String ID_FORWARD    = "nn.mobile.app.med:id/exo_ffwd_with_amount";
    static final String DESC_FORWARD  = "Fast forward 15 seconds";
    static final String ID_REWIND     = "nn.mobile.app.med:id/exo_rew_with_amount";
    static final String DESC_REWIND   = "Rewind 5 seconds";
    static final String ID_PLAY_PAUSE = "nn.mobile.app.med:id/exo_play_pause";
    static final String ID_PREV       = "nn.mobile.app.med:id/exo_prev";       // skip to previous
    static final String ID_NEXT       = "nn.mobile.app.med:id/exo_next";       // skip to next
    static final String ID_POSITION   = "nn.mobile.app.med:id/exo_position";   // current time "MM:SS"
    static final String ID_DURATION   = "nn.mobile.app.med:id/exo_duration";   // total time "MM:SS"

    // iOS (XCUITest) — native player; controls are real named Buttons, forward is +10s (not 15).
    static final String IOS_PLAYER     = "Video";
    static final String IOS_FORWARD    = "VideoSkipForwardButton";
    static final String IOS_PLAY_PAUSE = "VideoPlayPauseButton";
    static final String IOS_REMAINING  = "VideoRemainingTimeLabel"; // "-M:SS" (time LEFT)
    static final String IOS_CLOSE      = "CloseButtonBlack";
    static final String IOS_START_QUIZ = "Start quiz"; // the quiz-intro button — reliable "video ended" signal

    private static final int SKIP_SECONDS = 15;         // Android +15s forward increment
    private static final int SKIP_SECONDS_IOS = 10;     // iOS +10s forward increment
    private static final int DEFAULT_FORWARD_TAPS = 30; // fallback cap if duration can't be read

    @Step("Wait for NeuroBooster video to load")
    public void waitForScreen() {
        if (isIOS()) {
            // The "Video" player element only exists while the controls are shown (they auto-hide),
            // so waiting on it burned the full 30s timeout (~32s of the video played first). Reveal
            // the controls and wait for the forward/remaining control — present as soon as we're on
            // the playing video screen — or the quiz intro if the video was very short.
            revealControlsIOS();
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(d -> !d.findElements(AppiumBy.accessibilityId(IOS_FORWARD)).isEmpty()
                            || !d.findElements(AppiumBy.accessibilityId(IOS_REMAINING)).isEmpty()
                            || !d.findElements(AppiumBy.accessibilityId(IOS_START_QUIZ)).isEmpty());
            return;
        }
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_PLAYER)));
    }

    @Step("Check if NeuroBooster video screen is displayed")
    public boolean isDisplayed() {
        return !findAllByPlatformId(ID_PLAYER, IOS_PLAYER).isEmpty();
    }

    /** The video ships English subtitles over German audio — assert the track exists (not the text). */
    @Step("Check NeuroBooster video subtitle track is present")
    public boolean hasSubtitleTrack() {
        // iOS renders captions as a plain StaticText overlay with no stable id — the video
        // always ships them, so there's nothing separate to assert there.
        if (isIOS()) return true;
        return !driver.findElements(AppiumBy.id(ID_SUBTITLES)).isEmpty();
    }

    /** Wait for the loading shutter to clear — the video is ready once {@code exo_shutter} is gone. */
    @Step("Wait for NeuroBooster video to finish loading")
    public void waitForVideoLoaded() {
        if (isIOS()) return; // iOS has no shutter/player-id signal; waitForScreen already revealed controls
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(d -> d.findElements(AppiumBy.id(ID_SHUTTER)).isEmpty()
                            && !d.findElements(AppiumBy.id(ID_PLAYER)).isEmpty());
        } catch (Exception ignored) { /* proceed; guarded below */ }
    }

    // ---------- time (read from exo_duration / exo_position) ----------
    /** Total video length in seconds, or -1 if not readable. */
    @Step("Get NeuroBooster video duration")
    public int getDurationSeconds() { return readTimeSeconds(ID_DURATION); }

    /** Current playback position in seconds, or -1 if not readable. */
    @Step("Get NeuroBooster video position")
    public int getPositionSeconds() { return readTimeSeconds(ID_POSITION); }

    private int readTimeSeconds(String id) {
        List<WebElement> els = driver.findElements(AppiumBy.id(id));
        if (els.isEmpty()) return -1;
        String t = els.get(0).getText();
        if (t == null || t.isEmpty()) t = els.get(0).getAttribute("content-desc");
        return parseClock(t);
    }

    /** Parse "M:SS", "MM:SS" or "H:MM:SS" to seconds; -1 if unparseable. */
    static int parseClock(String s) {
        if (s == null) return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:(\\d+):)?(\\d{1,2}):(\\d{2})").matcher(s.trim());
        if (!m.find()) return -1;
        int h = m.group(1) == null ? 0 : Integer.parseInt(m.group(1));
        return h * 3600 + Integer.parseInt(m.group(2)) * 60 + Integer.parseInt(m.group(3));
    }

    // ---------- fast-forward (duration-based) ----------
    /**
     * Fast-forward to the end, <b>duration-based</b>: reveal the controls, read
     * {@code exo_duration}/{@code exo_position}, and click the +15s button
     * {@code ceil(remaining / 15) (+1)} times, by locator. Falls back to a bounded
     * click-until-player-gone loop if the duration can't be read. E.g. a 45s video → 3.
     */
    @Step("Fast-forward NeuroBooster video to the end")
    public void fastForwardToEnd() {
        if (isIOS()) { fastForwardToEndIOS(); return; }
        waitForVideoLoaded();
        ensurePaused(); // pin the controller open so the forward control stays put for the rapid taps
        int duration = getDurationSeconds();
        int position = Math.max(0, getPositionSeconds());
        int need = duration > 0
                ? (int) Math.ceil(Math.max(0, duration - position) / (double) SKIP_SECONDS)
                : DEFAULT_FORWARD_TAPS;
        WebElement fwd = findControl(ID_FORWARD, DESC_FORWARD);
        if (fwd == null) { revealControls(); fwd = findControl(ID_FORWARD, DESC_FORWARD); }
        if (fwd == null) { log.warn("Android forward control not found — cannot fast-forward"); return; }
        var loc = fwd.getLocation(); var sz = fwd.getSize();
        int fx = loc.getX() + sz.getWidth() / 2, fy = loc.getY() + sz.getHeight() / 2;
        log.info("Android video duration={}s position={}s → {} rapid forward taps at ({},{})",
                duration, position, need, fx, fy);
        // All needed +15s seeks as ONE gesture (single round-trip) — not one findElement+click per tap.
        tapAtRepeated(fx, fy, need, 250);
        // The last seek lands on the end boundary; a tap or two more fires ExoPlayer ENDED → quiz.
        int extra = 0;
        while (extra < 4 && !driver.findElements(AppiumBy.id(ID_PLAYER)).isEmpty()) { tapAt(fx, fy); extra++; }
        log.info("Android fast-forward: {} taps (+{} to trigger end), player present={}",
                need, extra, !driver.findElements(AppiumBy.id(ID_PLAYER)).isEmpty());
    }

    /** Click the +15s button up to {@code taps} times (stops early once the video ends). */
    @Step("Fast-forward NeuroBooster video: {taps} forward clicks")
    public void fastForwardToEnd(int taps) {
        int forwards = 0;
        for (int i = 0; i < taps; i++) {
            if (driver.findElements(AppiumBy.id(ID_PLAYER)).isEmpty()) break; // ended → quiz
            WebElement forward = findControl(ID_FORWARD, DESC_FORWARD);
            if (forward == null) { revealControls(); forward = findControl(ID_FORWARD, DESC_FORWARD); }
            if (forward == null) continue; // controls wouldn't reveal this frame
            forward.click();
            forwards++;
        }
        log.info("Fast-forward: {} forward clicks, player still present={}",
                forwards, !driver.findElements(AppiumBy.id(ID_PLAYER)).isEmpty());
    }

    // ---------- iOS fast-forward (native player, +10s, reveal-per-tap) ----------
    /**
     * iOS fast-forward. The native player's controls are real named Buttons but auto-hide,
     * and iOS forward is <b>+10s</b>. We reveal the controller with a tap in the UPPER video
     * area (never the centre — play/pause sits dead-centre) and click {@code VideoSkipForwardButton}
     * until the {@code Video} element disappears (→ quiz). Verified on device.
     */
    private void fastForwardToEndIOS() {
        // No wait on the flaky "Video" element here — waitForScreen already revealed the controls.
        revealControlsIOS(); // ensure controls are shown (fast no-op if the forward button is present)
        WebElement fwd = firstByAccessibilityId(IOS_FORWARD);
        if (fwd == null) { revealControlsIOS(); fwd = firstByAccessibilityId(IOS_FORWARD); }
        if (fwd == null) { log.warn("iOS forward control not found — cannot fast-forward"); return; }
        var loc = fwd.getLocation(); var sz = fwd.getSize();
        int fx = loc.getX() + sz.getWidth() / 2, fy = loc.getY() + sz.getHeight() / 2;

        int remaining = readRemainingSecondsIOS();
        int need = remaining > 0 ? (int) Math.ceil(remaining / (double) SKIP_SECONDS_IOS) : DEFAULT_FORWARD_TAPS;
        log.info("iOS video remaining={}s → {} rapid forward taps at ({},{})", remaining, need, fx, fy);
        // All needed taps as ONE gesture (single round-trip) — fast, not one round-trip per tap.
        tapAtRepeated(fx, fy, need, 250);
        // The final +10s seek lands ON the end boundary; the app needs a tap or two more to actually
        // transition to the quiz intro. Add ONLY those, and stop as soon as the intro appears.
        int extra = 0;
        while (extra < 3 && driver.findElements(AppiumBy.accessibilityId(IOS_START_QUIZ)).isEmpty()) {
            tapAt(fx, fy); extra++;
        }
        log.info("iOS fast-forward: {} taps (+{} to trigger end), quiz reached={}",
                need, extra, !driver.findElements(AppiumBy.accessibilityId(IOS_START_QUIZ)).isEmpty());
    }

    /** Reveal the iOS controller with a tap in the UPPER video area (away from centre play/pause
     *  and the bottom slider), so it only shows controls and never toggles playback. */
    private void revealControlsIOS() {
        var size = driver.manage().window().getSize();
        // Retry the reveal tap: the controls auto-hide ~3s after appearing, so a single tap can miss
        // (the caller then found no forward button and had to reveal again). Loop until the forward
        // control is actually present (or we've reached the quiz intro).
        for (int attempt = 0; attempt < 3; attempt++) {
            if (firstByAccessibilityId(IOS_FORWARD) != null) return;                        // controls up
            if (!driver.findElements(AppiumBy.accessibilityId(IOS_START_QUIZ)).isEmpty()) return; // at quiz intro
            tapAt(size.getWidth() / 2, (int) (size.getHeight() * 0.22)); // neutral upper area (avoid centre play/pause)
            try {
                new WebDriverWait(driver, Duration.ofSeconds(2))
                        .until(d -> !d.findElements(AppiumBy.accessibilityId(IOS_FORWARD)).isEmpty()
                                || !d.findElements(AppiumBy.accessibilityId(IOS_START_QUIZ)).isEmpty());
            } catch (Exception ignored) { /* retry */ }
        }
    }

    /** Parse the iOS "-M:SS" remaining-time label to seconds; -1 if unreadable. */
    private int readRemainingSecondsIOS() {
        WebElement el = firstByAccessibilityId(IOS_REMAINING);
        if (el == null) return -1;
        String t = el.getAttribute("value");
        if (t == null || t.isEmpty()) t = el.getText();
        return parseClock(t == null ? null : t.replace("-", "").trim());
    }

    private WebElement firstByAccessibilityId(String accessibilityId) {
        List<WebElement> els = driver.findElements(AppiumBy.accessibilityId(accessibilityId));
        return els.isEmpty() ? null : els.get(0);
    }

    // ---------- individual controls (locator-based) ----------
    @Step("Rewind NeuroBooster video") public void tapRewind()   { clickControl(ID_REWIND, DESC_REWIND); }
    @Step("Play/pause NeuroBooster video") public void tapPlayPause() { clickControl(ID_PLAY_PAUSE, null); }
    @Step("Skip to next NeuroBooster video") public void tapNext() { clickControl(ID_NEXT, null); }
    @Step("Skip to previous NeuroBooster video") public void tapPrevious() { clickControl(ID_PREV, null); }

    /**
     * Reveal the controller with a single real TOUCH tap at screen centre. Two
     * device-verified facts drive this:
     * <ul>
     *   <li>Appium {@code element.click()} on the player issues an <i>accessibility</i>
     *       click, which ExoPlayer's controller-toggle ignores — only a real touch event
     *       ({@link #tapAt}) reveals the controller.</li>
     *   <li>While playing, the controller stays up for only ~2s, and a single Appium
     *       look-up can take longer than that, so locating a control then acting on it
     *       loses the race. {@link #ensurePaused()} removes the race (paused ⇒ pinned).</li>
     * </ul>
     * No-op if a control is already visible (a second tap would hide it).
     */
    private void revealControls() {
        if (findControl(ID_FORWARD, DESC_FORWARD) != null) return; // already visible — don't toggle it off
        if (driver.findElements(AppiumBy.id(ID_PLAYER)).isEmpty()) return;
        tapScreenCenter(); // ONE real touch tap reveals the controller
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(d -> !d.findElements(AppiumBy.id(ID_FORWARD)).isEmpty()
                            || !d.findElements(AppiumBy.accessibilityId(DESC_FORWARD)).isEmpty()
                            || d.findElements(AppiumBy.id(ID_PLAYER)).isEmpty());
        } catch (Exception ignored) { /* proceed; callers guard on a null control */ }
    }

    /**
     * Pause playback so the controller stays pinned open — the +15s button then stays
     * present between the slow Appium look-ups and every forward click lands (verified on
     * device: paused, 14/14 taps advanced +15s to the end → quiz). This is the manual
     * "pause, then fast-forward" workaround.
     *
     * <p>The play/pause button sits dead-centre, so a centre touch tap reveals the
     * controller (when hidden) or toggles play/pause (when shown). We read the button's
     * reported state each round and tap until it reports paused — self-correcting for the
     * ~2s auto-hide racing our taps.
     */
    private void ensurePaused() {
        var size = driver.manage().window().getSize();
        int cx = size.getWidth() / 2, cy = size.getHeight() / 2; // play/pause sits dead-centre
        for (int attempt = 0; attempt < 4; attempt++) {
            String state = playPauseState();
            if ("paused".equals(state)) { waitForControl(ID_FORWARD); return; }
            if ("playing".equals(state)) {
                tapAt(cx, cy);            // controls already shown → one centre tap hits play/pause → pause
            } else {
                // Hidden: replicate the manual action exactly — reveal, then pause within <1s,
                // as ONE atomic gesture so no slow findElement slips between the two taps and
                // lets the ~2s controller auto-hide turn the 2nd tap back into a reveal.
                tapAtTwice(cx, cy, 500);  // tap (reveal) → 500ms → tap (centre play/pause → pause)
            }
        }
        log.warn("Could not confirm the video is paused after 4 attempts — continuing");
    }

    /** "paused" (button offers Play), "playing" (button offers Pause), or "hidden" (no controller). */
    private String playPauseState() {
        WebElement pp = findControl(ID_PLAY_PAUSE, null);
        if (pp == null) return "hidden";
        String d = pp.getAttribute("content-desc");
        d = d == null ? "" : d.toLowerCase();
        return (d.contains("play") && !d.contains("pause")) ? "paused" : "playing";
    }

    /** Poll up to 2s for a control (by resource-id) to be present; null if it doesn't appear. */
    private WebElement waitForControl(String id) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(2)).until(d -> {
                List<WebElement> els = d.findElements(AppiumBy.id(id));
                return els.isEmpty() ? null : els.get(0);
            });
        } catch (Exception e) {
            return null;
        }
    }

    /** One real touch tap at the centre of the screen (where ExoPlayer puts play/pause). */
    private void tapScreenCenter() {
        var size = driver.manage().window().getSize();
        tapAt(size.getWidth() / 2, size.getHeight() / 2);
    }

    private void clickControl(String id, String desc) {
        revealControls();
        WebElement el = findControl(id, desc);
        if (el != null) el.click();
    }

    /** Find a control by resource-id, falling back to accessibility id; null if hidden/absent. */
    private WebElement findControl(String id, String desc) {
        List<WebElement> byId = driver.findElements(AppiumBy.id(id));
        if (!byId.isEmpty()) return byId.get(0);
        if (desc == null) return null;
        List<WebElement> byDesc = driver.findElements(AppiumBy.accessibilityId(desc));
        return byDesc.isEmpty() ? null : byDesc.get(0);
    }

    /** Close (X) the video — returns to the detail page WITHOUT completing the quiz. */
    @Step("Close NeuroBooster video")
    public void close() {
        driver.findElement(AppiumBy.id(ID_CLOSE)).click();
    }
}
