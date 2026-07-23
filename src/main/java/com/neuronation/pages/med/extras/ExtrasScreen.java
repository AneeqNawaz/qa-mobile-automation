package com.neuronation.pages.med.extras;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Extras tab -> Neurobooster listing. A vertical {@code recycler_view} of
 * categories, each with a title, Help button, "X / N discovered" progress, and a
 * vertical stack of tile cards (title + subtitle + thumbnail). Condition-agnostic:
 * it captures whatever is on screen, so the same object serves MCI (one category)
 * and Parkinson's (body category + Cognitive Health categories).
 *
 * <p>Locators captured live from Android {@code nn.mobile.app.med}
 * (see docs/superpowers/specs/extras-neurobooster-locators.md). iOS ids are
 * unverified placeholders (Android-only trust for now).
 *
 * <p>captureContent() + openTileBySubtitle() are added in Task 3.
 */
public class ExtrasScreen extends BaseScreen {

    // === Android resource-ids (captured live) ===
    static final String ID_TOOLBAR_TITLE          = "nn.mobile.app.med:id/main_toolbar_title";
    static final String ID_TOOLBAR_SUBTITLE       = "nn.mobile.app.med:id/toolbar_subtitle";
    static final String ID_RECYCLER               = "nn.mobile.app.med:id/recycler_view";
    static final String ID_CATEGORY_TITLE         = "nn.mobile.app.med:id/category_title";
    static final String ID_CATEGORY_PROGRESS_TEXT = "nn.mobile.app.med:id/category_progress_text";
    static final String ID_TILE_CARD              = "nn.mobile.app.med:id/domainCardView";
    static final String ID_TILE_TITLE             = "nn.mobile.app.med:id/title";
    static final String ID_TILE_SUBTITLE          = "nn.mobile.app.med:id/subtitle";
    static final String ID_TILE_THUMBNAIL         = "nn.mobile.app.med:id/thumbnail";
    static final String ID_TILE_CHECKMARK         = "nn.mobile.app.med:id/checkmark"; // present only on a discovered tile
    static final String ID_CATEGORY_HELP_BUTTON   = "nn.mobile.app.med:id/category_help_button";

    // Category "Help" tooltip — a standard Android AlertDialog (captured live on
    // RZCY82DVM3P): title in the app's alertTitle, body/button are framework ids.
    static final String ID_HELP_DIALOG_TITLE      = "nn.mobile.app.med:id/alertTitle";
    static final String ID_HELP_DIALOG_MESSAGE    = "android:id/message";
    static final String ID_HELP_DIALOG_OK         = "android:id/button1";

    // === iOS accessibility ids/labels (captured live 2026-07-22) ===
    // The app is Flutter-style: category/tile/progress are all StaticText (name==text),
    // distinguished by x-inset (category header x≈16, tile title/subtitle x≈32, progress
    // right-aligned matching "X / N discovered"). Tiles carry the nb id in the thumbnail.
    static final String IOS_TOOLBAR_TITLE      = "Neurobooster";
    static final String IOS_TOOLBAR_SUBTITLE   = "Mini-exercises to refresh your brain in the middle of a tiring day";
    static final String IOS_CATEGORY_HELP      = "questionmark.circle";
    static final String IOS_HELP_DIALOG_TITLE  = "alertTitle";
    static final String IOS_THUMB_PREFIX       = "mini_teaser_thumbnail_"; // + nbId
    static final String IOS_CHECKMARK          = "checkmark";              // discovered tick (label "selected")
    private static final int IOS_HEADER_MAX_X  = 24;  // category header x-inset (tiles are ~32)
    private static final java.util.regex.Pattern IOS_PROGRESS =
            java.util.regex.Pattern.compile("\\d+\\s*/\\s*\\d+\\s*discovered");

    /** Top-to-bottom section order (catalog JSON). Null until {@link #setSectionOrder} is called. */
    private SectionScrollPlanner sectionPlanner;

    /** Verbatim top-to-bottom section titles (for the order-walk in captureCategoryProgress). */
    private java.util.List<String> sectionOrderTitles;

    /** Set by {@link #findWithinSection} each iteration: the target section's header has been
     *  seen and the next section's header is not yet visible → visible tiles are in-section. */
    private boolean fwsInWindow;

    /** When true, tile/header location uses a MONOTONIC top-down scan (never reverses) instead of the
     *  bidirectional {@link #findWithinSection} planner. The planner oscillates on a deep bounded
     *  section (overshoots the window, reverses, overshoots back — the "stuck on the 3rd section"
     *  symptom); a down-only scan is deterministic. Suited to in-order processing where the app lands
     *  back at the top after each opened tile (Android). Enabled by the content regression. */
    private boolean monotonicScan;

    /** Route tile/header location through the non-reversing monotonic scan (Android). */
    public void setMonotonicScan(boolean on) { this.monotonicScan = on; }

    /**
     * Feed the top-to-bottom section (category) order — e.g. {@code catalog.categories}
     * titles. Enables section-bounded scrolling; if never called, all scrolling falls
     * back to the legacy behavior (zero regression risk).
     */
    @Step("Set Neurobooster section order for bounded scrolling")
    public void setSectionOrder(java.util.List<String> categoryTitlesTopToBottom) {
        this.sectionPlanner = new SectionScrollPlanner(categoryTitlesTopToBottom);
        this.sectionOrderTitles = categoryTitlesTopToBottom == null
                ? null : new java.util.ArrayList<>(categoryTitlesTopToBottom);
    }

    /** True when we can bound scrolling for this section (order set AND the section is known). */
    private boolean canBound(String category) {
        return sectionPlanner != null && sectionPlanner.hasOrder() && sectionPlanner.indexOf(category) >= 0;
    }

    @AndroidFindBy(id = ID_TOOLBAR_TITLE)
    @iOSXCUITFindBy(accessibility = IOS_TOOLBAR_TITLE)
    private WebElement toolbarTitle;

