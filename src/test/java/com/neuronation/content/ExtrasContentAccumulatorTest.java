package com.neuronation.content;

import com.neuronation.pages.med.extras.ExtrasContentAccumulator;
import com.neuronation.pages.med.extras.ExtrasContentAccumulator.Row;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class ExtrasContentAccumulatorTest {

    @Test
    public void dedupesOverlappingFramesWithinCategory() {
        ExtrasContentAccumulator acc = new ExtrasContentAccumulator();
        acc.addFrame(Arrays.asList(
                Row.category("Use mini-exercises for your body"),
                Row.progress("0 / 19 discovered"),
                Row.tile("Breathing Exercise", "For More Cool, Calm, and Collectedness", true),
                Row.tile("Upper Body", "For More Vim and Vigour", true)));
        acc.addFrame(Arrays.asList(
                Row.tile("Upper Body", "For More Vim and Vigour", true),
                Row.tile("Eyes", "For Relaxed Eyes", true)));

        assertEquals(acc.categoryCount(), 1);
        assertEquals(acc.totalTileCount(), 3);
    }

    @Test
    public void tilesAfterHeaderScrollsOffAttachToCurrentCategory() {
        ExtrasContentAccumulator acc = new ExtrasContentAccumulator();
        acc.addFrame(Arrays.asList(
                Row.category("Cognitive Health"),
                Row.progress("0 / 3 discovered"),
                Row.tile("Mood", "Psychoeducational", true)));
        acc.addFrame(Arrays.asList(
                Row.tile("Mood", "Reflection and Marketing", true),
                Row.tile("Mood", "Motivation and Intentions Exercise", true)));

        assertEquals(acc.categoryCount(), 1);
        assertEquals(acc.totalTileCount(), 3);
    }

    @Test
    public void sameTitleDifferentSubtitleAreDistinctTiles() {
        ExtrasContentAccumulator acc = new ExtrasContentAccumulator();
        acc.addFrame(Arrays.asList(
                Row.category("Use mini-exercises for your body"),
                Row.tile("Knowledge", "What cognitive impairments are", true),
                Row.tile("Knowledge", "How to stay fit over the long term", true)));
        assertEquals(acc.totalTileCount(), 2);
    }

    @Test
    public void flatMapUsesDottedKeysAndVerbatimProgress() {
        ExtrasContentAccumulator acc = new ExtrasContentAccumulator();
        acc.addFrame(Arrays.asList(
                Row.category("Use mini-exercises for your body"),
                Row.progress("0 / 19 discovered"),
                Row.tile("Breathing Exercise", "For More Cool, Calm, and Collectedness", true)));
        Map<String, String> m = acc.toFlatMap("Neurobooster", "Mini-exercises to refresh your brain");

        assertEquals(m.get("toolbarTitle"), "Neurobooster");
        assertEquals(m.get("categoryCount"), "1");
        assertEquals(m.get("category[0].title"), "Use mini-exercises for your body");
        assertEquals(m.get("category[0].progress"), "0 / 19 discovered");
        assertEquals(m.get("category[0].tileCount"), "1");
        assertEquals(m.get("category[0].tile[0].title"), "Breathing Exercise");
        assertEquals(m.get("category[0].tile[0].subtitle"), "For More Cool, Calm, and Collectedness");
        assertEquals(m.get("category[0].tile[0].hasImage"), "true");
    }
}
