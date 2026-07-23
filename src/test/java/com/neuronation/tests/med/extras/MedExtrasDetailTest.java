package com.neuronation.tests.med.extras;

import com.neuronation.base.BaseTest;
import com.neuronation.helpers.CatalogProvider;
import com.neuronation.pages.med.extras.NeuroBoosterDetailScreen;
import com.neuronation.testdata.Features;
import com.neuronation.testdata.NeuroBoosterCatalog;
import com.neuronation.testdata.NeuroBoosterLabels;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Per-type NeuroBooster **detail** verification — read-only. Each tile is opened by
 * subtitle, its content asserted, then exited via {@code goBack()} so the discovered
 * count is never mutated. Data-driven over the catalog; one registration covers the
 * whole loop (SMOKE = one tile per type, REGRESSION = all tiles).
 */
@Epic("NeuroNation MED App")
@Feature("Extras / Neurobooster")
public class MedExtrasDetailTest extends BaseTest {

    private static final String CONDITION = "parkinson";
    private static final String FLOW = "flow1_password_morning_skip";

    private NeuroBoosterCatalog catalog;
    private NeuroBoosterLabels labels;

    @BeforeMethod(alwaysRun = true)
    public void navigateToExtras() {
        medFlow.completeFullFlow(FLOW, true);
        screens.dashboard().waitForScreen();
        screens.dashboard().tapExtrasTab();
        screens.extras().waitForScreen();
        catalog = CatalogProvider.load(context.getLanguage(), CONDITION);
        screens.extras().setSectionOrder(
                catalog.categories.stream().map(c -> c.title).collect(java.util.stream.Collectors.toList()));
        labels = CatalogProvider.labels(context.getLanguage());
    }

    @Test(groups = {Features.MED, Features.EXTRAS, Features.CONTENT, Features.SMOKE})
    @Severity(SeverityLevel.CRITICAL)
    @Story("One NeuroBooster of each type opens and shows expected content")
    public void testDetail_smoke() {
        for (NeuroBoosterCatalog.Tile t : CatalogProvider.smokeTiles(catalog)) verifyDetail(t);
        softAssert.assertAll();
    }

    @Test(groups = {Features.MED, Features.EXTRAS, Features.CONTENT, Features.REGRESSION})
    @Severity(SeverityLevel.NORMAL)
    @Story("Every NeuroBooster tile opens and shows expected content")
    public void testDetail_all() {
        List<NeuroBoosterCatalog.Tile> all = CatalogProvider.tiles(catalog, t -> true);
        for (NeuroBoosterCatalog.Tile t : all) verifyDetail(t);
        softAssert.assertAll();
    }

    @Step("Verify NeuroBooster detail (read-only): {tile.category} / {tile.listSubtitle}")
    private void verifyDetail(NeuroBoosterCatalog.Tile tile) {
        screens.extras().openTile(tile.category, tile.listSubtitle);
        NeuroBoosterDetailScreen d = screens.neuroBoosterDetail();
        d.waitForScreen();

        softAssert.assertEquals(d.getHeading(), tile.detailHeading,
                "heading[" + tile.listSubtitle + "]");
        softAssert.assertEquals(d.hasImage(), tile.hasImage,
                "hasImage[" + tile.listSubtitle + "]");
        softAssert.assertEquals(d.getPrimaryCtaText(), labels.detail.primaryCtaFor(tile.type),
                "primaryCta[" + tile.listSubtitle + "] must match type " + tile.type);
        if (tile.detailBodyContains != null && !tile.detailBodyContains.isEmpty()) {
            String body = d.getBodyText();
            for (String needle : tile.detailBodyContains) {
                softAssert.assertTrue(body != null && body.contains(needle),
                        "body[" + tile.listSubtitle + "] should contain '" + needle + "', got '" + body + "'");
            }
        }
        d.goBack();
        screens.extras().waitForScreen();
    }
}