    @Step("Wait for Extras (Neurobooster) screen to load")
    public void waitForScreen() {
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.presenceOfElementLocated(
                        platformLocator(ID_TOOLBAR_TITLE, IOS_TOOLBAR_TITLE)));
    }

    @Step("Check if Extras (Neurobooster) screen is displayed")
    public boolean isDisplayed() {
        return !findAllByPlatformId(ID_TOOLBAR_TITLE, IOS_TOOLBAR_TITLE).isEmpty();
    }

    @Step("Get Neurobooster toolbar title")
    public String getToolbarTitle() {
        return getTextByPlatformId(ID_TOOLBAR_TITLE, IOS_TOOLBAR_TITLE);
    }

    @Step("Get Neurobooster toolbar subtitle")
    public String getToolbarSubtitle() {
        return getTextByPlatformId(ID_TOOLBAR_SUBTITLE, IOS_TOOLBAR_SUBTITLE);
    }

    @Step("Get number of currently visible Neurobooster categories")
    public int getCategoryCount() {
        return findAllByPlatformId(ID_CATEGORY_TITLE, ID_CATEGORY_TITLE).size();
    }

    // ──────────────────────────────────────────────
    // Category "Help" tooltip (Android AlertDialog)
    // ──────────────────────────────────────────────

    /** Tap the first visible category "Help" button to open its tooltip dialog. */
    @Step("Open the category Help tooltip")
    public void openCategoryHelp() {
        findAllByPlatformId(ID_CATEGORY_HELP_BUTTON, IOS_CATEGORY_HELP).get(0).click();
    }

    /**
     * Open the "?" Help tooltip for a SPECIFIC section: anchor that section's header, then tap the
     * help button sitting between it and the next header. Android only (uses the header Y-band).
     */
    @Step("Open the Help tooltip for section: {category}")
    public void openCategoryHelp(String category) {
        if (canBound(category) && !isCategoryVisible(category)) anchorCategory(category);
        Integer headerY = null, nextHeaderY = null;
        for (WebElement h : findAllByPlatformId(ID_CATEGORY_TITLE, ID_CATEGORY_TITLE)) {
            if (category.equals(textOf(h))) { headerY = yOf(h); break; }
        }
        if (headerY == null) { openCategoryHelp(); return; } // fallback: first visible
        for (WebElement h : findAllByPlatformId(ID_CATEGORY_TITLE, ID_CATEGORY_TITLE)) {
            int hy = yOf(h);
            if (hy > headerY && (nextHeaderY == null || hy < nextHeaderY)) nextHeaderY = hy;
        }
        WebElement best = null; int bestY = Integer.MAX_VALUE;
        for (WebElement b : findAllByPlatformId(ID_CATEGORY_HELP_BUTTON, IOS_CATEGORY_HELP)) {
            int by = yOf(b);
            if (by >= headerY - 40 && (nextHeaderY == null || by < nextHeaderY) && by < bestY) { best = b; bestY = by; }
        }
        (best != null ? best : findAllByPlatformId(ID_CATEGORY_HELP_BUTTON, IOS_CATEGORY_HELP).get(0)).click();
    }

    @Step("Get Help tooltip title")
    public String getHelpTooltipTitle() {
        return getTextByPlatformId(ID_HELP_DIALOG_TITLE, IOS_HELP_DIALOG_TITLE);
    }

    /** Reads {@code android:id/message} via the shared base alert helper. */
    @Step("Get Help tooltip message")
    public String getHelpTooltipMessage() {
        return getAlertMessage();
    }

    /** Dismiss the Help tooltip via its OK button (shared base alert helper). */
    @Step("Dismiss the Help tooltip")
    public void dismissHelpTooltip() {
        dismissAlert();
    }

    // ──────────────────────────────────────────────
    // Scroll enumeration + tile navigation / discovery
    // ──────────────────────────────────────────────

    /**
     * Scrolls the whole Neurobooster list top-to-bottom, one frame per scroll,
     * stitching frames via {@link ExtrasContentAccumulator}. Stops after two
     * consecutive scrolls reveal no new tiles (bottom reached).
     */
    @Override
    public Map<String, String> captureContent() {
        String title = getToolbarTitle();
        String subtitle = getToolbarSubtitle();
        // Both platforms: currentFrame() parses headers/progress/tiles — Android via resource-ids,
        // iOS via StaticText x-inset + mini_teaser_thumbnail_<id> images (verified live) — so the
        // same scroll-and-stitch capture works on iOS too.
        //
        // Parse the first frame ONCE and derive "at top" from it, instead of a separate scrollToTop()
        // page-source read followed by the first frame read (two ~3s parses on iOS for the same frame).
        ExtrasContentAccumulator acc = new ExtrasContentAccumulator();
        FrameSnapshot first = currentFrame();
        if (!first.hasCategory(FIRST_CATEGORY)) { scrollToTop(); first = currentFrame(); }
        int stagnant = 0, lastTotal = -1;
        // iOS: use the shorter overlapping scan swipe (a full swipe skips whole section headers on
        // the longer iOS list) with a higher step cap. Android's full swipe is proven, keep it.
        int cap = isIOS() ? 60 : 40;
        for (int step = 0; step < cap && stagnant < 2; step++) {
            acc.addFrame(step == 0 ? extractVisibleFrame(first) : extractVisibleFrame()); // reuse the first parse
            int total = acc.totalTileCount();
            if (total == lastTotal) stagnant++; else stagnant = 0;
            lastTotal = total;
            if (isIOS()) scanSwipeUp(); else swipeUp();
        }
        acc.addFrame(extractVisibleFrame()); // final bottom frame
        log.info("Extras captured: {} categories, {} tiles", acc.categoryCount(), acc.totalTileCount());
        return acc.toFlatMap(title, subtitle);
    }

    /**
     * Bounded bidirectional search inside ONE section's window.
     * Start anchor = the target category's header; end anchor = the NEXT category header
     * (from {@link SectionScrollPlanner}). Probes the CURRENT frame first (zero swipes when
     * the target is already visible), else scrolls the direction the planner picks — reversing
     * at a bound — and throws once the whole window has been observed without a hit. Never
     * scrolls past the end anchor; never resets to the very top.
     *
     * <p>Only call when {@link #canBound(String)} is true.
     */
    private <T> T findWithinSection(String category, java.util.function.Function<FrameSnapshot, T> probe) {
        SectionScrollPlanner.Search search = sectionPlanner.newSearch(category);
        String nextCategory = sectionPlanner.nextSection(category);
        String lastBottom = "";
        boolean anchored = false, everSeenStart = false;
        for (int step = 0; step < 30; step++) {
            FrameSnapshot f = currentFrame();

            // Window state (BEFORE the probe): once the target header has been seen and the
            // next section's header is not yet on screen, every visible tile belongs to THIS
            // section. This lets the probe match a repeated-subtitle tile even after the header
            // scrolls off the top — which happens when the native anchor parks the header at
            // the bottom edge and we then scroll down to reveal its tiles.
            if (f.hasCategory(category)) everSeenStart = true;
            boolean nextVisible = nextCategory != null && f.hasCategory(nextCategory);
            fwsInWindow = everSeenStart && !nextVisible;

            T hit = probe.apply(f);
            if (hit != null) return hit;

            // Android: if the section header isn't on screen yet, jump straight to it with the
            // native UiScrollable scroll (robust across the long body section). Short manual
            // swipes stall at a section boundary — bottomSubtitle() tracks the last CARD, and
            // the next thing to scroll in there is a HEADER, so the bottom card stops changing
            // and the atBottom heuristic false-positives into an oscillation. Anchoring once
            // guarantees the header is visible, so the planner has its anchor and the probe
            // (which needs the header for in-section matching) can hit. Stays section-scoped:
            // scrollIntoView stops AT the header, never runs to the bottom.
            if (isAndroid() && !anchored && !f.hasCategory(category)) {
                anchored = true;
                scrollIntoViewByText(ID_CATEGORY_TITLE, category);
                continue;
            }
            // iOS has no native scrollIntoView: if the target header isn't in view and we've never
            // seen it, reorient to the top once so the planner has a known anchor (the first header)
            // and scans DOWN to the target deterministically — otherwise, deep in the list with no
            // header visible, the planner has no direction and can stall.
            if (isIOS() && !anchored && !everSeenStart && !f.hasCategory(category)) {
                anchored = true;
                scrollToTop();
                continue;
            }

            String bottom = f.bottomSubtitle();
            boolean atBottom = !bottom.isEmpty() && bottom.equals(lastBottom);
            lastBottom = bottom;

            java.util.List<String> headers = new java.util.ArrayList<>();
            for (YText h : f.headers) headers.add(h.text);

            SectionScrollPlanner.Move move = search.decide(headers, f.hasCategory(FIRST_CATEGORY), atBottom);
            switch (move) {
                case SCROLL_DOWN: scanSwipeUp(); break;   // scan swipe → no header skipped between frames
                case SCROLL_UP:   swipeDown();  break;
                case EXHAUSTED:
                    throw new NoSuchElementException(
                            "Neurobooster target not found in section window: " + category);
            }
        }
        throw new NoSuchElementException("Neurobooster section scroll exceeded step cap: " + category);
    }

    /**
     * Open the tile identified by (category, subtitle). Subtitles repeat across
     * cognitive-health categories (e.g. "Psychoeducational" appears under every
     * category), so the category disambiguates. Uses UiScrollable to jump straight
     * to the category (robust on the long list), then taps its matching tile.
     */
    public void openTile(String category, String subtitle) { openTile(category, subtitle, null); }

    /**
     * Open the tile. On iOS, when {@code nbId} is known (COGNITIVE_HEALTH) we match by the
     * thumbnail's nb id — wording-independent and immune to the repeated-subtitle problem;
     * otherwise (body tiles) we match by subtitle. Android uses the resource-id resolver.
     */
    @Step("Open Neurobooster tile: {category} / {subtitle}")
    public void openTile(String category, String subtitle, Integer nbId) {
        if (isIOS()) {
            CardInfo c = locateTileIOS(category, subtitle, nbId);
            if (c == null) throw new NoSuchElementException("iOS tile not found: " + category + " / " + subtitle);
            tapAt(c.tapX, c.tapY); // iOS tiles have no tappable id — tap the tile centre
            return;
        }
        resolveTile(category, subtitle).click();
    }

    public boolean isTileDiscovered(String category, String subtitle) { return isTileDiscovered(category, subtitle, null); }

    /**
     * True if the (category, subtitle[, nbId]) tile shows a discovered {@code checkmark}.
     * The tick paints a beat AFTER returning from the detail/quiz screen (the progress count
     * updates first), so after locating the tile we poll briefly for the checkmark to appear
     * rather than reading once and racing the render.
     */
    @Step("Check Neurobooster tile discovered: {category} / {subtitle}")
    public boolean isTileDiscovered(String category, String subtitle, Integer nbId) {
        if (isIOS()) {
            try {
                return new WebDriverWait(driver, Duration.ofSeconds(5), Duration.ofMillis(400)).until(d -> {
                    CardInfo c = locateTileIOS(category, subtitle, nbId);
                    return c != null && c.checkmark;
                });
            } catch (Exception e) { return false; }
        }
        WebElement card = resolveTile(category, subtitle); // scrolls the tile into view (stays in view while we poll)
        if (!card.findElements(AppiumBy.id(ID_TILE_CHECKMARK)).isEmpty()) return true;
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(5), Duration.ofMillis(400)).until(d -> {
                WebElement c = findVisibleTileInCategory(category, subtitle);
                if (c == null) c = findVisibleTileBySubtitle(subtitle);
                return c != null && !c.findElements(AppiumBy.id(ID_TILE_CHECKMARK)).isEmpty();
            });
        } catch (Exception e) { return false; }
    }

    /** True when the Extras list is scrolled to the top (first category header visible). */
    public boolean isAtTop() { return atTop(); }

    /** Listing-card snapshot of ONE tile (title/subtitle/image/discovered), read in place. */
    public static final class TileCard {
        public final String title, subtitle;
        public final boolean hasImage, discovered;
        TileCard(String title, String subtitle, boolean hasImage, boolean discovered) {
            this.title = title == null ? "" : title;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.hasImage = hasImage;
            this.discovered = discovered;
        }
    }

    /**
     * Locate a single tile (section-bounded, in place — NO full-list scroll) and read its listing
     * card: title, subtitle, whether a thumbnail is present, and whether it shows a discovered tick.
     * Used by the per-tile content regression to assert a tile's initial state before opening it.
     */
    @Step("Read Neurobooster tile card: {category} / {subtitle}")
    public TileCard readTileCard(String category, String subtitle, Integer nbId) {
        if (isIOS()) {
            CardInfo c = locateTileIOS(category, subtitle, nbId);
            if (c == null) throw new NoSuchElementException("Tile card not found: " + category + " / " + subtitle);
            return new TileCard(c.title, c.subtitle, c.thumbnail, c.checkmark);
        }
        return readAndroidCard(resolveTile(category, subtitle));
    }

    /**
     * Locate a tile ONCE, read its listing card (initial state), then open it — a single scroll
     * instead of the two the content regression used to do (readTileCard THEN openTile each scanned
     * to the same tile). The returned card is the PRE-open snapshot (read before the tap).
     */
    @Step("Open Neurobooster tile (reading its card first): {category} / {subtitle}")
    public TileCard openTileAndReadCard(String category, String subtitle, Integer nbId) {
        if (isIOS()) {
            CardInfo c = locateTileIOS(category, subtitle, nbId);
            if (c == null) throw new NoSuchElementException("iOS tile not found: " + category + " / " + subtitle);
            TileCard card = new TileCard(c.title, c.subtitle, c.thumbnail, c.checkmark);
            tapAt(c.tapX, c.tapY);
            return card;
        }
        WebElement el = resolveTile(category, subtitle);
        TileCard card = readAndroidCard(el);
        el.click();
        return card;
    }

    /** Read an already-located Android tile card's fields. Thumbnail is best-effort (lazy-loads);
     *  the tile's image is asserted on the detail screen, not from this listing snapshot. */
    private TileCard readAndroidCard(WebElement card) {
        return new TileCard(
                descText(card, ID_TILE_TITLE),
                descText(card, ID_TILE_SUBTITLE),
                !card.findElements(AppiumBy.id(ID_TILE_THUMBNAIL)).isEmpty(),
                !card.findElements(AppiumBy.id(ID_TILE_CHECKMARK)).isEmpty());
    }

    /**
     * iOS: bring the target tile into the tappable band and return it. Position-based and
     * BIDIRECTIONAL: once the card is on screen we nudge toward it — if it sits above the band
     * we scroll UP (swipe down), if below we scroll DOWN (swipe up). Scrolling only one way
     * misses a tile we've already passed (e.g. the last tile of a small category after the
     * previous tile completed). Matches by nbId when known (COGNITIVE_HEALTH), else subtitle.
     */
    private CardInfo locateTileIOS(String category, String subtitle, Integer nbId) {
        boolean uniqueSubtitle = FIRST_CATEGORY.equals(category); // body subtitles are unique
        var size = driver.manage().window().getSize();
        int minY = 140, maxY = size.getHeight() - 120;

        // Monotonic mode (content regression): DOWN-only scan, no bidirectional planner → no
        // scroll-to-top/reverse on deep sections (flow4). Cognitive tiles match by unique nbId and
        // body tiles by unique subtitle — both global — so no window tracking is needed.
        if (monotonicScan) return locateTileIOSMonotonic(category, subtitle, nbId, uniqueSubtitle, minY, maxY);

        // Section-bounded fast path: stay inside this category's window and stop at the next
        // section header instead of scanning the whole list. The tappable-band guard lives in
        // the probe — an out-of-band card returns null, so findWithinSection keeps scrolling
        // until the card sits in the band. Preserves the "not found → null" contract.
        if (canBound(category)) {
            final boolean unique = uniqueSubtitle;
            final int loY = minY, hiY = maxY;
            try {
                return findWithinSection(category, f -> {
                    CardInfo c = findCardIOS(f, category, subtitle, unique, nbId);
                    return (c != null && c.tapY >= loY && c.tapY <= hiY) ? c : null;
                });
            } catch (NoSuchElementException e) {
                return null;
            }
        }

        scrollToCategory(category);
        int offFrame = 0;
        for (int i = 0; i < 30; i++) {
            FrameSnapshot f = currentFrame();
            CardInfo c = findCardIOS(f, category, subtitle, uniqueSubtitle, nbId); // ignores the band
            if (c != null) {
                offFrame = 0;
                if (c.tapY >= minY && c.tapY <= maxY) {
                    log.info("iOS tile '{}' (nbId={}) at tap=({},{})", subtitle, nbId, c.tapX, c.tapY);
                    return c;
                }
                if (c.tapY < minY) swipeDown(); // target above the band → scroll UP to bring it down
                else swipeUp();                 // target below the band → scroll DOWN to bring it up
                continue;
            }
            if (++offFrame > 7) break;           // not on screen for several frames → give up
            swipeUp();                           // the target usually sits below the category header
        }
        log.warn("iOS tile '{}' (nbId={}) not found; last-frame cards={}", subtitle, nbId,
                currentFrame().cards.stream()
                        .map(c -> c.subtitle + "[nb=" + c.nbId + ",tapY=" + c.tapY + "]")
                        .collect(java.util.stream.Collectors.toList()));
        return null;
    }

    /** Find the target card in the frame, ignoring visibility band (nbId when known, else subtitle). */
    /** iOS monotonic tile locate: scan DOWN from the current position (never reverse/scroll-to-top),
     *  returning the card once it sits in the tappable band. Falls back to ONE reset-to-top + rescan
     *  if the card is above us (already passed). Preserves the "not found → null" contract. */
    private CardInfo locateTileIOSMonotonic(String category, String subtitle, Integer nbId,
                                            boolean unique, int minY, int maxY) {
        CardInfo c = iosScanDownForCard(category, subtitle, nbId, unique, minY, maxY);
        if (c != null) return c;
        if (!atTop()) {
            scrollToTop();
            c = iosScanDownForCard(category, subtitle, nbId, unique, minY, maxY);
            if (c != null) return c;
        }
        return null;
    }

    /** One DOWN-only pass from the current position: return the matching card when it is in the
     *  tappable band [minY,maxY]; a card below the band keeps the scan going (it rises into the band
     *  as we scroll down). When the LIST BOTTOM is reached and the card was seen but can't scroll into
     *  the band (the last section's tiles sit at the bottom), accept it where it is (still on-screen +
     *  tappable) instead of returning null — that avoids a needless scrollToTop-and-rescan round trip.
     *  Returns null only if the card was never seen. Never swipes up. */
    private CardInfo iosScanDownForCard(String category, String subtitle, Integer nbId,
                                        boolean unique, int minY, int maxY) {
        String lastBottom = "";
        int stagnant = 0;
        CardInfo lastSeen = null; // best card seen so far, even if below the ideal band
        for (int step = 0; step < 45; step++) {
            FrameSnapshot f = currentFrame();
            CardInfo c = findCardIOS(f, category, subtitle, unique, nbId);
            if (c != null) {
                if (c.tapY >= minY && c.tapY <= maxY) return c;
                if (c.tapY > minY) lastSeen = c;      // on-screen but below the band — remember it
            }
            String bottom = f.bottomSubtitle();
            if (!bottom.equals(lastBottom) || f.hasCategory(FIRST_CATEGORY)) stagnant = 0;
            else if (++stagnant >= 2) return lastSeen;  // bottom: accept the low-but-visible card if seen
            lastBottom = bottom;
            scanSwipeUp();                            // DOWN only — never reverse
        }
        return lastSeen;
    }

    private CardInfo findCardIOS(FrameSnapshot f, String category, String subtitle, boolean uniqueSubtitle, Integer nbId) {
        if (nbId != null && nbId > 0) {
            for (CardInfo c : f.cards) if (c.nbId == nbId) return c;
            return null;
        }
        String target = norm(subtitle);
        if (uniqueSubtitle) {
            for (CardInfo c : f.cards) if (cardMatches(c, target)) return c;
            return null;
        }
        Integer hy = null;
        for (YText h : f.headers) if (norm(category).equals(norm(h.text))) { hy = h.y; break; }
        if (hy == null) return null;
        Integer nextY = null;
        for (YText h : f.headers) if (h.y > hy && (nextY == null || h.y < nextY)) nextY = h.y;
        for (CardInfo c : f.cards) {
            if (!cardMatches(c, target)) continue;
            if (c.y > hy && (nextY == null || c.y < nextY)) return c;
        }
        return null;
    }

    /** A tile matches when the target text equals its subtitle OR its title (iOS labels vary). */
    private static boolean cardMatches(CardInfo c, String target) {
        return norm(c.subtitle).equals(target) || norm(c.title).equals(target);
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.replace('’', '\'').replace('‘', '\'').replace('“', '"').replace('”', '"')
                .replace(' ', ' ').replace(' ', ' ').replaceAll("\\s+", " ").trim();
    }

    /**
     * Return the matching card ONLY when it sits fully inside the tappable band (below the
     * toolbar, above the tab bar) — a partially-scrolled tile has a tap point near/off the
     * fold, so the coordinate tap would miss. Returning null keeps {@link #locateTileIOS}
     * scrolling until the tile is comfortably in view.
     */

    /**
     * Locate a tile by (category, subtitle), quickly and reliably:
     *  1) jump to the category header — cognitive tiles (repeated subtitles) sit right
     *     under it, so the in-category match resolves immediately;
     *  2) otherwise the subtitle is unique (body tiles), so scroll straight to it.
     */
    private WebElement resolveTile(String category, String subtitle) {
        // Monotonic mode (content regression): down-only scan, no bidirectional planner → no
        // oscillation on deep bounded sections. Android only (iOS uses locateTileIOS).
        if (monotonicScan && isAndroid()) return resolveTileMonotonic(category, subtitle);
        // Section-bounded fast path: stay inside this category's window (its header → the
        // next category header), probing the current frame first (zero swipes when the tile
        // is already visible) and never running to the bottom. Legacy scan below otherwise.
        if (canBound(category)) {
            final boolean unique = FIRST_CATEGORY.equals(category);
            return findWithinSection(category, f -> {
                if (unique) return findVisibleTileBySubtitle(subtitle);   // body subtitles are unique
                // Cognitive subtitles repeat across sections. While we're inside this section's
                // window (header seen, next header not yet visible) a subtitle match is
                // unambiguous even if the header has scrolled off; otherwise fall back to the
                // strict header-bounded match.
                if (fwsInWindow) {
                    WebElement e = findVisibleTileBySubtitle(subtitle);
                    if (e != null) return e;
                }
                return findVisibleTileInCategory(category, subtitle);
            });
        }

        // Body subtitles are unique → match by subtitle anywhere; cognitive subtitles
        // repeat across categories → must match within the category (header + tile).
        boolean uniqueSubtitle = FIRST_CATEGORY.equals(category);

        // Primary: UiScrollable.scrollIntoView by the subtitle locator — reliable via
        // Appium's full tree, and immune to the flaky per-frame text rendering that
        // trips manual scanning. Prefer a category-verified match; else, if the
        // subtitle is unique on screen, take it directly.
        scrollIntoViewByText(ID_TILE_SUBTITLE, subtitle);
        WebElement inCat = findVisibleTileInCategory(category, subtitle);
        if (inCat != null) return inCat;
        WebElement bySub = findVisibleTileBySubtitle(subtitle);
        // Accept a subtitle match when it's unambiguous — the body subtitles are unique,
        // or only one tile with this subtitle is on screen (scrollIntoView landed on it).
        if (bySub != null && (uniqueSubtitle || countVisibleTilesBySubtitle(subtitle) == 1)) return bySub;

        // Fallback: manual scan with early-exit (no stagnation stop).
        scrollToCategory(category);
        for (int i = 0; i < 26; i++) {
            WebElement card = uniqueSubtitle
                    ? findVisibleTileBySubtitle(subtitle)
                    : findVisibleTileInCategory(category, subtitle);
            if (card != null) return card;
            if (!uniqueSubtitle && !isCategoryVisible(category) && i > 0) break; // scrolled past the category
            swipeUp();
        }
        throw new NoSuchElementException("Neurobooster tile not found: " + category + " / " + subtitle);
    }

    /** Scroll a text (of a given resource-id) into view via UiScrollable on the recycler. No-op on iOS/failure. */
    private void scrollIntoViewByText(String resId, String text) {
        if (!isAndroid()) return;
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().resourceId(\"" + ID_RECYCLER + "\")).setMaxSearchSwipes(30)"
                            + ".scrollIntoView(new UiSelector().resourceId(\"" + resId + "\").text(\""
                            + text.replace("\"", "\\\"") + "\"))"));
        } catch (Exception ignored) { /* fall back to manual scan */ }
    }


    /** Subtitle of the bottom-most visible tile card — used for reliable end-of-list detection. */
    private String bottomTileSubtitle() {
        return currentFrame().bottomSubtitle();
    }

    private WebElement findVisibleTileBySubtitle(String subtitle) {
        for (WebElement card : findAllByPlatformId(ID_TILE_CARD, ID_TILE_CARD)) {
            if (subtitle.equals(descText(card, ID_TILE_SUBTITLE))) return card;
        }
        return null;
    }

    /**
     * Monotonic, non-reversing tile locate: scroll to top, then scan DOWN only until the tile is in
     * view within its section window (target header seen, next-section header not yet). Never swipes
     * up, so it cannot oscillate around a deep section's boundary the way the bidirectional planner
     * does. Body subtitles are unique (match anywhere); cognitive subtitles repeat across sections
     * (match only inside the window). Throws if the whole list is scanned without a hit.
     */
    private WebElement resolveTileMonotonic(String category, String subtitle) {
        // Scan DOWN from the CURRENT position first — tiles are processed in order and the target is
        // almost always at/below where we are (e.g. right after reading the section header or its help),
        // so this avoids a pointless trip back to the top of the list. Only if the tile is above us
        // (we've already scrolled past it) do we reset to the top ONCE and scan down again.
        WebElement card = scanDownForTile(category, subtitle);
        if (card != null) return card;
        if (!atTop()) {
            scrollToTop();
            card = scanDownForTile(category, subtitle);
            if (card != null) return card;
        }
        throw new NoSuchElementException("Neurobooster tile not found (monotonic): " + category + " / " + subtitle);
    }

    /** One DOWN-only pass from the current position for a tile inside its section window. Returns the
     *  card, or null if the window was passed / the bottom was reached without a hit (never reverses). */
    private WebElement scanDownForTile(String category, String subtitle) {
        boolean unique = FIRST_CATEGORY.equals(category);
        String nextCat = sectionPlanner != null ? sectionPlanner.nextSection(category) : null;
        boolean inWindow = false, everTarget = false;
        String lastBottom = "";
        int stagnant = 0;
        for (int step = 0; step < 45; step++) {
            FrameSnapshot f = currentFrame();
            boolean hasTarget = f.hasCategory(category);
            boolean hasNext = nextCat != null && f.hasCategory(nextCat);
            if (hasTarget) { inWindow = true; everTarget = true; }

            WebElement card = null;
            if (unique) {
                card = findVisibleTileBySubtitle(subtitle);            // body subtitles are unique
            } else if (hasTarget) {
                // Target header on screen → match ONLY a tile BELOW it (and above the next header).
                // Cognitive subtitles repeat across sections, so a plain subtitle match here would grab
                // a same-subtitle tile from the PREVIOUS section still visible above this header — the
                // flow3 wrong-tile bug (opened Drive content / a Drive quiz under a Mood tile).
                card = findVisibleTileInCategory(category, subtitle);
            } else if (inWindow && !hasNext) {
                // Header has scrolled off the top and the next section isn't visible yet → only this
                // section's tiles are on screen, so a subtitle match is unambiguous.
                card = findVisibleTileBySubtitle(subtitle);
            }
            if (card != null) return card;

            // Passed the whole target window (its header seen, now gone, next section in view) → give up.
            if (everTarget && hasNext && !hasTarget) return null;

            String bottom = f.bottomSubtitle();
            if (!bottom.equals(lastBottom) || f.hasCategory(FIRST_CATEGORY)) stagnant = 0;
            else if (++stagnant >= 3) return null;   // bottom reached without a hit
            lastBottom = bottom;
            scanSwipeUp();                            // DOWN only — never reverse
        }
        return null;
    }

    /** Monotonic category-progress read: scan DOWN to the header (reset to top once if we're already
     *  past it), then read the progress line under it. No bidirectional anchoring → no oscillation. */
    private String monotonicGetCategoryProgress(String category) {
        if (!isCategoryVisible(category) && !reachCategoryDirected(category)) {
            if (!atTop()) scrollToTop();
            scanDownToCategory(category);
        }
        for (int i = 0; i < 6; i++) {
            FrameSnapshot f = currentFrame();
            for (YText h : f.headers) {
                if (SectionScrollPlanner.norm(h.text).equals(SectionScrollPlanner.norm(category))) {
                    String p = f.progressFor(h.y);
                    if (p != null) return p;
                }
            }
            scanSwipeUp();
        }
        throw new NoSuchElementException("Neurobooster category not found (monotonic): " + category);
    }

    private int countVisibleTilesBySubtitle(String subtitle) {
        int n = 0;
        for (WebElement card : findAllByPlatformId(ID_TILE_CARD, ID_TILE_CARD)) {
            if (subtitle.equals(descText(card, ID_TILE_SUBTITLE))) n++;
        }
        return n;
    }

    /** Scroll to a category header and return its verbatim progress text, e.g. "1 / 19 discovered". */
    @Step("Get Neurobooster category progress: {categoryTitle}")
    public String getCategoryProgress(String categoryTitle) {
        // Monotonic header read is platform-agnostic (frame headers + progress line), so enable it on
        // iOS too — the bidirectional planner reverses (scrolls toward the top) on deep sections there.
        if (monotonicScan) return monotonicGetCategoryProgress(categoryTitle);
        if (!canBound(categoryTitle)) return legacyGetCategoryProgress(categoryTitle);
        // Anchor the header natively (robust across the long body section; short manual swipes
        // stall at section boundaries), then read its progress with a few small nudges — the
        // progress line sits just under the header and can land below the fold when the native
        // scroll parks the header at the bottom edge. (getCategoryProgress only needs the header
        // + its progress, so it does NOT go through the tile-oriented findWithinSection, whose
        // end-anchor bound would race past a header parked at the bottom.)
        if (!isCategoryVisible(categoryTitle)) anchorCategory(categoryTitle);
        for (int i = 0; i < 6; i++) {
            FrameSnapshot f = currentFrame();
            for (YText h : f.headers) {
                if (SectionScrollPlanner.norm(h.text).equals(SectionScrollPlanner.norm(categoryTitle))) {
                    String p = f.progressFor(h.y);
                    if (p != null) return p;
                }
            }
            scanSwipeUp(); // small nudge to bring the progress line under the header into view
        }
        throw new NoSuchElementException("Neurobooster category not found: " + categoryTitle);
    }

    /**
     * Bring a section header on screen, direction-aware: Android jumps via native
     * {@code UiScrollable.scrollIntoView} (inside {@link #findWithinSection}); iOS scrolls up or
     * down as the planner directs from the visible headers (no native scroll). No-op if not found.
     */
    private void anchorCategory(String category) {
        try { findWithinSection(category, f -> f.hasCategory(category) ? Boolean.TRUE : null); }
        catch (NoSuchElementException ignored) { }
    }

    private String legacyGetCategoryProgress(String categoryTitle) {
        scrollToCategory(categoryTitle);
        for (int step = 0; step < 6; step++) {
            for (WebElement t : findAllByPlatformId(ID_CATEGORY_TITLE, ID_CATEGORY_TITLE)) {
                if (!categoryTitle.equals(textOf(t))) continue;
                int ty = yOf(t);
                WebElement best = null;
                for (WebElement p : findAllByPlatformId(ID_CATEGORY_PROGRESS_TEXT, ID_CATEGORY_PROGRESS_TEXT)) {
                    if (yOf(p) >= ty && (best == null || yOf(p) < yOf(best))) best = p;
                }
                if (best != null) return textOf(best);
            }
            swipeUp();
        }
        throw new NoSuchElementException("Neurobooster category not found: " + categoryTitle);
    }

    /**
     * Bring a category header into view: fast path via UiScrollable.scrollIntoView,
     * verified, with a bounded linear fallback that stops at the bottom category.
     * No-op on iOS.
     */
    /** Bring a category header into view via manual swipes (UiScrollable is a no-op in this app). */
    private void scrollToCategory(String category) {
        if (isCategoryVisible(category)) return;
        // We almost always move DOWN the list (tiles opened/verified in order), so scan down
        // from the CURRENT position first — this avoids snapping back to the very top on every
        // open/verify. Only if the target isn't below us (we were already past it) do we reset
        // to the top and scan down deterministically.
        if (scanDownToCategory(category)) return;
        scrollToTop();
        scanDownToCategory(category);
    }

    /**
     * Reach a section header by scanning the RIGHT direction, decided from the catalog order vs a
     * currently-visible header: if the target sits ABOVE the visible section, scan UP (a few swipes —
     * e.g. iOS lands at the just-completed tile, below its header, at a section transition); if BELOW,
     * scan DOWN. This avoids the down-to-bottom-then-scrollToTop detour a pure down-scan takes when the
     * header is just above. Returns false if not reached (caller falls back to scrollToTop + down).
     */
    private boolean reachCategoryDirected(String category) {
        int targetIdx = sectionPlanner != null ? sectionPlanner.indexOf(category) : -1;
        if (targetIdx >= 0) {
            Integer visIdx = null;
            for (YText h : currentFrame().headers) {
                int idx = sectionPlanner.indexOf(h.text);
                if (idx >= 0) { visIdx = idx; break; }
            }
            if (visIdx != null) {
                return targetIdx < visIdx ? scanUpToCategory(category) : scanDownToCategory(category);
            }
        }
        return scanDownToCategory(category) || scanUpToCategory(category);
    }

    /** Swipe down (scroll the list UP) until the category header appears or the top is reached. */
    private boolean scanUpToCategory(String category) {
        String lastTop = "";
        int stagnant = 0;
        for (int step = 0; step < 30; step++) {
            FrameSnapshot f = currentFrame();
            if (f.hasCategory(category)) return true;
            if (atTop()) return f.hasCategory(category);
            String top = f.cards.isEmpty() ? "" : f.cards.get(0).subtitle;
            if (!top.equals(lastTop)) stagnant = 0; else if (++stagnant >= 2) return false;
            lastTop = top;
            scanSwipeDown(); // short OVERLAPPING up-scroll — a full swipeDown would skip the header
        }
        return false;
    }

    /** Swipe up (scroll the list down) until the category header appears or the list stops moving. */
    private boolean scanDownToCategory(String category) {
        String lastBottom = "";
        int stagnant = 0;
        // Cap raised to 50: a DEEP section (e.g. Dealing = section 5, reached from the top through the
        // 19 body + 9 earlier-cognitive tiles) needs more short overlapping swipes than the old 30 cap
        // allowed, which made flow4 give up before ever reaching the Dealing header.
        for (int step = 0; step < 50; step++) {
            FrameSnapshot f = currentFrame();
            if (f.hasCategory(category)) return true;
            String bottom = f.bottomSubtitle();
            // Ignore "no movement" while still at the top (the toolbar is just collapsing). Require 3
            // stagnant frames (was 2) so a lazy-load pause mid-scroll can't bail early before the bottom.
            if (!bottom.equals(lastBottom) || f.hasCategory(FIRST_CATEGORY)) stagnant = 0;
            else if (++stagnant >= 3) return false; // bottom reached without finding it
            lastBottom = bottom;
            scanSwipeUp(); // overlapping scan swipe → don't skip the target category header
        }
        return false;
    }

    private boolean isCategoryVisible(String category) {
        return currentFrame().hasCategory(category);
    }

    /**
     * One quick top-to-bottom pass returning each category's verbatim progress text,
     * stopping as soon as the list stops moving (bottom reached). Used to verify the
     * fresh-account baseline without per-category jumps.
     */
    /** Subtitles of tiles found showing a discovered checkmark during the last {@link #captureCategoryProgress()}. */
    private final List<String> lastDiscoveredSubtitles = new ArrayList<>();

    public List<String> lastDiscoveredSubtitles() {
        return new ArrayList<>(lastDiscoveredSubtitles);
    }

    /** Record any not-yet-seen tiles in this frame that show a discovered checkmark. */
    private void collectDiscovered(FrameSnapshot f, java.util.Set<String> seen) {
        for (CardInfo c : f.cards) {
            String key = c.title + "|" + c.subtitle;
            if (seen.add(key) && c.checkmark) lastDiscoveredSubtitles.add(c.subtitle);
        }
    }

    /**
     * One top-to-bottom pass returning each category's progress text (and recording
     * any discovered tiles). Stops as soon as the known last tile (from the catalog:
     * {@code lastTileTitle} / {@code lastTileSubtitle}) is visible — the true bottom —
     * so there are no extra scrolls. Assumes the list starts at the top (as it is on
     * entering the Extras tab); only resets to top if it isn't already there.
     */
    public Map<String, String> captureCategoryProgress(String lastTileTitle, String lastTileSubtitle) {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
        lastDiscoveredSubtitles.clear();

        // Robust path (Android, order known): walk the sections in order, anchoring each
        // header with the native scroll (short manual swipes stall at section boundaries —
        // bottomSubtitle() stops changing when the next item to scroll in is a HEADER, which
        // the legacy sweep below misreads as end-of-list and stops early, missing later
        // sections). Read each section's progress and collect any discovered tiles in view.
        if (isAndroid() && sectionOrderTitles != null && !sectionOrderTitles.isEmpty()) {
            java.util.Set<String> seen = new java.util.LinkedHashSet<>();
            if (!atTop()) scrollToTop();
            for (String category : sectionOrderTitles) {
                try {
                    out.put(category, getCategoryProgress(category)); // anchors the header natively
                } catch (NoSuchElementException e) {
                    // leave absent → caller asserts "should be present"
                }
                collectDiscovered(currentFrame(), seen);
            }
            return out;
        }

        if (!atTop()) scrollToTop();
        java.util.Set<String> seenTiles = new java.util.LinkedHashSet<>();
        String lastBottom = "";
        int stagnant = 0;
        for (int step = 0; step < 30; step++) {
            FrameSnapshot f = currentFrame(); // one page-source read per frame (was ~dozens of round-trips)
            for (YText t : f.headers) {
                if (t.text.isEmpty() || out.containsKey(t.text)) continue;
                String prog = f.progressFor(t.y);
                if (prog != null) out.put(t.text, prog);
            }
            boolean reachedLastTile = false;
            for (CardInfo c : f.cards) {
                String key = c.title + "|" + c.subtitle;
                if (seenTiles.add(key) && c.checkmark) lastDiscoveredSubtitles.add(c.subtitle);
                if (c.title.equals(lastTileTitle) && c.subtitle.equals(lastTileSubtitle)) reachedLastTile = true;
            }
            // Primary stop: the catalog's last tile is on screen → true bottom.
            if (reachedLastTile) break;
            // Safety net only after the list stops producing new tiles for several frames
            // (kept lenient so the collapsing toolbar near the top can't trip it early).
            String bottom = f.bottomSubtitle();
            if (!bottom.equals(lastBottom)) stagnant = 0; else if (++stagnant >= 4) break;
            lastBottom = bottom;
            scanSwipeUp(); // overlapping scan swipe → don't skip a category header between frames
        }
        return out;
    }

    /**
     * Scroll to the top. The collapsing toolbar's subtitle is only shown when the
     * toolbar is fully expanded — i.e. at the very top — so it's a reliable stop
     * signal (no fixed swipe count, no over-scrolling).
     */
    @Step("Scroll Neurobooster list to top")
    public void scrollToTop() {
        for (int i = 0; i < 16 && !atTop(); i++) swipeDown();
    }

    /** The list's first category header — visible only at the very top (device-agnostic top signal). */
    static final String FIRST_CATEGORY = "Use mini-exercises for your body";

    /**
     * True when the list is at the top, i.e. the first category header is on screen.
     * Content-based (not the collapsing toolbar's subtitle, which behaves differently
     * across devices/emulators).
     */
    private boolean atTop() {
        return isCategoryVisible(FIRST_CATEGORY);
    }

    /**
     * Find a visible tile card whose subtitle matches, positioned under the given
     * category header (between it and the next header). Returns null if the header
     * or a matching in-range tile is not currently on screen.
     */
    private WebElement findVisibleTileInCategory(String category, String subtitle) {
        Integer headerY = null;
        for (WebElement h : findAllByPlatformId(ID_CATEGORY_TITLE, ID_CATEGORY_TITLE)) {
            if (category.equals(textOf(h))) { headerY = yOf(h); break; }
        }
        if (headerY == null) return null;
        Integer nextHeaderY = null;
        for (WebElement h : findAllByPlatformId(ID_CATEGORY_TITLE, ID_CATEGORY_TITLE)) {
            int hy = yOf(h);
            if (hy > headerY && (nextHeaderY == null || hy < nextHeaderY)) nextHeaderY = hy;
        }
        WebElement best = null;
        int bestY = Integer.MAX_VALUE;
        for (WebElement card : findAllByPlatformId(ID_TILE_CARD, ID_TILE_CARD)) {
            if (!subtitle.equals(descText(card, ID_TILE_SUBTITLE))) continue;
            int cy = yOf(card);
            if (cy > headerY && (nextHeaderY == null || cy < nextHeaderY) && cy < bestY) {
                best = card; bestY = cy;
            }
        }
        return best;
    }

    private List<ExtrasContentAccumulator.Row> extractVisibleFrame() {
        return extractVisibleFrame(currentFrame());
    }

    private List<ExtrasContentAccumulator.Row> extractVisibleFrame(FrameSnapshot f) {
        List<Positioned> rows = new ArrayList<>();
        for (YText h : f.headers) rows.add(new Positioned(h.y, ExtrasContentAccumulator.Row.category(h.text)));
        for (YText p : f.progress) rows.add(new Positioned(p.y, ExtrasContentAccumulator.Row.progress(p.text)));
        for (CardInfo c : f.cards) {
            if (c.title.isEmpty() && c.subtitle.isEmpty()) continue; // empty placeholder card
            rows.add(new Positioned(c.y, ExtrasContentAccumulator.Row.tile(c.title, c.subtitle, c.thumbnail, c.checkmark)));
        }
        rows.sort(Comparator.comparingInt(p -> p.y));
        List<ExtrasContentAccumulator.Row> ordered = new ArrayList<>();
        for (Positioned p : rows) ordered.add(p.row);
        return ordered;
    }

    // ──────────────────────────────────────────────
    // Single-parse frame snapshot
    //
    // While scrolling, reading the visible frame element-by-element (findElements +
    // getText + getLocation + per-card child finds) costs dozens of slow Appium
    // round-trips (~2s/frame). One getPageSource (~140ms) parsed in memory carries the
    // same data, so read-only enumeration takes ONE round-trip per frame. Falls back to
    // the element reads if the page source can't be parsed. (The click path — resolveTile
    // — still returns real WebElements and is unaffected.)
    // ──────────────────────────────────────────────

    private static final java.util.regex.Pattern BOUNDS =
            java.util.regex.Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]");

    private static final class YText {
        final int y; final String text;
        YText(int y, String text) { this.y = y; this.text = text == null ? "" : text; }
    }

    private static final class CardInfo {
        final int y; final String title, subtitle; final boolean checkmark, thumbnail;
        final int tapX, tapY; // iOS: centre coordinate to tap the tile (Android: -1, unused)
        final int nbId;       // iOS: NeuroBooster id from the thumbnail (-1 if none)
        CardInfo(int y, String title, String subtitle, boolean checkmark, boolean thumbnail) {
            this(y, title, subtitle, checkmark, thumbnail, -1, -1, -1);
        }
        CardInfo(int y, String title, String subtitle, boolean checkmark, boolean thumbnail, int tapX, int tapY, int nbId) {
            this.y = y; this.title = title == null ? "" : title; this.subtitle = subtitle == null ? "" : subtitle;
            this.checkmark = checkmark; this.thumbnail = thumbnail; this.tapX = tapX; this.tapY = tapY; this.nbId = nbId;
        }
    }

    private static final class FrameSnapshot {
        final List<YText> headers = new ArrayList<>();
        final List<YText> progress = new ArrayList<>();
        final List<CardInfo> cards = new ArrayList<>(); // sorted top-to-bottom by y
        boolean hasCategory(String name) { for (YText h : headers) if (name.equals(h.text)) return true; return false; }
        String bottomSubtitle() { return cards.isEmpty() ? "" : cards.get(cards.size() - 1).subtitle; }
        /** Progress text of the header at {@code headerY}: the nearest progress node at/below it. */
        String progressFor(int headerY) {
            YText best = null;
            for (YText p : progress) if (p.y >= headerY && (best == null || p.y < best.y)) best = p;
            return best == null ? null : best.text;
        }
    }

    /** The current visible frame from ONE page-source parse; element reads only if parsing fails. */
    private FrameSnapshot currentFrame() {
        FrameSnapshot f = isIOS() ? frameFromPageSourceIOS() : frameFromPageSource();
        if (f == null) f = frameFromElements();
        f.cards.sort(Comparator.comparingInt(c -> c.y));
        return f;
    }

    /**
     * iOS visible frame from ONE page-source parse. The app is Flutter-style: category
     * headers, tile titles/subtitles and progress are all StaticText (name==text),
     * distinguished by x-inset; tiles anchor on the thumbnail Image {@code mini_teaser_thumbnail_<nbId>};
     * discovered tiles carry a nearby {@code checkmark} Image. Tiles are opened by coordinate.
     */
    private FrameSnapshot frameFromPageSourceIOS() {
        try {
            org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new org.xml.sax.InputSource(new java.io.StringReader(driver.getPageSource())));
            org.w3c.dom.NodeList all = doc.getElementsByTagName("*");
            List<int[]> checks = new ArrayList<>();          // {y}
            List<int[]> thumbs = new ArrayList<>();           // {cx, cy, top}
            List<Object[]> texts = new ArrayList<>();         // {name, x, y}
            FrameSnapshot f = new FrameSnapshot();
            for (int i = 0; i < all.getLength(); i++) {
                org.w3c.dom.Element el = (org.w3c.dom.Element) all.item(i);
                String type = el.getTagName();
                String name = el.getAttribute("name");
                int x = iInt(el, "x"), y = iInt(el, "y"), w = iInt(el, "width"), h = iInt(el, "height");
                if (type.endsWith("StaticText")) {
                    if (name == null || name.isEmpty()) continue;
                    if (IOS_PROGRESS.matcher(name).find()) { f.progress.add(new YText(y, name)); continue; }
                    if (name.equals(IOS_TOOLBAR_SUBTITLE) || name.equals(IOS_TOOLBAR_TITLE)) continue;
                    if (x <= IOS_HEADER_MAX_X && y > 120) f.headers.add(new YText(y, name)); // category header
                    else texts.add(new Object[]{name, x, y});                                // tile title/subtitle
                } else if (type.endsWith("Image") && name != null) {
                    if (name.startsWith(IOS_THUMB_PREFIX)) {
                        int nb = -1;
                        try { nb = Integer.parseInt(name.substring(IOS_THUMB_PREFIX.length())); } catch (Exception ignored) {}
                        thumbs.add(new int[]{x + w / 2, y + h / 2, y, nb});
                    } else if (name.equals(IOS_CHECKMARK)) checks.add(new int[]{y});
                }
            }
            // Build a card per thumbnail: subtitle/title are the two indented texts just above it.
            for (int[] th : thumbs) {
                int top = th[2];
                Object[] sub = nearestTextAbove(texts, top);
                Object[] tit = sub == null ? null : nearestTextAbove(texts, (int) sub[2]);
                String subtitle = sub == null ? "" : (String) sub[0];
                String title = tit == null ? "" : (String) tit[0];
                int cardTop = tit != null ? (int) tit[2] : (sub != null ? (int) sub[2] : top);
                boolean checked = false;
                for (int[] c : checks) if (c[0] >= cardTop - 4 && c[0] <= th[2] + 120) { checked = true; break; }
                f.cards.add(new CardInfo(cardTop, title, subtitle, checked, true, th[0], th[1], th[3]));
            }
            return f;
        } catch (Exception e) {
            log.warn("iOS page-source frame parse failed ({})", e.getMessage());
            return new FrameSnapshot();
        }
    }

    private static Object[] nearestTextAbove(List<Object[]> texts, int belowY) {
        Object[] best = null; int bestY = Integer.MIN_VALUE;
        for (Object[] t : texts) {
            int ty = (int) t[2];
            if (ty < belowY && ty > bestY) { best = t; bestY = ty; }
        }
        return best;
    }

    private static int iInt(org.w3c.dom.Element el, String attr) {
        try { return Integer.parseInt(el.getAttribute(attr)); } catch (Exception e) { return 0; }
    }

    private FrameSnapshot frameFromPageSource() {
        try {
            org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new org.xml.sax.InputSource(new java.io.StringReader(driver.getPageSource())));
            FrameSnapshot f = new FrameSnapshot();
            org.w3c.dom.NodeList all = doc.getElementsByTagName("*");
            for (int i = 0; i < all.getLength(); i++) {
                org.w3c.dom.Element el = (org.w3c.dom.Element) all.item(i);
                String rid = el.getAttribute("resource-id");
                if (ID_CATEGORY_TITLE.equals(rid)) f.headers.add(new YText(topY(el), el.getAttribute("text")));
                else if (ID_CATEGORY_PROGRESS_TEXT.equals(rid)) f.progress.add(new YText(topY(el), el.getAttribute("text")));
                else if (ID_TILE_CARD.equals(rid)) f.cards.add(parseCard(el));
            }
            return f;
        } catch (Exception e) {
            log.warn("Page-source frame parse failed ({}); using element reads", e.getMessage());
            return null;
        }
    }

    private CardInfo parseCard(org.w3c.dom.Element card) {
        String title = "", sub = ""; boolean check = false, thumb = false;
        org.w3c.dom.NodeList kids = card.getElementsByTagName("*");
        for (int i = 0; i < kids.getLength(); i++) {
            String rid = ((org.w3c.dom.Element) kids.item(i)).getAttribute("resource-id");
            if (ID_TILE_TITLE.equals(rid)) title = ((org.w3c.dom.Element) kids.item(i)).getAttribute("text");
            else if (ID_TILE_SUBTITLE.equals(rid)) sub = ((org.w3c.dom.Element) kids.item(i)).getAttribute("text");
            else if (ID_TILE_CHECKMARK.equals(rid)) check = true;
            else if (ID_TILE_THUMBNAIL.equals(rid)) thumb = true;
        }
        return new CardInfo(topY(card), title, sub, check, thumb);
    }

    private static int topY(org.w3c.dom.Element el) {
        java.util.regex.Matcher m = BOUNDS.matcher(el.getAttribute("bounds"));
        return m.find() ? Integer.parseInt(m.group(2)) : Integer.MAX_VALUE;
    }

    /** Fallback frame via element reads — only used when page-source parsing fails. */
    private FrameSnapshot frameFromElements() {
        FrameSnapshot f = new FrameSnapshot();
        for (WebElement e : findAllByPlatformId(ID_CATEGORY_TITLE, ID_CATEGORY_TITLE))
            f.headers.add(new YText(yOf(e), textOf(e)));
        for (WebElement e : findAllByPlatformId(ID_CATEGORY_PROGRESS_TEXT, ID_CATEGORY_PROGRESS_TEXT))
            f.progress.add(new YText(yOf(e), textOf(e)));
        for (WebElement card : findAllByPlatformId(ID_TILE_CARD, ID_TILE_CARD))
            f.cards.add(new CardInfo(yOf(card), descText(card, ID_TILE_TITLE), descText(card, ID_TILE_SUBTITLE),
                    !card.findElements(AppiumBy.id(ID_TILE_CHECKMARK)).isEmpty(),
                    !card.findElements(AppiumBy.id(ID_TILE_THUMBNAIL)).isEmpty()));
        return f;
    }

    private static final class Positioned {
        final int y;
        final ExtrasContentAccumulator.Row row;
        Positioned(int y, ExtrasContentAccumulator.Row row) { this.y = y; this.row = row; }
    }

    private int yOf(WebElement e) {
        try { return e.getLocation().getY(); } catch (Exception ex) { return Integer.MAX_VALUE; }
    }
    private String textOf(WebElement e) {
        try { return e.getText() == null ? "" : e.getText(); } catch (Exception ex) { return ""; }
    }
    private String descText(WebElement card, String id) {
        try { return textOf(card.findElement(AppiumBy.id(id))); }
        catch (NoSuchElementException ex) { return ""; }
    }
}
