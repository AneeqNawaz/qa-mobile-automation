package com.neuronation.pages.med.extras;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.AppiumBy;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * NeuroBooster quiz — the intro ({@code GenericBridgeActivity}, "Start quiz") and
 * the question screens ({@code NeuroBoosterQuizActivity}). The result screen is
 * modelled by {@link NeuroBoosterQuizResultScreen}.
 *
 * <p>After an answer is tapped the feedback renders as children of the selected
 * {@code answer_item}: {@code answer_result_title} ("✓ Right answer:" /
 * "✗ Not quite:") + {@code answer_explanation}. The top {@code progressBar} advances
 * one step per answered question (right or wrong) but exposes no value — track the
 * question index in the test instead.
 */
public class NeuroBoosterQuizScreen extends BaseScreen {

    // intro
    static final String ID_INTRO_TITLE  = "nn.mobile.app.med:id/main_toolbar_title";
    static final String ID_INTRO_CONTENT = "nn.mobile.app.med:id/content";
    static final String ID_START_BUTTON = "nn.mobile.app.med:id/cta_button_inner";
    // question
    static final String ID_PROGRESS_BAR  = "nn.mobile.app.med:id/progressBar";
    static final String ID_QUESTION      = "nn.mobile.app.med:id/question";
    static final String ID_ANSWER_ITEM   = "nn.mobile.app.med:id/answer_item";
    static final String ID_ANSWER_TEXT   = "nn.mobile.app.med:id/answer_text";
    static final String ID_RESULT_TITLE  = "nn.mobile.app.med:id/answer_result_title";
    static final String ID_EXPLANATION   = "nn.mobile.app.med:id/answer_explanation";
    static final String ID_CONTINUE      = "nn.mobile.app.med:id/continue_button";

    // iOS: named Buttons for Start/Continue; the question image marks an unanswered question;
    // question + 4 answers are the only StaticTexts (question = topmost). Answers tapped by coordinate.
    static final String IOS_START           = "Start quiz";
    static final String IOS_CONTINUE        = "Understood, continue";
    static final String IOS_Q_IMAGE         = "quiz_question_unanswered";

    // ---------- intro ----------
    /**
     * Wait for the quiz intro after the video. Fast-forward skips to the end via the
     * +15s locator, so the quiz appears promptly; a modest buffer covers the
     * video→quiz transition.
     */
    @Step("Wait for quiz intro")
    public void waitForIntro() {
        By anchor = isIOS() ? AppiumBy.accessibilityId(IOS_START) : AppiumBy.id(ID_START_BUTTON);
        new WebDriverWait(driver, Duration.ofSeconds(60))
                .until(ExpectedConditions.presenceOfElementLocated(anchor));
    }

    @Step("Check quiz intro displayed")
    public boolean isIntroDisplayed() {
        return !driver.findElements(isIOS() ? AppiumBy.accessibilityId(IOS_START) : AppiumBy.id(ID_START_BUTTON)).isEmpty();
    }

    @Step("Get quiz intro heading")
    public String getIntroHeading() {
        if (isIOS()) {
            // The heading is the top-most content StaticText (below the NavigationBar at y≈47),
            // e.g. "Let's do a little quiz!"; the NavigationBar itself is the section name ("Tips").
            WebElement best = null; int bestY = Integer.MAX_VALUE;
            for (WebElement t : driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
                int y; try { y = t.getLocation().getY(); } catch (Exception e) { continue; }
                if (y >= 80 && y < bestY) { best = t; bestY = y; }
            }
            return best == null ? "" : nz(best.getAttribute("name"));
        }
        return getTextByPlatformId(ID_INTRO_TITLE, ID_INTRO_TITLE);
    }

    @Step("Tap 'Start quiz'")
    public void tapStartQuiz() {
        driver.findElement(isIOS() ? AppiumBy.accessibilityId(IOS_START) : AppiumBy.id(ID_START_BUTTON)).click();
    }

