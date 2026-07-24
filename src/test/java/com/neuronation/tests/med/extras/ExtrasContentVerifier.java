package com.neuronation.tests.med.extras;

import com.neuronation.base.Screens;
import com.neuronation.pages.med.extras.ExtrasScreen;
import com.neuronation.pages.med.extras.NeuroBoosterDetailScreen;
import com.neuronation.pages.med.extras.NeuroBoosterQuizResultScreen;
import com.neuronation.pages.med.extras.NeuroBoosterQuizScreen;
import com.neuronation.testdata.NeuroBoosterCatalog;
import com.neuronation.testdata.NeuroBoosterCatalog.Answer;
import com.neuronation.testdata.NeuroBoosterCatalog.Category;
import com.neuronation.testdata.NeuroBoosterCatalog.Question;
import com.neuronation.testdata.NeuroBoosterCatalog.Tile;
import com.neuronation.testdata.NeuroBoosterLabels;
import io.qameta.allure.Step;
import org.testng.asserts.SoftAssert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reusable, catalog-driven Extras (Neurobooster) verifier — the single source of truth for both the
 * fast {@link MedExtrasSmokeTest} and the exhaustive per-flow content regression in the E2E happy path.
 *
 * <p>All assertions are <b>soft</b> — the caller owns the {@link SoftAssert} and decides when to
 * {@code assertAll()}, so one broken tile never halts the rest of a flow.
 *
 * <p><b>Per-tile lifecycle</b> (regression, in catalog order, ONE scroll per tile — no top↔bottom
 * bounce): section header (progress {@code 0 / N}, N == tile count, "?" help), then for each tile a
 * single locate that reads the initial listing state (title/subtitle/image, no tick) AND opens it →
 * detail (heading/body/button/image) → COMPLETE (body = CTA; cognitive = video + quiz pass). The
 * count is asserted by index (read once at the header after each completion). Ticks are verified in
 * ONE refreshed sweep at the end — the app only repaints a tile's checkmark when the list reloads,
 * so a per-tile in-session read is both wasteful and unreliable.
 */
public class ExtrasContentVerifier {

    private static final Pattern PROGRESS = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    private final Screens screens;
    private final SoftAssert softAssert;
    private final NeuroBoosterCatalog catalog;
    private final NeuroBoosterLabels labels;
    private final boolean android;

    public ExtrasContentVerifier(Screens screens, SoftAssert softAssert,
                                 NeuroBoosterCatalog catalog, NeuroBoosterLabels labels, String platform) {
        this.screens = screens;
        this.softAssert = softAssert;
        this.catalog = catalog;
        this.labels = labels;
        this.android = "android".equalsIgnoreCase(platform);
    }

    private void applySectionOrder() {
        screens.extras().setSectionOrder(
                catalog.categories.stream().map(c -> c.title).collect(Collectors.toList()));
        // Down-only tile/header location — deterministic on deep sections (no planner oscillation).
        screens.extras().setMonotonicScan(true);
    }

    // ══════════════════════════════════════════════════
    //  Structure (smoke) — full top-to-bottom capture
    // ══════════════════════════════════════════════════

