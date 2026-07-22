package com.neuronation.pages.med.extras;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure decision logic for section-bounded scrolling. Holds the top-to-bottom
 * section order (from the catalog JSON) and, per search, decides which way to
 * scroll — or that the section window is exhausted — from the section titles
 * currently visible on screen. No Appium/device dependency: unit-testable.
 */
public final class SectionScrollPlanner {

    public enum Move { SCROLL_DOWN, SCROLL_UP, EXHAUSTED }

    private final List<String> order = new ArrayList<>();

    public SectionScrollPlanner(List<String> sectionTitlesTopToBottom) {
        if (sectionTitlesTopToBottom != null) {
            for (String s : sectionTitlesTopToBottom) order.add(norm(s));
        }
    }

    public boolean hasOrder() { return !order.isEmpty(); }

    /** Index of a section title in the order, or -1 if unknown. */
    public int indexOf(String category) { return order.indexOf(norm(category)); }

    /** The section that follows {@code category}, or null if it is last/unknown. */
    public String nextSection(String category) {
        int i = indexOf(category);
        return (i < 0 || i + 1 >= order.size()) ? null : order.get(i + 1);
    }

    /** Begin a bounded search for {@code targetCategory}. */
    public Search newSearch(String targetCategory) { return new Search(indexOf(targetCategory)); }

    public final class Search {
        private final int targetIdx;
        private boolean seenStart, seenEnd;

        private Search(int targetIdx) { this.targetIdx = targetIdx; }

        /**
         * Decide the next move from the sections currently visible (top-to-bottom).
         * Call AFTER probing the frame for a hit; only reached when the probe missed.
         */
        public Move decide(List<String> visibleHeaders, boolean atTop, boolean atBottom) {
            int minIdx = Integer.MAX_VALUE, maxIdx = -1;
            boolean startVisible = false, endVisible = false;
            int endIdx = targetIdx + 1;
            boolean hasEnd = endIdx >= 0 && endIdx < order.size();
            for (String h : visibleHeaders) {
                int idx = order.indexOf(norm(h));
                if (idx < 0) continue;
                minIdx = Math.min(minIdx, idx);
                maxIdx = Math.max(maxIdx, idx);
                if (idx == targetIdx) startVisible = true;
                if (hasEnd && idx == endIdx) endVisible = true;
            }
            if (startVisible) seenStart = true;
            if (endVisible) seenEnd = true;

            if (!seenStart) {
                if (maxIdx >= 0 && targetIdx > maxIdx) return Move.SCROLL_DOWN;         // target below all visible
                if (minIdx != Integer.MAX_VALUE && targetIdx < minIdx) return Move.SCROLL_UP; // target above all visible
                if (atTop) return Move.SCROLL_DOWN;
                if (atBottom) return Move.SCROLL_UP;
                return Move.SCROLL_DOWN;                                                // default: probe downward
            }
            // start seen — sweep the window toward the end anchor
            if (hasEnd) {
                return seenEnd ? Move.EXHAUSTED   // whole window observed, probe already failed
                               : Move.SCROLL_DOWN; // reveal the rest of the section
            }
            // target is the LAST section: window runs to the bottom
            return atBottom ? Move.EXHAUSTED : Move.SCROLL_DOWN;
        }
    }

    static String norm(String s) {
        if (s == null) return "";
        return s.replace("‘", "'").replace("’", "'")
                .replace("“", "\"").replace("”", "\"")
                .replace(" ", " ").replace(" ", " ")
                .replaceAll("\\s+", " ").trim();
    }
}
