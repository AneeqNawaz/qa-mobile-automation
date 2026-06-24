package com.neuronation.testdata;

import org.testng.annotations.Test;
import java.util.List;
import static org.testng.Assert.*;

public class ExerciseCatalogTest {

    @Test
    public void allContains23ExercisesInOrder() {
        ExerciseCatalog c = ExerciseCatalog.load();
        assertEquals(c.all().size(), 23, "Catalog should list 23 exercises");
        assertEquals(c.all().get(0), "Memobox");
        assertEquals(c.all().get(22), "Parallel Perfection");
    }

    @Test
    public void lockedSetsPerOption() {
        ExerciseCatalog c = ExerciseCatalog.load();
        assertEquals(c.lockedFor("standard"), List.of());
        assertEquals(c.lockedFor("colorVision"), List.of("Form Fever", "Colour Craze", "Quantum Leap"));
        assertEquals(c.lockedFor("arithmetic"), List.of("Reflector", "Chain Reaction", "Mathrobatics"));
        assertEquals(c.lockedFor("both").size(), 6);
    }

    @Test
    public void expectedCountsDeriveFromLockedSize() {
        ExerciseCatalog c = ExerciseCatalog.load();
        assertEquals(c.expectedAvailable("standard"), 23);
        assertEquals(c.expectedAvailable("colorVision"), 20);
        assertEquals(c.expectedAvailable("arithmetic"), 20);
        assertEquals(c.expectedAvailable("both"), 17);
        assertEquals(c.expectedCountLabel("colorVision"), "20/23");
        assertEquals(c.expectedCountLabel("both"), "17/23");
    }
}