    @Step("Structure: sections in JSON order + every tile's title/subtitle")
    public void verifyStructure() {
        applySectionOrder();
        Map<String, String> cap = screens.extras().captureContent();
        for (int i = 0; i < catalog.categories.size(); i++) {
            softAssert.assertEquals(cap.get("category[" + i + "].title"),
                    catalog.categories.get(i).title, "category[" + i + "].title (order)");
        }
        Map<String, List<Tile>> byCat = new LinkedHashMap<>();
        for (Tile t : catalog.tiles) byCat.computeIfAbsent(t.category, k -> new ArrayList<>()).add(t);
        for (int i = 0; i < catalog.categories.size(); i++) {
            Map<String, String> capTiles = new LinkedHashMap<>();
            int capCount = Integer.parseInt(cap.getOrDefault("category[" + i + "].tileCount", "0"));
            for (int j = 0; j < capCount; j++) {
                String b = "category[" + i + "].tile[" + j + "]";
                capTiles.put(cap.get(b + ".subtitle"), cap.get(b + ".title"));
            }
            for (Tile t : byCat.getOrDefault(catalog.categories.get(i).title, List.of())) {
                boolean present = capTiles.containsKey(t.listSubtitle);
                softAssert.assertTrue(present, "tile '" + t.listSubtitle + "' present under '"
                        + catalog.categories.get(i).title + "'");
                if (present) {
                    softAssert.assertEquals(capTiles.get(t.listSubtitle), t.listTitle,
                            "title for '" + t.listSubtitle + "'");
                }
            }
        }
        screens.extras().scrollToTop();
    }

    // ══════════════════════════════════════════════════
    //  Per-flow content regression
    // ══════════════════════════════════════════════════

    /** MCI-only: the listing must contain EXACTLY the given section, then run its full lifecycle. */
    @Step("Verify Extras exposes ONLY the '{sectionTitle}' section, then verify it")
    public void verifyOnlySection(String sectionTitle) {
        applySectionOrder();
        Map<String, String> cap = screens.extras().captureContent();
        int count = Integer.parseInt(cap.getOrDefault("categoryCount", "0"));
        softAssert.assertEquals(count, 1,
                "MCI account should expose exactly 1 Extras section (body only), found " + count);
        softAssert.assertEquals(cap.get("category[0].title"), sectionTitle,
                "the only Extras section should be '" + sectionTitle + "'");
        screens.extras().scrollToTop();
        verifySections(List.of(sectionTitle));
    }

    /** Full lifecycle for each section (header + per-tile detail/complete/count), then ONE tick sweep. */
    @Step("Verify Extras sections: {sectionTitles}")
    public void verifySections(List<String> sectionTitles) {
        applySectionOrder();
        for (String sec : sectionTitles) verifySectionLifecycle(sec);
        verifyAllTicks(sectionTitles);
    }

    /**
     * Smoke: verify a curated set of representative tiles the RELIABLE (regression) way. Processes
     * them in catalog top-to-bottom order via the shared open→verify-detail→complete path (the SAME
     * engine the per-flow regression uses), then confirms ticks for the discovered tiles in ONE
     * refreshed sweep.
     *
     * <p>Deliberately does NOT read {@code getCategoryProgress} per tile nor check the tick in-session
     * — both flake: the post-completion count re-read races the re-rendering listing ("category not
     * found: Use mini-exercises for your body"), and a tile's tick a11y node only appears on reload.
     * The old per-tile {@link #completeSimple}/{@link #completeCognitive} did exactly those and were
     * the source of the build #81 smoke flakiness.
     *
     * <p>{@code failTiles} are completed with a FAILING quiz (a failed quiz does NOT mark the tile
     * discovered, so those are excluded from the tick sweep).
     */
    @Step("Smoke: verify representative tiles (reliable, in order)")
    public void verifySmokeTiles(List<Tile> tiles, java.util.Set<Tile> failTiles) {
        applySectionOrder();
        List<Tile> ordered = new ArrayList<>(tiles);
        ordered.sort(java.util.Comparator
                .comparingInt((Tile t) -> indexOfSection(t.category))
                .thenComparingInt(t -> catalog.tiles.indexOf(t)));
        List<Tile> discovered = new ArrayList<>();
        for (Tile t : ordered) {
            boolean fail = failTiles.contains(t);
            ExtrasScreen.TileCard card = "COGNITIVE_HEALTH".equals(t.type)
                    ? openVerifyCompleteCognitive(t, !fail)
                    : openVerifyCompleteSimple(t);
            verifyInitialListing(card, t, t.category);
            if (!fail) discovered.add(t);   // a failed quiz leaves the tile un-discovered
        }
        verifyTicksAndProgress(discovered);
    }

