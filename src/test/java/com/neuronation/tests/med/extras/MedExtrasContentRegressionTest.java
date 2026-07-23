package com.neuronation.tests.med.extras;

import com.neuronation.base.BaseTest;
import com.neuronation.helpers.CatalogProvider;
import com.neuronation.testdata.Features;
import com.neuronation.testdata.NeuroBoosterCatalog;
import com.neuronation.testdata.NeuroBoosterLabels;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Standalone, fast-iteration harness for the Extras (Neurobooster) content regression. Runs the
 * shared {@link ExtrasContentVerifier} per-tile lifecycle (header → initial listing → detail →
 * complete → tick + progress) over the sections assigned to each flow. The authoritative coverage
 * lives merged into the per-flow Extras step of {@code MedFullE2EHappyPathTest}; this class exists
 * to iterate on Extras without paying for a full 30-screen onboarding each time.
 *
 * <ul>
 *   <li>flow1 → the body section (19 tiles)</li>
 *   <li>flow2 → Cognitive Health, Drive and motivation, Mood</li>
 *   <li>flow3 → Dealing with anxieties, Changes in relationships, The effects of medication</li>
 * </ul>
 */
@Epic("NeuroNation MED App")
@Feature("Extras / Neurobooster")
public class MedExtrasContentRegressionTest extends BaseTest {

    private static final String CONDITION = "parkinson";
    private static final String FLOW1 = "flow1_password_morning_skip";
    private static final String FLOW2 = "flow2_password_evening_doctor";
    private static final String FLOW3 = "flow3_declinepasswordless_noon_colorvision";

    private ExtrasContentVerifier verifier;

    @BeforeMethod(alwaysRun = true)
    public void loadCatalog() {
        NeuroBoosterCatalog catalog = CatalogProvider.load(context.getLanguage(), CONDITION);
        NeuroBoosterLabels labels = CatalogProvider.labels(context.getLanguage());
        verifier = new ExtrasContentVerifier(screens, softAssert, catalog, labels, context.getPlatform());
    }

    @Test(groups = {Features.MED, Features.EXTRAS, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Content regression (flow1): body section — every tile lifecycle")
    public void testFlow1_bodyContent() {
        enterExtras(FLOW1);
        verifier.verifySections(List.of("Use mini-exercises for your body"));
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.EXTRAS, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Content regression (flow2): Cognitive Health, Drive, Mood — every tile lifecycle")
    public void testFlow2_cognitiveContent() {
        enterExtras(FLOW2);
        verifier.verifySections(List.of("Cognitive Health", "Drive and motivation", "Mood"));
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.EXTRAS, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Content regression (flow3): Dealing, Changes, Medication — every tile lifecycle")
    public void testFlow3_cognitiveContent() {
        enterExtras(FLOW3);
        verifier.verifySections(List.of("Dealing with anxieties about the future",
                "Changes in relationships and roles", "The effects of medication"));
        softAssert.assertAll();
    }

    // ---------- helpers ----------

    /** Register + onboard via the given flow (Parkinson account), then land on the Extras listing. */
    private void enterExtras(String flow) {
        boolean useLogin = "true".equalsIgnoreCase(System.getProperty("extras.useLogin"));
        if (!useLogin) medFlow.completeFullFlow(flow, true); // true = Parkinson (has all 7 sections)
        screens.dashboard().waitForScreen();
        screens.dashboard().tapExtrasTab();
        screens.extras().waitForScreen();
    }
}
