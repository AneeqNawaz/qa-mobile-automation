package com.neuronation.tests.med;

import com.neuronation.base.BaseTest;
import com.neuronation.config.AppType;
import com.neuronation.driver.DriverManager;
import com.neuronation.testdata.ActivationData;
import com.neuronation.testdata.Features;
import com.neuronation.utils.ScreenDumper;
import com.neuronation.utils.TestDataLoader;
import io.qameta.allure.*;
import org.testng.annotations.Test;

/**
 * Discovery test — walks through the MED app flow and dumps every screen.
 *
 * Run:  mvn test -Dapp.type=med -Dplatform=android -Dtest="MedScreenDiscoveryTest"
 *
 * Activation code: 77AAAAAAAAAAAAAX (reusable, unlimited redemptions)
 * Override in CI: DIGA_ACTIVATION_CODE env var
 */
@Epic("NeuroNation MED App")
@Feature("Screen Discovery")
public class MedScreenDiscoveryTest extends BaseTest {

    @Test(
        description = "Walk through full MED flow and dump all screen elements",
        groups = {Features.MED, "discovery"}
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("Discover all screen locators in MED onboarding flow")
    public void testDumpMedOnboardingFlow() throws InterruptedException {
        ActivationData activation = TestDataLoader.loadActivationData(
                AppType.MED, context.getLanguage(), ActivationData.class);
        log.info("Using activation code: {}", activation.getDigaCode());

        // Screen 1: Launch
        ScreenDumper.dumpCurrentScreen("01_LaunchScreen");
        screens.launch().tapStartNow();
        Thread.sleep(2000);

        // Screen 2: App Selection
        ScreenDumper.dumpCurrentScreen("02_AppSelectionScreen");
        screens.appSelection().selectMedicalApp();
        Thread.sleep(2000);

        // Screen 3: DiGA Activation Code
        ScreenDumper.dumpCurrentScreen("03_DiGACodeScreen");
        screens.digaCode().enterCodeAndActivate(activation.getDigaCode());
        Thread.sleep(5000);

        // Screen 4: Onboarding Video
        ScreenDumper.dumpCurrentScreen("04_OnboardingVideo");
        screens.onboardingVideo().tapClose();
        Thread.sleep(3000);

        // Screen 5: Create Account
        ScreenDumper.dumpCurrentScreen("05_CreateAccountScreen");
        screens.createAccount().tapRegisterViaEmail();
        Thread.sleep(3000);

        // Screen 6: Email registration confirmation dialog
        ScreenDumper.dumpCurrentScreen("06_EmailConfirmDialog");
        // Tap "Continue" on the dialog
        DriverManager.getDriver().findElement(
                io.appium.java_client.AppiumBy.id("android:id/button1")).click();
        Thread.sleep(3000);

        // Screen 7: Email registration form
        ScreenDumper.dumpCurrentScreen("07_EmailRegistrationForm");

        log.info("Discovery complete — check screen-dumps/ folder");
    }
}