    private int indexOfSection(String title) {
        for (int i = 0; i < catalog.categories.size(); i++)
            if (catalog.categories.get(i).title.equals(title)) return i;
        return Integer.MAX_VALUE;
    }

    /**
     * ONE deferred sweep after a listing reload (Training→Extras, so checkmarks repaint as a11y nodes),
     * verifying — for EVERY section in catalog order — both:
     *   (a) each completed tile in the section shows a tick, and
     *   (b) the section's "X / N discovered" progress: X == the number of representative tiles COMPLETED
     *       in that section (0 for untouched sections; a FAILED quiz does not count), N == its tile count.
     * Both read from the SAME {@link ExtrasScreen#captureContent()} — no extra scrolling, no per-tile
     * getCategoryProgress re-read (that was the flaky "category not found"). Keyed by section because
     * cognitive subtitles repeat across sections.
     */
    private void verifyTicksAndProgress(List<Tile> discovered) {
        screens.dashboard().tapTrainingTab();
        screens.dashboard().tapExtrasTab();
        screens.extras().waitForScreen();
        applySectionOrder();
        Map<String, String> cap = screens.extras().captureContent();

        Map<String, String> progressBySection = new HashMap<>();
        Map<String, Map<String, String>> tickBySection = new HashMap<>();
        int catCount = Integer.parseInt(cap.getOrDefault("categoryCount", "0"));
        for (int i = 0; i < catCount; i++) {
            String secTitle = cap.get("category[" + i + "].title");
            progressBySection.put(secTitle, cap.getOrDefault("category[" + i + "].progress", ""));
            Map<String, String> m = tickBySection.computeIfAbsent(secTitle, k -> new HashMap<>());
            int tc = Integer.parseInt(cap.getOrDefault("category[" + i + "].tileCount", "0"));
            for (int j = 0; j < tc; j++) {
                String b = "category[" + i + "].tile[" + j + "]";
                m.put(cap.get(b + ".subtitle"), cap.getOrDefault(b + ".discovered", "false"));
            }
        }

        // In catalog (top-to-bottom) order: progress first, then the section's completed ticks.
        for (Category c : catalog.categories) {
            String sec = c.title;
            int expDiscovered = (int) discovered.stream().filter(t -> sec.equals(t.category)).count();
            int expTiles = tilesOf(sec).size();
            String prog = progressBySection.get(sec);
            softAssert.assertNotNull(prog, "section captured [" + sec + "]");
            if (prog != null) {
                softAssert.assertEquals(discoveredCount(prog), expDiscovered, "discovered count [" + sec + "]");
                softAssert.assertEquals(denominator(prog), expTiles, "section tile count N [" + sec + "]");
            }
            Map<String, String> ticks = tickBySection.getOrDefault(sec, java.util.Map.of());
            for (Tile t : discovered) {
                if (sec.equals(t.category)) {
                    softAssert.assertEquals(ticks.get(t.listSubtitle), "true",
                            "tile shows a tick after completion [" + sec + " / " + t.listSubtitle + "]");
                }
            }
        }
    }

