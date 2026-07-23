package com.neuronation.tests.med.extras;

import com.neuronation.base.BaseTest;
import com.neuronation.helpers.CatalogProvider;
import com.neuronation.pages.med.extras.NeuroBoosterQuizResultScreen;
import com.neuronation.pages.med.extras.NeuroBoosterQuizScreen;
import com.neuronation.testdata.Features;
import com.neuronation.testdata.NeuroBoosterCatalog;
import com.neuronation.testdata.NeuroBoosterLabels;
import io.qameta.allure.*;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NeuroBooster **completion** behaviour — mutating, so every method registers a
 * FRESH Parkinson's account (all 0/N) via {@code completeFullFlow(flow, true)}.
 *
 * <ul>
 *   <li>EXERCISE / KNOWLEDGE: tapping "I've completed this, continue" marks the
 *       tile discovered (count +1, checkmark) and returns to the listing.</li>
 *   <li>COGNITIVE_HEALTH: detail → video (fast-forward) → quiz → result; completing
 *       the attempt (pass or fail) marks the tile discovered.</li>
 * </ul>
 *
 * Pass/fail-outcome tests are data-driven on {@code quiz.correctIndices} and are
 * SKIPPED until that content is filled in the catalog.
 */
@Epic("NeuroNation MED App")
@Feature("Extras / Neurobooster")
public class MedExtrasCompletionTest extends BaseTest {

    private static final String CONDITION = "parkinson";
    private static final String FLOW = "flow1_password_morning_skip";
    private static final Pattern PROGRESS = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    private NeuroBoosterCatalog catalog;
    private NeuroBoosterLabels labels;

    @BeforeMethod(alwaysRun = true)
    public void navigateToExtras() {
        medFlow.completeFullFlow(FLOW, true);
        screens.dashboard().waitForScreen();
        screens.dashboard().tapExtrasTab();
        screens.extras().waitForScreen();
        catalog = CatalogProvider.load(context.getLanguage(), CONDITION);
        screens.extras().setSectionOrder(
                catalog.categories.stream().map(c -> c.title).collect(java.util.stream.Collectors.toList()));
        labels = CatalogProvider.labels(context.getLanguage());
    }

    // ---------- EXERCISE / KNOWLEDGE (no quiz) ----------

    @Test(groups = {Features.MED, Features.EXTRAS, Features.REGRESSION, Features.CRITICAL})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Completing an EXERCISE NeuroBooster marks it discovered")
    public void testExercise_complete_marksDiscovered() {
        completeSimpleAndAssertDiscovered(smokeOf("EXERCISE"));
    }

    @Test(groups = {Features.MED, Features.EXTRAS, Features.REGRESSION, Features.CRITICAL})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Completing a KNOWLEDGE NeuroBooster marks it discovered")
    public void testKnowledge_complete_marksDiscovered() {
        completeSimpleAndAssertDiscovered(smokeOf("KNOWLEDGE"));
    }

    /** EXERCISE/KNOWLEDGE: read count, open, tap primary CTA, assert count+1 + checkmark. */
    @Step("Complete simple NeuroBooster and assert discovered: {tile.category} / {tile.listSubtitle}")
    private void completeSimpleAndAssertDiscovered(NeuroBoosterCatalog.Tile tile) {
        int before = discoveredCount(screens.extras().getCategoryProgress(tile.category));

        screens.extras().openTile(tile.category, tile.listSubtitle);
        screens.neuroBoosterDetail().waitForScreen();
        softAssert.assertEquals(screens.neuroBoosterDetail().getPrimaryCtaText(),
                labels.detail.primaryCtaFor(tile.type), "primaryCta");
        screens.neuroBoosterDetail().tapPrimaryCta();

        screens.extras().waitForScreen();
        int after = discoveredCount(screens.extras().getCategoryProgress(tile.category));
        softAssert.assertEquals(after, before + 1, "discovered count should increment by 1");
        softAssert.assertTrue(screens.extras().isTileDiscovered(tile.category, tile.listSubtitle),
                "tile should show a checkmark after completion");
        softAssert.assertAll();
    }

    // ---------- COGNITIVE_HEALTH (video + quiz) ----------

