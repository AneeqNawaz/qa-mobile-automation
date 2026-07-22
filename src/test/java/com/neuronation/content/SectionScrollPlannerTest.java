package com.neuronation.content;

import com.neuronation.pages.med.extras.SectionScrollPlanner;
import com.neuronation.pages.med.extras.SectionScrollPlanner.Move;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class SectionScrollPlannerTest {

    private static final List<String> ORDER = Arrays.asList(
            "Use mini-exercises for your body", "Cognitive Health", "Drive and motivation");

    private static SectionScrollPlanner planner() { return new SectionScrollPlanner(ORDER); }

    @Test
    public void nextSectionFollowsOrder_nullOnLast() {
        assertEquals(planner().nextSection("Cognitive Health"), "Drive and motivation");
        assertNull(planner().nextSection("Drive and motivation"));
        assertNull(planner().nextSection("Unknown section"));
    }

    @Test
    public void targetBelowVisible_scrollsDown() {
        SectionScrollPlanner.Search s = planner().newSearch("Drive and motivation");
        assertEquals(s.decide(Collections.singletonList("Use mini-exercises for your body"), false, false),
                Move.SCROLL_DOWN);
    }

    @Test
    public void targetAboveVisible_scrollsUp() {
        SectionScrollPlanner.Search s = planner().newSearch("Use mini-exercises for your body");
        assertEquals(s.decide(Collections.singletonList("Drive and motivation"), false, false),
                Move.SCROLL_UP);
    }

    @Test
    public void wholeWindowVisible_exhausts() {
        SectionScrollPlanner.Search s = planner().newSearch("Cognitive Health");
        // start + end header both on screen, probe already missed -> nothing left to reveal
        assertEquals(s.decide(Arrays.asList("Cognitive Health", "Drive and motivation"), false, false),
                Move.EXHAUSTED);
    }

    @Test
    public void insideWindow_revealsThenExhausts() {
        SectionScrollPlanner.Search s = planner().newSearch("Cognitive Health");
        assertEquals(s.decide(Collections.singletonList("Cognitive Health"), false, false), Move.SCROLL_DOWN);
        assertEquals(s.decide(Collections.singletonList("Drive and motivation"), false, false), Move.EXHAUSTED);
    }

    @Test
    public void lastSection_runsToBottomThenExhausts() {
        SectionScrollPlanner.Search s = planner().newSearch("Drive and motivation");
        assertEquals(s.decide(Collections.singletonList("Drive and motivation"), false, false), Move.SCROLL_DOWN);
        assertEquals(s.decide(Collections.singletonList("Drive and motivation"), false, true), Move.EXHAUSTED);
    }
}