    @Step("Section '{section}': header + every tile initial→detail→complete→count")
    private void verifySectionLifecycle(String section) {
        Category cat = category(section);
        List<Tile> tiles = tilesOf(section);

        // header: progress "0 / N discovered", N == tile count, "?" help
        String prog0 = screens.extras().getCategoryProgress(section);
        if (cat != null && cat.progress != null) {
            softAssert.assertEquals(prog0, cat.progress, "initial progress [" + section + "]");
        }
        softAssert.assertEquals(denominator(prog0), tiles.size(),
                "section tile count (progress denominator N) [" + section + "]");
        softAssert.assertEquals(discoveredCount(prog0), 0, "initial discovered = 0 [" + section + "]");

        screens.extras().openCategoryHelp(section);
        softAssert.assertEquals(screens.extras().getHelpTooltipTitle(),
                cat != null ? cat.helpTitle : null, "help title [" + section + "]");
        softAssert.assertEquals(norm(screens.extras().getHelpTooltipMessage()),
                norm(cat != null ? cat.helpMessage : null), "help message [" + section + "]");
        screens.extras().dismissHelpTooltip();

        // each tile IN ORDER: one locate (read initial + open) → detail → complete.
        // The count is NOT read per tile (that forced a climb back UP to the section header every
        // time); it is read ONCE below. The tick is NOT read per tile either: the app DRAWS the
        // checkmark immediately (visible to a human) but only exposes it as an ACCESSIBILITY NODE —
        // all Appium can read — when the list RELOADS. So per-tile in-session reads fail (measured:
        // ~11/19 fresh body tiles undetectable); the tick is verified in one refreshed sweep below.
        for (Tile tile : tiles) {
            ExtrasScreen.TileCard card = "COGNITIVE_HEALTH".equals(tile.type)
                    ? openVerifyCompleteCognitive(tile, true)
                    : openVerifyCompleteSimple(tile);
            verifyInitialListing(card, tile, section);
        }

        // count ONCE, after the whole section: every tile is now discovered → "N / N".
        int finalCount = discoveredCount(screens.extras().getCategoryProgress(section));
        softAssert.assertEquals(finalCount, tiles.size(),
                "discovered count = N after completing all tiles [" + section + "]");
    }

    /**
     * Verify every completed tile shows a tick — in ONE sweep after a listing REFRESH (leave to the
     * Training tab and back). The app DRAWS the checkmark immediately (visible to a human), but only
     * exposes it as an ACCESSIBILITY node — all Appium can read — when the list RELOADS; measured
     * ~11/19 fresh body ticks undetectable in-session. So this refreshed sweep is required to read
     * the persisted discovered state (per-tile in-session reads fail, not because the tick is absent).
     */
    @Step("Verify all completed tiles show a tick (refreshed sweep): {sections}")
    private void verifyAllTicks(List<String> sections) {
        screens.dashboard().tapTrainingTab();   // leave Extras
        screens.dashboard().tapExtrasTab();      // re-enter → list reloads, ticks repaint as a11y nodes
        screens.extras().waitForScreen();
        applySectionOrder();
        Map<String, String> cap = screens.extras().captureContent();

        // (sectionTitle -> (subtitle -> discovered)). Keyed by SECTION too: cognitive subtitles
        // ("Psychoeducational", "Reflection and MKT", …) REPEAT across sections, so a flat
        // subtitle->discovered map collapses them and every section's tick resolves to whichever
        // section was captured last (the flow3 false "tick not found" bug).
        Map<String, Map<String, String>> bySection = new HashMap<>();
        int catCount = Integer.parseInt(cap.getOrDefault("categoryCount", "0"));
        for (int i = 0; i < catCount; i++) {
            String secTitle = cap.get("category[" + i + "].title");
            Map<String, String> m = bySection.computeIfAbsent(secTitle, k -> new HashMap<>());
            int tc = Integer.parseInt(cap.getOrDefault("category[" + i + "].tileCount", "0"));
            for (int j = 0; j < tc; j++) {
                String b = "category[" + i + "].tile[" + j + "]";
                m.put(cap.get(b + ".subtitle"), cap.getOrDefault(b + ".discovered", "false"));
            }
        }
        for (String sec : sections) {
            Map<String, String> m = bySection.getOrDefault(sec, java.util.Map.of());
            for (Tile t : tilesOf(sec)) {
                softAssert.assertEquals(m.get(t.listSubtitle), "true",
                        "tile shows a tick after completion [" + sec + " / " + t.listSubtitle + "]");
            }
        }
    }

