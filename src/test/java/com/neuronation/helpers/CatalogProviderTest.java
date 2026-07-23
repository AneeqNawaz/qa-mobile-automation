package com.neuronation.helpers;

import com.neuronation.testdata.NeuroBoosterCatalog;
import com.neuronation.testdata.NeuroBoosterLabels;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Unit tests for CatalogProvider — no device, run in normal `mvn test`. */
public class CatalogProviderTest {

    private static final String JSON = "{"
            + "\"toolbarTitle\":\"Neurobooster\",\"toolbarSubtitle\":\"sub\","
            + "\"categories\":[{\"title\":\"Use mini-exercises for your body\",\"progress\":\"0 / 19 discovered\"}],"
            + "\"tiles\":["
            + "{\"category\":\"Use mini-exercises for your body\",\"listTitle\":\"Breathing Exercise\","
            + "\"listSubtitle\":\"For More Cool, Calm, and Collectedness\",\"type\":\"EXERCISE\","
            + "\"detailHeading\":\"For More Cool, Calm, and Collectedness\",\"detailBodyContains\":[\"breath\"],"
            + "\"primaryCta\":\"done\",\"secondaryCta\":\"later\",\"hasImage\":true,\"smoke\":true},"
            + "{\"category\":\"Cognitive Health\",\"listTitle\":\"Mood\",\"listSubtitle\":\"Psychoeducational\","
            + "\"type\":\"COGNITIVE_HEALTH\",\"nbId\":206,\"detailHeading\":\"Mood\",\"detailBodyContains\":[],"
            + "\"primaryCta\":\"video\",\"secondaryCta\":\"later\",\"hasImage\":true,\"smoke\":false,"
            + "\"quiz\":{\"questionCount\":1,\"questions\":[{\"prompt\":\"Q?\",\"answers\":["
            + "{\"text\":\"right\",\"correct\":true,\"explanation\":\"because\"},"
            + "{\"text\":\"wrong\",\"correct\":false,\"explanation\":\"nope\"}]}]}},"
            + "{\"category\":\"Cognitive Health\",\"listTitle\":\"Mood\",\"listSubtitle\":\"Reflection\","
            + "\"type\":\"COGNITIVE_HEALTH\",\"nbId\":207,\"detailHeading\":\"Mood\",\"detailBodyContains\":[],"
            + "\"primaryCta\":\"video\",\"secondaryCta\":\"later\",\"hasImage\":true,\"smoke\":false,"
            + "\"quiz\":{\"questionCount\":0,\"questions\":[]}}"
            + "]}";

    @Test
    public void parsesCategoriesAndTiles() {
        NeuroBoosterCatalog c = CatalogProvider.parse(JSON);
        assertEquals(c.toolbarTitle, "Neurobooster");
        assertEquals(c.categories.size(), 1);
        assertEquals(c.tiles.size(), 3);
        assertEquals(c.tiles.get(0).type, "EXERCISE");
    }

    @Test
    public void smokeAndTypeFilters() {
        NeuroBoosterCatalog c = CatalogProvider.parse(JSON);
        assertEquals(CatalogProvider.smokeTiles(c).size(), 1);
        assertEquals(CatalogProvider.byType(c, "COGNITIVE_HEALTH").size(), 2);
        assertNotNull(CatalogProvider.firstSmokeOfType(c, "EXERCISE"));
        assertNull(CatalogProvider.firstSmokeOfType(c, "KNOWLEDGE"));
    }

    @Test
    public void quizFilledDetectionAndAnswerAccessors() {
        NeuroBoosterCatalog c = CatalogProvider.parse(JSON);
        NeuroBoosterCatalog.Tile filled = CatalogProvider.byType(c, "COGNITIVE_HEALTH").get(0);
        NeuroBoosterCatalog.Tile placeholder = CatalogProvider.byType(c, "COGNITIVE_HEALTH").get(1);
        assertTrue(filled.quiz.isFilled());
        assertFalse(placeholder.quiz.isFilled());
        NeuroBoosterCatalog.Question q = filled.quiz.questions.get(0);
        assertEquals(q.correct().text, "right");
        assertEquals(q.correct().explanation, "because");
        assertEquals(q.firstWrong().text, "wrong");
        assertEquals(q.firstWrong().explanation, "nope");
    }

    @Test
    public void loadsRealParkinsonCatalogAndLabelsFromResources() {
        NeuroBoosterCatalog c = CatalogProvider.load("en", "parkinson");
        assertNotNull(c.tiles);
        assertTrue(c.tiles.size() >= 19, "expected the full Parkinson's catalog");
        assertTrue(CatalogProvider.byType(c, "EXERCISE").size() > 0);
        assertTrue(CatalogProvider.byType(c, "KNOWLEDGE").size() > 0);
        assertTrue(CatalogProvider.byType(c, "COGNITIVE_HEALTH").size() > 0);

        NeuroBoosterLabels labels = CatalogProvider.labels("en");
        assertEquals(labels.detail.primaryCtaFor("COGNITIVE_HEALTH"), "Continue to video and quiz");
        assertEquals(labels.result.actionPass, "Continue");
        assertEquals(labels.result.actionFail, "Try again");
    }
}
