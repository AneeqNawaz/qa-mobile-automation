package com.neuronation.base;

import com.neuronation.listeners.AllureScreenshotListener;
import org.testng.asserts.IAssert;
import org.testng.asserts.SoftAssert;

/**
 * SoftAssert that captures a screenshot the moment each soft assertion fails.
 *
 * Plain SoftAssert only throws at assertAll(), so the test-failure screenshot (taken by the
 * listener) shows the end-of-test screen, not the failing step. TestNG calls onAssertFailure(...)
 * inline as soon as a soft assert fails — so capturing here gives a screenshot of the ACTUAL
 * failing step. This also covers the step() wrapper's navigation/read failures, which it records
 * via softAssert.fail(...).
 */
public class ScreenshotSoftAssert extends SoftAssert {

    @Override
    public void onAssertFailure(IAssert<?> assertCommand, AssertionError ex) {
        try {
            String label = assertCommand != null ? assertCommand.getMessage() : ex.getMessage();
            AllureScreenshotListener.captureScreenshot("FAIL @ step: " + label);
        } catch (Exception ignored) {
            // Screenshot is best-effort — never mask the assertion failure itself.
        }
        super.onAssertFailure(assertCommand, ex);
    }
}