    /**
     * Assert a tile's PRISTINE pre-open listing snapshot: title, subtitle, no tick. The listing
     * THUMBNAIL is intentionally not asserted here — it lazy-loads as the tile scrolls in and is
     * unreliable mid-scroll; the tile's image is verified authoritatively on the detail screen
     * (see {@link #verifyDetail}), where it is settled.
     */
    private void verifyInitialListing(ExtrasScreen.TileCard card, Tile t, String section) {
        String where = "[" + section + " / " + t.listSubtitle + "]";
        softAssert.assertEquals(card.title, t.listTitle, "listing title " + where);
        softAssert.assertEquals(card.subtitle, t.listSubtitle, "listing subtitle " + where);
        softAssert.assertFalse(card.discovered, "tile NOT discovered before completion (no tick) " + where);
    }


    // ══════════════════════════════════════════════════
    //  Completion mechanics (shared)
    // ══════════════════════════════════════════════════

    /** Open (reading the pre-open card) → verify detail → tap CTA → back to listing. Returns the card. */
    private ExtrasScreen.TileCard openVerifyCompleteSimple(Tile tile) {
        ExtrasScreen.TileCard card =
                screens.extras().openTileAndReadCard(tile.category, tile.listSubtitle, tile.nbId);
        NeuroBoosterDetailScreen detail = screens.neuroBoosterDetail();
        detail.waitForScreen();
        verifyDetail(detail, tile);
        detail.tapPrimaryCta();
        screens.extras().waitForScreen();
        return card;
    }

    /** Open → detail → video → quiz (pass/fail) → result → back to listing. Returns the pre-open card. */
    private ExtrasScreen.TileCard openVerifyCompleteCognitive(Tile tile, boolean pass) {
        ExtrasScreen.TileCard card =
                screens.extras().openTileAndReadCard(tile.category, tile.listSubtitle, tile.nbId);
        NeuroBoosterDetailScreen detail = screens.neuroBoosterDetail();
        detail.waitForScreen();
        verifyDetail(detail, tile);
        detail.tapPrimaryCta();

        var video = screens.neuroBoosterVideo();
        video.waitForScreen();
        video.waitForVideoLoaded();
        softAssert.assertTrue(video.hasSubtitleTrack(), "video subtitle track present");
        video.fastForwardToEnd();

        NeuroBoosterQuizScreen quiz = screens.neuroBoosterQuiz();
        quiz.waitForIntro();
        softAssert.assertEquals(quiz.getIntroHeading(), labels.quiz.introTitle, "quiz intro heading");
        quiz.tapStartQuiz();
        quiz.waitForQuestion();
        int guard = 0;
        while (quiz.isQuestionDisplayed() && guard++ < tile.quiz.questionCount + 2) {
            softAssert.assertTrue(quiz.hasProgressBar(), "quiz progress bar");
            softAssert.assertTrue(quiz.getAnswerCount() >= 2, "quiz has >= 2 answers");
            Question cq = findQuestion(tile.quiz, quiz.getQuestionText());
            Answer pick = pass ? cq.correct() : cq.firstWrong();
            quiz.selectAnswerByText(pick.text);
            softAssert.assertTrue(quiz.isFeedbackShown(), "quiz feedback shown");
            verifyAnswerFeedback(cq, pick, tile.listSubtitle);
            quiz.tapContinue();
        }

        NeuroBoosterQuizResultScreen result = screens.neuroBoosterQuizResult();
        result.waitForScreen();
        softAssert.assertTrue(result.getScoreLabel().length() > 0, "result score present");
        softAssert.assertTrue(result.getResultContent().length() > 0, "result content present");
        if (pass) {
            softAssert.assertTrue(result.isPass(), "PASS should show fireworks");
            softAssert.assertTrue(result.hasImage(), "PASS result image present");
            softAssert.assertEquals(result.getResultTitle(), labels.result.titlePass, "pass title");
            softAssert.assertEquals(result.getActionLabel(), labels.result.actionPass, "pass action");
            result.tapAction();
            screens.extras().waitForScreen();
        } else {
            softAssert.assertFalse(result.isPass(), "FAIL should not show fireworks");
            softAssert.assertEquals(result.getResultTitle(), labels.result.titleFail, "fail title");
            softAssert.assertEquals(result.getActionLabel(), labels.result.actionFail, "fail action");
            result.tapAction();
            softAssert.assertTrue(screens.neuroBoosterDetail().isDisplayed(), "fail -> back to detail");
            screens.neuroBoosterDetail().tapSecondaryCta(); // "Let me do this later" -> back to listing
            screens.extras().waitForScreen();
        }
        return card;
    }

