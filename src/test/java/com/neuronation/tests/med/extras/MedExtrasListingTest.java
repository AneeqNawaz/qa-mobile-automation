package com.neuronation.tests.med.extras;

import com.neuronation.base.BaseTest;
import com.neuronation.helpers.CatalogProvider;
import com.neuronation.testdata.Features;
import com.neuronation.testdata.NeuroBoosterCatalog;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertTrue;

/**
 * Extras (Neurobooster) listing — read-only. Screen loads, content snapshot, and
 * category structure vs the catalog. Registers a fresh Parkinson's account (the
 * superset condition) and navigates to the Extras tab.
 */
@Epic("NeuroNation MED App")
@Feature("Extras / Neurobooster")
public class MedExtrasListingTest extends BaseTest {

    private static final String CONDITION = "parkinson";
    private static final String FLOW = "flow1_password_morning_skip";

    private NeuroBoosterCatalog catalog;

    @BeforeMethod(alwaysRun = true)
    public void navigateToExtras() {
        medFlow.completeFullFlow(FLOW, true);
        screens.dashboard().waitForScreen();
        screens.dashboard().tapExtrasTab();
        screens.extras().waitForScreen();
        catalog = CatalogProvider.load(context.getLanguage(), CONDITION);
        screens.extras().setSectionOrder(
                catalog.categories.stream().map(c -> c.title).collect(java.util.stream.Collectors.toList()));
    }

    @Test(groups = {Features.MED, Features.EXTRAS, Features.SMOKE, Features.CRITICAL})
    @Severity(SeverityLevel.CRITICAL)
    @Story("Extras (Neurobooster) screen loads")
    public void testExtras_screenLoads() {
        assertTrue(screens.extras().isDisplayed(), "Neurobooster screen should be visible");
    }

    @Test(groups = {Features.MED, Features.EXTRAS, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Extras listing snapshot matches baseline")
    public void testExtras_contentSnapshot() {
        verifyOrRecordContent(screens.extras(), "ExtrasScreenParkinson");
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.EXTRAS, Features.CONTENT, Features.SANITY})
    @Severity(SeverityLevel.NORMAL)
    @Story("Extras categories match the catalog")
    public void testExtras_categoriesMatchCatalog() {
        Map<String, String> captured = screens.extras().captureContent();
        for (int i = 0; i < catalog.categories.size(); i++) {
            NeuroBoosterCatalog.Category c = catalog.categories.get(i);
            softAssert.assertEquals(captured.get("category[" + i + "].title"), c.title,
                    "category[" + i + "].title");
            softAssert.assertEquals(captured.get("category[" + i + "].progress"), c.progress,
                    "category[" + i + "].progress");
        }
        softAssert.assertAll();
    }
}