    @Test(groups = {Features.MED, Features.EXTRAS, Features.REGRESSION, Features.CRITICAL})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Cognitive-Health video+quiz: structural walkthrough marks tile discovered")
    public void testCognitiveHealth_quizWalkthrough() {
        NeuroBoosterCatalog.Tile tile = smokeOf("COGNITIVE_HEALTH");
        openVideoAndReachQuiz(tile);

        NeuroBoosterQuizScreen quiz = screens.neuroBoosterQuiz();
        quiz.tapStartQuiz();
        quiz.waitForQuestion();

        int answered = 0;
        for (int q = 0; q < 12 && quiz.isQuestionDisplayed(); q++) {
            softAssert.assertTrue(quiz.hasProgressBar(), "question " + q + " should show a progress bar");
            softAssert.assertTrue(quiz.getAnswerCount() >= 2, "question " + q + " should have >= 2 answers");
            quiz.selectAnswer(0); // fixed structural strategy
            softAssert.assertTrue(quiz.isFeedbackShown(), "feedback should appear after selecting");
            String fb = quiz.getFeedbackTitle();
            softAssert.assertTrue(
                    fb.startsWith(labels.quiz.answerCorrectPrefix) || fb.startsWith(labels.quiz.answerWrongPrefix),
                    "feedback title should start with ✓ or ✗, got: " + fb);
            quiz.tapContinue();
            answered++;
        }
        softAssert.assertTrue(answered >= 1, "should have answered at least one question");

        NeuroBoosterQuizResultScreen result = screens.neuroBoosterQuizResult();
        result.waitForScreen();
        softAssert.assertTrue(result.getScoreLabel().length() > 0, "result should show a score");
        result.tapAction(); // exits result (Continue -> listing, or Try again -> detail)

        // Discovery is marked by attempting, regardless of score.
        softAssert.assertTrue(screens.extras().isTileDiscovered(tile.category, tile.listSubtitle),
                "cognitive-health tile should be discovered after attempting the quiz");
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.EXTRAS, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Cognitive-Health quiz PASS: correct answers show ✓ + explanation, then 'Great job!' + fireworks, returns to listing")
    public void testCognitiveHealth_quizPass() {
        NeuroBoosterCatalog.Tile tile = requireFilledQuiz("COGNITIVE_HEALTH");
        openVideoAndReachQuiz(tile);
        answerAllVerifying(tile.quiz, true); // positive path: pick + verify each correct answer

        NeuroBoosterQuizResultScreen result = screens.neuroBoosterQuizResult();
        result.waitForScreen();
        softAssert.assertTrue(result.isPass(), "pass should show the fireworks overlay");
        softAssert.assertEquals(result.getResultTitle(), labels.result.titlePass, "pass title");
        softAssert.assertEquals(result.getActionLabel(), labels.result.actionPass, "pass action label");
        result.tapAction();
        screens.extras().waitForScreen(); // pass -> Extras listing
        softAssert.assertTrue(screens.extras().isTileDiscovered(tile.category, tile.listSubtitle), "tile discovered");
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.EXTRAS, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Cognitive-Health quiz FAIL: wrong answers show ✗ + explanation, then 'Try again', returns to detail")
    public void testCognitiveHealth_quizFail() {
        NeuroBoosterCatalog.Tile tile = requireFilledQuiz("COGNITIVE_HEALTH");
        openVideoAndReachQuiz(tile);
        answerAllVerifying(tile.quiz, false); // negative path: pick + verify each wrong answer

        NeuroBoosterQuizResultScreen result = screens.neuroBoosterQuizResult();
        result.waitForScreen();
        softAssert.assertFalse(result.isPass(), "fail should NOT show fireworks");
        softAssert.assertEquals(result.getResultTitle(), labels.result.titleFail, "fail title");
        softAssert.assertEquals(result.getActionLabel(), labels.result.actionFail, "fail action label");
        result.tapAction(); // fail -> back to the NB detail page
        softAssert.assertTrue(screens.neuroBoosterDetail().isDisplayed(),
                "Try again should land on the NB detail page");
        softAssert.assertAll();
    }

    // ---------- helpers ----------

    /** Open a COGNITIVE_HEALTH tile, launch the video, fast-forward, and land on the quiz intro. */
    @Step("Open video and reach quiz: {tile.category} / {tile.listSubtitle}")
    private void openVideoAndReachQuiz(NeuroBoosterCatalog.Tile tile) {
        screens.extras().openTile(tile.category, tile.listSubtitle);
        screens.neuroBoosterDetail().waitForScreen();
        softAssert.assertEquals(screens.neuroBoosterDetail().getPrimaryCtaText(),
                labels.detail.primaryCtaFor("COGNITIVE_HEALTH"), "cognitive-health primary CTA");
        screens.neuroBoosterDetail().tapPrimaryCta();

        screens.neuroBoosterVideo().waitForScreen();
        softAssert.assertTrue(screens.neuroBoosterVideo().hasSubtitleTrack(),
                "video should carry a subtitle track");
        screens.neuroBoosterVideo().fastForwardToEnd();
        screens.neuroBoosterQuiz().waitForIntro();
        softAssert.assertEquals(screens.neuroBoosterQuiz().getIntroHeading(), labels.quiz.introTitle,
                "quiz intro heading");
    }

    /**
     * Walk every quiz question, selecting by TEXT (shuffle-proof). When
     * {@code correct} is true picks each question's correct answer and verifies the
     * "✓" feedback + its explanation (positive path); otherwise picks a wrong answer
     * and verifies the "✗" feedback + that answer's explanation (negative path).
     * Questions are matched to the catalog by prompt, so display order doesn't matter.
     */
    @Step("Answer all quiz questions (correct={correct})")
    private void answerAllVerifying(NeuroBoosterCatalog.Quiz quizData, boolean correct) {
        NeuroBoosterQuizScreen quiz = screens.neuroBoosterQuiz();
        quiz.tapStartQuiz();
        quiz.waitForQuestion();
        int guard = 0;
        while (quiz.isQuestionDisplayed() && guard++ < quizData.questionCount + 2) {
            String prompt = quiz.getQuestionText();
            NeuroBoosterCatalog.Question cq = findQuestion(quizData, prompt);
            NeuroBoosterCatalog.Answer pick = correct ? cq.correct() : cq.firstWrong();

            quiz.selectAnswerByText(pick.text);

            String expectedPrefix = pick.correct ? labels.quiz.answerCorrectPrefix : labels.quiz.answerWrongPrefix;
            softAssert.assertTrue(quiz.getFeedbackTitle().startsWith(expectedPrefix),
                    "feedback prefix for [" + prompt + "] expected '" + expectedPrefix + "', got '"
                            + quiz.getFeedbackTitle() + "'");
            if (pick.explanation != null && !pick.explanation.isEmpty()) {
                softAssert.assertEquals(norm(quiz.getFeedbackExplanation()), norm(pick.explanation),
                        "explanation for [" + prompt + "]");
            }
            quiz.tapContinue();
        }
    }

    /** Match the displayed question prompt to a catalog question (order-independent). */
    private NeuroBoosterCatalog.Question findQuestion(NeuroBoosterCatalog.Quiz quizData, String prompt) {
        String p = norm(prompt);
        for (NeuroBoosterCatalog.Question q : quizData.questions) {
            if (norm(q.prompt).equals(p)) return q;
        }
        // fallback: substring match (handles minor punctuation drift)
        for (NeuroBoosterCatalog.Question q : quizData.questions) {
            if (!p.isEmpty() && (norm(q.prompt).contains(p) || p.contains(norm(q.prompt)))) return q;
        }
        throw new IllegalStateException("Displayed question not in catalog: '" + prompt + "'");
    }

    private static String norm(String s) {
        if (s == null) return "";
        String n = s.replace('’', '\'').replace('‘', '\'')
                    .replace('“', '"').replace('”', '"').replace(' ', ' ');
        return n.replaceAll("\\s+", " ").trim();
    }

    private NeuroBoosterCatalog.Tile smokeOf(String type) {
        NeuroBoosterCatalog.Tile t = CatalogProvider.firstSmokeOfType(catalog, type);
        if (t == null) throw new SkipException("No smoke tile of type " + type + " in the catalog");
        return t;
    }

    private NeuroBoosterCatalog.Tile requireFilledQuiz(String type) {
        for (NeuroBoosterCatalog.Tile t : CatalogProvider.byType(catalog, type)) {
            if (t.quiz != null && t.quiz.isFilled()) return t;
        }
        throw new SkipException("No " + type + " tile has quiz content (correctIndices) filled yet — "
                + "add quiz.questionCount + correctIndices in the catalog to enable pass/fail tests.");
    }

    private int discoveredCount(String progress) {
        Matcher m = PROGRESS.matcher(progress == null ? "" : progress);
        if (m.find()) return Integer.parseInt(m.group(1));
        throw new IllegalStateException("Cannot parse category progress: " + progress);
    }
}