    // ── smoke entry points (one representative tile each; per-tile tick+count is fine at that scale) ──

    /** Smoke: EXERCISE/KNOWLEDGE tile — detail → complete → tick + count +1. */
    public void completeSimple(Tile tile) {
        int before = discoveredCount(screens.extras().getCategoryProgress(tile.category));
        openVerifyCompleteSimple(tile);
        softAssert.assertTrue(screens.extras().isTileDiscovered(tile.category, tile.listSubtitle, tile.nbId),
                "tick[" + tile.type + "] after completion");
        softAssert.assertEquals(discoveredCount(screens.extras().getCategoryProgress(tile.category)),
                before + 1, "discovered count +1 [" + tile.type + "]");
    }

    /** Smoke: COGNITIVE tile — detail → video → quiz (pass/fail) → result → (pass) tick + count +1. */
    public void completeCognitive(Tile tile, boolean pass) {
        int before = pass ? discoveredCount(screens.extras().getCategoryProgress(tile.category)) : -1;
        openVerifyCompleteCognitive(tile, pass);
        if (pass) {
            softAssert.assertTrue(screens.extras().isTileDiscovered(tile.category, tile.listSubtitle, tile.nbId),
                    "pass -> tile discovered (tick)");
            softAssert.assertEquals(discoveredCount(screens.extras().getCategoryProgress(tile.category)),
                    before + 1, "pass -> discovered count +1");
        }
    }

    // ══════════════════════════════════════════════════
    //  Detail + quiz-feedback verification (shared)
    // ══════════════════════════════════════════════════

    /** Detail screen: heading, image (Android), EXACT body, primary button. */
    public void verifyDetail(NeuroBoosterDetailScreen detail, Tile tile) {
        softAssert.assertEquals(detail.getHeading(), tile.detailHeading, "detail title[" + tile.type + "]");
        if (android) {
            softAssert.assertEquals(detail.hasImage(), tile.hasImage, "detail image[" + tile.type + "]");
        }
        softAssert.assertTrue(detail.getBodyText().trim().length() > 0, "detail content present[" + tile.type + "]");
        if (tile.detailBodyContains != null && !tile.detailBodyContains.isEmpty()) {
            softAssert.assertEquals(norm(detail.getBodyText()), norm(tile.detailBodyContains.get(0)),
                    "detail body (exact)[" + tile.type + "]");
        }
        softAssert.assertEquals(detail.getPrimaryCtaText(), labels.detail.primaryCtaFor(tile.type),
                "detail button[" + tile.type + "]");
    }