    // ---------- questions ----------
    @Step("Wait for quiz question")
    public void waitForQuestion() {
        By anchor = isIOS() ? AppiumBy.accessibilityId(IOS_Q_IMAGE) : AppiumBy.id(ID_QUESTION);
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.presenceOfElementLocated(anchor));
    }

    @Step("Check a quiz question is displayed")
    public boolean isQuestionDisplayed() {
        if (isIOS()) return !driver.findElements(AppiumBy.accessibilityId(IOS_Q_IMAGE)).isEmpty();
        return !driver.findElements(AppiumBy.id(ID_QUESTION)).isEmpty();
    }

    @Step("Get quiz question text")
    public String getQuestionText() {
        if (isIOS()) { WebElement q = questionStaticTextIOS(); return q == null ? "" : nz(q.getAttribute("name")); }
        return getTextByPlatformId(ID_QUESTION, ID_QUESTION);
    }

    @Step("Check quiz progress bar present")
    public boolean hasProgressBar() {
        if (isIOS()) return !driver.findElements(AppiumBy.className("XCUIElementTypeProgressIndicator")).isEmpty();
        return !driver.findElements(AppiumBy.id(ID_PROGRESS_BAR)).isEmpty();
    }

    @Step("Get quiz answer count")
    public int getAnswerCount() {
        if (isIOS()) {
            // Answers are StaticTexts indented at x≈28 (the question sits at x≈16). Before answering
            // there are no ✓/✗ markers; exclude the continue-button label and any marker text.
            int n = 0;
            for (WebElement t : driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
                int x; try { x = t.getLocation().getX(); } catch (Exception e) { continue; }
                String name = nz(t.getAttribute("name"));
                if (x >= 24 && !name.isEmpty() && !name.startsWith("✓") && !name.startsWith("✗")
                        && !name.equals(IOS_CONTINUE)) n++;
            }
            return n;
        }
        return driver.findElements(AppiumBy.id(ID_ANSWER_ITEM)).size();
    }

    @Step("Get quiz answer texts")
    public List<String> getAnswerTexts() {
        List<String> out = new ArrayList<>();
        for (WebElement item : driver.findElements(AppiumBy.id(ID_ANSWER_ITEM))) {
            try { out.add(item.findElement(AppiumBy.id(ID_ANSWER_TEXT)).getText()); }
            catch (Exception e) { out.add(""); }
        }
        return out;
    }

    /** Tap the answer at 0-based index and wait for the feedback to render. */
    @Step("Select quiz answer #{index}")
    public void selectAnswer(int index) {
        List<WebElement> items = driver.findElements(AppiumBy.id(ID_ANSWER_ITEM));
        if (index < 0 || index >= items.size())
            throw new IndexOutOfBoundsException("answer " + index + " of " + items.size());
        items.get(index).click();
        waitForFeedback();
    }

    /**
     * Select the on-screen answer whose text matches {@code answerText}. Order-proof
     * (the app shuffles options, especially on "Try again"), so we match by content.
     */
    @Step("Select quiz answer by text")
    public void selectAnswerByText(String answerText) {
        if (isIOS()) { selectAnswerByTextIOS(answerText); return; }
        String target = norm(answerText);
        List<WebElement> items = driver.findElements(AppiumBy.id(ID_ANSWER_ITEM));
        for (WebElement item : items) {
            String t;
            try { t = item.findElement(AppiumBy.id(ID_ANSWER_TEXT)).getText(); }
            catch (Exception e) { t = ""; }
            if (norm(t).equals(target)) {
                item.click();
                waitForFeedback();
                return;
            }
        }
        throw new org.openqa.selenium.NoSuchElementException(
                "No quiz answer matches text: '" + answerText + "' among " + getAnswerTexts());
    }

    private void waitForFeedback() {
        // After answering, the selected + correct answers get a ✓/✗ marker (answer_result_title).
        // When that marked answer is the LAST/bottom one, its marker + explanation render BELOW the
        // fixed "Understood, continue" button — off-screen, so Android's RecyclerView drops it from
        // the tree and a plain presence wait times out (the flow4 quiz failure). Try the quick wait;
        // if no marker is present yet, scroll the answer list down to bring it into view, then re-wait.
        if (waitForResultTitle(5)) return;
        for (int i = 0; i < 3; i++) {
            scanSwipeUp();               // reveal content below the fold (a bottom answer's feedback)
            if (waitForResultTitle(3)) return;
        }
        // Final attempt — throw if the feedback genuinely never appeared.
        new WebDriverWait(driver, Duration.ofSeconds(4))
                .until(ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_RESULT_TITLE)));
    }

    /** Wait up to {@code sec}s for any answer's ✓/✗ result marker to be present. Never throws. */
    private boolean waitForResultTitle(int sec) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(sec))
                    .until(ExpectedConditions.presenceOfElementLocated(AppiumBy.id(ID_RESULT_TITLE)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- iOS helpers ----------
    /** The question is the top-most StaticText (screen has only question + 4 answer StaticTexts). */
    private WebElement questionStaticTextIOS() {
        WebElement best = null; int bestY = Integer.MAX_VALUE;
        for (WebElement t : driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
            int y = t.getLocation().getY();
            if (y > 90 && y < bestY) { best = t; bestY = y; } // skip the top progress area
        }
        return best;
    }

    /** iOS: answers are StaticTexts (no tappable id) → match by text, tap the centre coordinate. */
    private void selectAnswerByTextIOS(String answerText) {
        String target = norm(answerText);
        for (WebElement t : driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
            String n = t.getAttribute("name");
            if (n != null && norm(n).equals(target)) {
                var r = t.getRect();
                tapAt(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2);
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.presenceOfElementLocated(AppiumBy.accessibilityId(IOS_CONTINUE)));
                revealIosFeedback();  // a bottom answer's ✓/✗ marker can sit below the Continue button
                return;
            }
        }
        throw new org.openqa.selenium.NoSuchElementException(
                "No iOS quiz answer matches text: '" + answerText + "'");
    }

    /** iOS: after answering, scroll the answer list down until a ✓/✗ result marker is on screen. When
     *  the marked (selected/correct) answer is the LAST one, its marker + explanation render below the
     *  fixed "Understood, continue" button and are missed by the feedback read — this brings them in. */
    private void revealIosFeedback() {
        for (int i = 0; i < 4; i++) {
            for (WebElement st : driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
                String n = nz(st.getAttribute("name"));
                if (n.startsWith("✓") || n.startsWith("✗")) return; // a marker is already visible
            }
            scanSwipeUp();
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String norm(String s) {
        if (s == null) return "";
        String n = s.replace('’', '\'').replace('‘', '\'')
                    .replace('“', '"').replace('”', '"').replace(' ', ' ');
        return n.replaceAll("\\s+", " ").trim();
    }

    /** After selecting: true if the feedback title marks the pick correct ("✓ …"). */
    @Step("Check if selected quiz answer was correct")
    public boolean isSelectedCorrect(String correctPrefix) {
        String title = getTextByPlatformId(ID_RESULT_TITLE, ID_RESULT_TITLE);
        return title != null && title.startsWith(correctPrefix);
    }

    @Step("Get answer feedback title")
    public String getFeedbackTitle() { return getTextByPlatformId(ID_RESULT_TITLE, ID_RESULT_TITLE); }

    /** Public so callers can key lookups the same way this class normalises answer text. */
    public static String normText(String s) { return norm(s); }

    /**
     * iOS: parse the answered-question screen in ONE page-source read — {@code findElements} per
     * element is slow on XCUITest, so several lookups per question caused a multi-second pause. Groups
     * StaticTexts by their container parent (answer + its checkmark/cross marker + explanation share a
     * parent). Returns normalised-answer-text -> [marker, explanation]; the key "" -> [questionStatus].
     */
    public java.util.Map<String, String[]> iosFeedbackSnapshot() {
        java.util.Map<String, String[]> out = new java.util.HashMap<>();
        out.put("", new String[]{""});
        try {
            org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new org.xml.sax.InputSource(new java.io.StringReader(driver.getPageSource())));
            org.w3c.dom.NodeList imgs = doc.getElementsByTagName("XCUIElementTypeImage");
            for (int i = 0; i < imgs.getLength(); i++) {
                String n = ((org.w3c.dom.Element) imgs.item(i)).getAttribute("name");
                if (n != null && n.startsWith("quiz_question_")) { out.put("", new String[]{n.substring("quiz_question_".length())}); break; }
            }
            org.w3c.dom.NodeList sts = doc.getElementsByTagName("XCUIElementTypeStaticText");
            java.util.Map<org.w3c.dom.Node, java.util.List<String>> byParent = new java.util.LinkedHashMap<>();
            for (int i = 0; i < sts.getLength(); i++) {
                org.w3c.dom.Element el = (org.w3c.dom.Element) sts.item(i);
                String name = el.getAttribute("name");
                if (name == null || name.isEmpty()) continue;
                byParent.computeIfAbsent(el.getParentNode(), k -> new ArrayList<>()).add(name);
            }
            for (java.util.List<String> g : byParent.values()) {
                String answer = null, marker = "", expl = "";
                for (String s : g) {
                    if (s.startsWith("✓") || s.startsWith("✗")) marker = s; // ✓ / ✗
                    else if (answer == null) answer = s;
                    else if (expl.isEmpty()) expl = s;
                }
                if (answer != null) out.put(norm(answer), new String[]{marker, expl});
            }
        } catch (Exception ignored) { }
        return out;
    }

    /**
     * The result-title (e.g. "✓ Right answer:" / "✗ Not quite:") shown ON the answer whose text
     * matches {@code answerText}. After answering, the app reveals the key on EVERY answer (a ✓ on
     * the correct one, ✗ on the wrongs) — so {@link #getFeedbackTitle()} returns the first (correct)
     * marker regardless of what was picked. To judge the SELECTED answer, read the marker inside its
     * own {@code answer_item}. Android-only (iOS answers have no such child id) → "" on iOS/none.
     */
    @Step("Get result marker on the answer with text: {answerText}")
    public String getResultTitleForAnswer(String answerText) {
        String target = norm(answerText);
        if (isIOS()) {
            // iOS: each answer sits in its OWN container with the answer text, the "✓ Right answer:" /
            // "✗ Not quite:" marker, and the explanation as sibling StaticTexts. Read the marker from
            // that container (only the correct answer + the user's pick are marked → "" for the rest).
            WebElement c = iosAnswerContainer(target);
            if (c != null) {
                for (WebElement st : c.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
                    String n = nz(st.getAttribute("name"));
                    if (n.startsWith("✓") || n.startsWith("✗")) return n;
                }
            }
            return "";
        }
        for (WebElement item : driver.findElements(AppiumBy.id(ID_ANSWER_ITEM))) {
            String t;
            try { t = item.findElement(AppiumBy.id(ID_ANSWER_TEXT)).getText(); } catch (Exception e) { t = ""; }
            if (norm(t).equals(target)) {
                try { return item.findElement(AppiumBy.id(ID_RESULT_TITLE)).getText(); }
                catch (Exception e) { return ""; }
            }
        }
        return "";
    }

    /** iOS: the container element that groups an answer's text + marker + explanation (parent of the
     *  answer StaticText). Robust vs Y-pairing — reads siblings, not screen coordinates. */
    private WebElement iosAnswerContainer(String normAnswer) {
        for (WebElement st : driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
            if (norm(nz(st.getAttribute("name"))).equals(normAnswer)) {
                try { return st.findElement(AppiumBy.xpath("./..")); } catch (Exception e) { return null; }
            }
        }
        return null;
    }

    /**
     * iOS per-question outcome from the status image name: "correct" / "incorrect" (or "unanswered"
     * before answering, "" if none). Id-based — the most robust signal that the pick was right/wrong.
     */
    public String iosQuestionStatus() {
        for (WebElement img : driver.findElements(AppiumBy.className("XCUIElementTypeImage"))) {
            String n = nz(img.getAttribute("name"));
            if (n.startsWith("quiz_question_")) return n.substring("quiz_question_".length());
        }
        return "";
    }

    /** The explanation text revealed ON the answer whose text matches. Android-only → "" on iOS/none. */
    @Step("Get explanation on the answer with text: {answerText}")
    public String getExplanationForAnswer(String answerText) {
        String target = norm(answerText);
        if (isIOS()) {
            // Explanation = the container's StaticText that is neither the answer nor the ✓/✗ marker.
            WebElement c = iosAnswerContainer(target);
            if (c != null) {
                for (WebElement st : c.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
                    String n = nz(st.getAttribute("name"));
                    if (norm(n).equals(target) || n.startsWith("✓") || n.startsWith("✗")) continue;
                    return n;
                }
            }
            return "";
        }
        for (WebElement item : driver.findElements(AppiumBy.id(ID_ANSWER_ITEM))) {
            String t;
            try { t = item.findElement(AppiumBy.id(ID_ANSWER_TEXT)).getText(); } catch (Exception e) { t = ""; }
            if (norm(t).equals(target)) {
                try { return item.findElement(AppiumBy.id(ID_EXPLANATION)).getText(); }
                catch (Exception e) { return ""; }
            }
        }
        return "";
    }

    @Step("Get answer feedback explanation")
    public String getFeedbackExplanation() { return getTextByPlatformId(ID_EXPLANATION, ID_EXPLANATION); }

    @Step("Check answer feedback is shown")
    public boolean isFeedbackShown() {
        if (isIOS()) {
            // Feedback is up once a ✓/✗ marker is revealed (or the question is scored correct/incorrect).
            for (WebElement st : driver.findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
                String n = nz(st.getAttribute("name"));
                if (n.startsWith("✓") || n.startsWith("✗")) return true;
            }
            String status = iosQuestionStatus();
            return status.equals("correct") || status.equals("incorrect");
        }
        return !driver.findElements(AppiumBy.id(ID_RESULT_TITLE)).isEmpty();
    }

    @Step("Tap 'Understood, continue'")
    public void tapContinue() {
        driver.findElement(isIOS() ? AppiumBy.accessibilityId(IOS_CONTINUE) : AppiumBy.id(ID_CONTINUE)).click();
    }

    @Step("Check continue button present")
    public boolean hasContinueButton() {
        return !driver.findElements(isIOS() ? AppiumBy.accessibilityId(IOS_CONTINUE) : AppiumBy.id(ID_CONTINUE)).isEmpty();
    }
}
