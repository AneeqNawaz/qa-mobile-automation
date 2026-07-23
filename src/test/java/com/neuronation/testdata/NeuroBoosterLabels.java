package com.neuronation.testdata;

/**
 * POJO for {@code testdata/med/content/extras/neurobooster-labels.json} — every
 * standard NeuroBooster UI string. Tests reference these so there are **no string
 * literals in test code**; a copy change is a one-line data edit.
 */
public class NeuroBoosterLabels {
    public String toolbarTitle;
    public String discoveredWord;
    public String discoveredFormat;   // e.g. "%d / %d discovered"
    public Detail detail;
    public Quiz quiz;
    public Result result;

    public static class Detail {
        public String primaryCtaExercise;
        public String primaryCtaKnowledge;
        public String primaryCtaCognitiveHealth;
        public String secondaryCta;

        /** Expected primary-CTA label for a tile type. */
        public String primaryCtaFor(String type) {
            switch (type) {
                case "EXERCISE":         return primaryCtaExercise;
                case "KNOWLEDGE":        return primaryCtaKnowledge;
                case "COGNITIVE_HEALTH": return primaryCtaCognitiveHealth;
                default: throw new IllegalArgumentException("Unknown NB type: " + type);
            }
        }
    }

    public static class Quiz {
        public String introTitle;
        public String startButton;
        public String answerContinueButton;
        public String answerCorrectPrefix;   // "✓"
        public String answerWrongPrefix;     // "✗"
    }

    public static class Result {
        public String titlePass;
        public String titleFail;
        public String actionPass;   // "Continue"
        public String actionFail;   // "Try again"
    }
}