    public void verifyAnswerFeedback(Question cq, Answer pick, String tileSub) {
        NeuroBoosterQuizScreen quiz = screens.neuroBoosterQuiz();
        String where = "[" + tileSub + " | " + cq.prompt + "]";
        Answer correct = cq.correct();
        String correctPrefix = labels.quiz.answerCorrectPrefix;

        if (!android) {
            Map<String, String[]> snap = quiz.iosFeedbackSnapshot();
            String status = snap.getOrDefault("", new String[]{""})[0];
            if (!status.isEmpty()) {
                softAssert.assertEquals(status, pick.correct ? "success" : "incorrect", "iOS question outcome " + where);
            }
            String[] sel = snap.get(NeuroBoosterQuizScreen.normText(pick.text));
            if (sel != null && !sel[0].isEmpty()) {
                String want = pick.correct ? correctPrefix : labels.quiz.answerWrongPrefix;
                softAssert.assertTrue(sel[0].startsWith(want),
                        "selected answer marked '" + want + "' " + where + ", got '" + sel[0] + "'");
            }
            String[] cor = snap.get(NeuroBoosterQuizScreen.normText(correct.text));
            if (cor != null && !cor[0].isEmpty()) {
                softAssert.assertTrue(cor[0].startsWith(correctPrefix),
                        "correct answer revealed with '" + correctPrefix + "' " + where + ", got '" + cor[0] + "'");
            }
            if (cq.answers != null) {
                for (Answer a : cq.answers) {
                    if (a.explanation == null || a.explanation.isEmpty()) continue;
                    String[] e = snap.get(NeuroBoosterQuizScreen.normText(a.text));
                    if (e != null && !e[1].isEmpty()) {
                        softAssert.assertEquals(norm(e[1]), norm(a.explanation), "explanation " + where + " answer '" + a.text + "'");
                    }
                }
            }
            return;
        }

        String selMarker = quiz.getResultTitleForAnswer(pick.text);
        if (!selMarker.isEmpty()) {
            String want = pick.correct ? correctPrefix : labels.quiz.answerWrongPrefix;
            softAssert.assertTrue(selMarker.startsWith(want),
                    "selected answer marked '" + want + "' " + where + ", got '" + selMarker + "'");
        }
        String corMarker = quiz.getResultTitleForAnswer(correct.text);
        if (!corMarker.isEmpty()) {
            softAssert.assertTrue(corMarker.startsWith(correctPrefix),
                    "correct answer revealed with '" + correctPrefix + "' " + where + ", got '" + corMarker + "'");
        }
        if (cq.answers != null) {
            for (Answer a : cq.answers) {
                if (a.explanation == null || a.explanation.isEmpty()) continue;
                String shown = quiz.getExplanationForAnswer(a.text);
                if (!shown.isEmpty()) {
                    softAssert.assertEquals(norm(shown), norm(a.explanation),
                            "explanation " + where + " answer '" + a.text + "'");
                }
            }
        }
    }

    // ══════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════

    private Question findQuestion(NeuroBoosterCatalog.Quiz quiz, String prompt) {
        String p = norm(prompt);
        for (Question q : quiz.questions) if (norm(q.prompt).equals(p)) return q;
        for (Question q : quiz.questions)
            if (!p.isEmpty() && (norm(q.prompt).contains(p) || p.contains(norm(q.prompt)))) return q;
        throw new IllegalStateException("Displayed question not in catalog: '" + prompt + "'");
    }

    private Category category(String title) {
        for (Category c : catalog.categories) if (c.title.equals(title)) return c;
        return null;
    }

    private List<Tile> tilesOf(String category) {
        List<Tile> out = new ArrayList<>();
        for (Tile t : catalog.tiles) if (category.equals(t.category)) out.add(t);
        return out;
    }

    private int discoveredCount(String progress) {
        Matcher m = PROGRESS.matcher(progress == null ? "" : progress);
        if (m.find()) return Integer.parseInt(m.group(1));
        throw new IllegalStateException("Cannot parse category progress: " + progress);
    }

    private int denominator(String progress) {
        Matcher m = PROGRESS.matcher(progress == null ? "" : progress);
        if (m.find()) return Integer.parseInt(m.group(2));
        throw new IllegalStateException("Cannot parse category progress: " + progress);
    }

    static String norm(String s) {
        if (s == null) return "";
        return s.replace('’', '\'').replace('‘', '\'')
                .replace('“', '"').replace('”', '"')
                .replace(' ', ' ').replaceAll("\\s+", " ").trim();
    }
}
