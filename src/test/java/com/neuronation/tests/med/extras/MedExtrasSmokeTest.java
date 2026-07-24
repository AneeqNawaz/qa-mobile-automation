package com.neuronation.tests.med.extras;

import com.neuronation.base.BaseTest;
import com.neuronation.helpers.CatalogProvider;
import com.neuronation.testdata.Features;
import com.neuronation.testdata.NeuroBoosterCatalog;
import com.neuronation.testdata.NeuroBoosterCatalog.Tile;
import com.neuronation.testdata.NeuroBoosterLabels;
import io.qameta.allure.*;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Fast Extras (Neurobooster) smoke — one representative of EVERY case in a SINGLE
 * registered session: EXERCISE completion, KNOWLEDGE completion, VIDEO, quiz PASS,
 * and quiz FAIL. One fresh Parkinson's account keeps it quick (the exhaustive
 * per-tile coverage lives in the E2E content regression / {@link MedExtrasCompletionTest}).
 *
 * <p>All verification logic is delegated to the shared {@link ExtrasContentVerifier};
 * this test just picks the representatives and orders the cases.
 */
@Epic("NeuroNation MED App")
@Feature("Extras / Neurobooster")
public class MedExtrasSmokeTest extends BaseTest {

    private static final String CONDITION = "parkinson";
    private static final String FLOW = "flow1_password_morning_skip";

    private NeuroBoosterCatalog catalog;
    private ExtrasContentVerifier verifier;

    @BeforeMethod(alwaysRun = true)
    public void navigateToExtras() {
        // -Dextras.useLogin=true attaches to an already logged-in account (app relaunches
        // with noReset=true, keeping the session) instead of registering a fresh one.
        boolean useLogin = "true".equalsIgnoreCase(System.getProperty("extras.useLogin"));
        if (!useLogin) medFlow.completeFullFlow(FLOW, true);
        screens.dashboard().waitForScreen();
        screens.dashboard().tapExtrasTab();
        screens.extras().waitForScreen();
        catalog = CatalogProvider.load(context.getLanguage(), CONDITION);
        NeuroBoosterLabels labels = CatalogProvider.labels(context.getLanguage());
        verifier = new ExtrasContentVerifier(screens, softAssert, catalog, labels, context.getPlatform());
    }

    @Test(groups = {Features.MED, Features.EXTRAS, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Smoke: sections+tiles structure, then exercise, knowledge, quiz pass & fail — full verification")
    public void testExtras_smoke_allCases() {
        // Representative tiles: 1 body EXERCISE, 1 body KNOWLEDGE, and 2 cognitive (one PASS, one FAIL).
        Tile exercise  = CatalogProvider.firstSmokeOfType(catalog, "EXERCISE");
        Tile knowledge = CatalogProvider.firstSmokeOfType(catalog, "KNOWLEDGE");
        List<Tile> cognitive = CatalogProvider.tiles(catalog, t -> t.quiz != null && t.quiz.isFilled());
        cognitive.sort(Comparator.comparingInt(t -> t.quiz.questionCount)); // fewest questions first = fastest
        if (cognitive.size() < 2)
            throw new SkipException("Need >= 2 cognitive tiles with filled quiz content");
        Tile passTile = cognitive.get(0);
        Tile failTile = cognitive.get(1);

        // Reliable ordered traversal — the SAME engine as the per-flow content regression: each tile
        // open → verify detail → complete (body = CTA; cognitive = video + quiz pass/fail), then ONE
        // refreshed tick sweep. No per-tile getCategoryProgress or in-session tick read (those flaked
        // — build #81), and NO verifyStructure() full-list scroll-to-bottom (which is what broke the
        // body-section lookup on the walk back up).
        verifier.verifySmokeTiles(List.of(exercise, knowledge, passTile, failTile), Set.of(failTile));

        softAssert.assertAll();
    }
}
