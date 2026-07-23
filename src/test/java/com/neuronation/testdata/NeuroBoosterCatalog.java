package com.neuronation.testdata;

import java.util.List;

/**
 * POJO for {@code testdata/med/content/extras/catalog-neurobooster-<condition>.json}.
 *
 * <p>One entry per NeuroBooster tile. Adding a new NB = add one {@link Tile} here
 * (and, for COGNITIVE_HEALTH, its {@link Quiz} content) — no test-code change.
 */
public class NeuroBoosterCatalog {
    public String toolbarTitle;
    public String toolbarSubtitle;
    public List<Category> categories;
    public List<Tile> tiles;

    public static class Category {
        public String title;
        public String progress;    // verbatim, e.g. "0 / 19 discovered"
        public String helpTitle;   // "?" tooltip title (e.g. "Help")
        public String helpMessage; // "?" tooltip body
    }

    public static class Tile {
        public String category;
        public String listTitle;
        public String listSubtitle;
        public String type;              // EXERCISE | KNOWLEDGE | COGNITIVE_HEALTH
        public Integer nbId;             // NeuroBooster id (200..217) for COGNITIVE_HEALTH; null otherwise
        public String detailHeading;
        public List<String> detailBodyContains;
        public String primaryCta;
        public String secondaryCta;
        public boolean hasImage;
        public boolean smoke;
        public Quiz quiz;                // COGNITIVE_HEALTH only; may be null / placeholder
    }

    /**
     * Quiz content for a COGNITIVE_HEALTH tile. Every question lists ALL its answers
     * with a {@code correct} flag and the per-answer {@code explanation}, so tests can
     * verify both the positive path (correct → "✓ Right answer:" + explanation) and
     * the negative path (wrong → "✗ Not quite:" + that answer's explanation).
     *
     * <p>The app shuffles option order (especially on "Try again"), so answers are
     * matched by <b>text</b> at runtime, never by index.
     *
     * <p>Adding a new monthly NB = add a tile with this block to the catalog JSON —
     * no test-code change. Placeholder state = {@code questionCount == 0}: outcome
     * assertions are skipped until content is filled.
     */
    public static class Quiz {
        public int questionCount;
        public List<Question> questions;

        public boolean isFilled() {
            return questionCount > 0 && questions != null && questions.size() == questionCount;
        }
    }

    public static class Question {
        public String prompt;
        public List<Answer> answers;

        public Answer correct() {
            if (answers != null) for (Answer a : answers) if (a.correct) return a;
            throw new IllegalStateException("No correct answer defined for question: " + prompt);
        }

        public Answer firstWrong() {
            if (answers != null) for (Answer a : answers) if (!a.correct) return a;
            throw new IllegalStateException("No wrong answer defined for question: " + prompt);
        }
    }

    public static class Answer {
        public String text;
        public boolean correct;
        public String explanation;
    }
}
