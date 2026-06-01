# NeuroNation Mobile Automation Framework — Developer Guide

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Package Reference](#package-reference)
4. [Setup & Prerequisites](#setup--prerequisites)
5. [First Run — Step by Step](#first-run--step-by-step)
6. [Running Tests](#running-tests)
7. [Content Snapshot System](#content-snapshot-system)
8. [Replacing Placeholder Locators](#replacing-placeholder-locators)
9. [Adding New Tests](#adding-new-tests)
10. [Allure Reports](#allure-reports)
11. [Troubleshooting](#troubleshooting)

---

## Project Overview

E2E mobile test framework for two NeuroNation apps:

| App | Package | Platforms | Languages |
|-----|---------|-----------|-----------|
| **MED** (Medical/DiGA) | `nn.mobile.app.med` | Android, iOS | EN, DE |
| **BT** (Brain Training) | `nn.mobile.app.bt` | Android, iOS | EN, DE |

**Stack**: Java 17, Maven, Appium 10.1, TestNG 7.10, RestAssured 5.4, Allure 2.25

**Pattern**: Page Object Model with Playwright-style fixtures (lazy-initialized screens in `BaseTest.screens`)

---

## Architecture

```
Test Class (e.g. MedRegistrationE2ETest)
    │
    ├── extends BaseTest
    │       ├── screens.*        ← lazy page objects (Screens.java)
    │       ├── context          ← per-test state (TestContext)
    │       ├── registrationApi  ← API services
    │       ├── statsApi
    │       ├── softAssert       ← content mismatch assertions
    │       └── setUp()/tearDown() manages driver lifecycle
    │
    ├── uses screens.launch().tapStartNow()
    │       └── BaseScreen
    │             ├── tap(), type(), swipe(), getText()
    │             ├── captureContent() → Map<String,String>
    │             └── PageFactory + AppiumFieldDecorator
    │
    └── uses registrationApi.registerFullUser(...)
            └── ApiClient → RestAssured → NeuroNation API
```

---

## Package Reference

### `src/main/java/com/neuronation/`

| Package | Class | What It Does |
|---------|-------|-------------|
| `config/` | `AppType.java` | Enum: `MED`, `BT` — holds package name and main activity per app |
| | `Platform.java` | Enum: `ANDROID`, `IOS` |
| | `ConfigManager.java` | Singleton. Loads `config.properties` (base) then overlays `config-{app}-{platform}.properties`. Resolves `-D` system properties and `${ENV_VAR}` references. Getters: `getString()`, `getInt()`, `getBoolean()`, `getAppType()`, `getPlatform()`. Call `ConfigManager.reset()` to force reload. |
| `driver/` | `DriverManager.java` | `ThreadLocal<AppiumDriver>` holder. `getDriver()`, `setDriver()`, `quitDriver()`. Each parallel thread gets its own isolated driver. |
| | `DriverFactory.java` | Creates `AndroidDriver` or `IOSDriver` based on config. Reads platform, device name, app path, UDID from config. When `browserstack.enabled=true`, connects to BrowserStack remote hub instead of local Appium. |
| `api/` | `ApiClient.java` | Shared RestAssured request builder. `baseRequest()` sets base URL + JSON headers. `authenticatedRequest(token)` adds Bearer auth. |
| | `RegistrationApiService.java` | DiGA 7-step registration: `createAccount()` → `requestEmailVerification()` → `confirmEmailVerification()` → `setProfile()` → `acceptTerms()` → `setDiGACode()` → `completeOnboarding()`. Shortcut: `registerFullUser(email, pass, name)` does steps 1-4. All methods have `@Step` for Allure. |
| | `StatsApiService.java` | `getOnboardingVariantId(token, userId)` — calls `GET /api/v2/users/{id}/stats`, filters for `statsType=4, statsId=0, statsComponent=0`, returns `valueInt` (the A/B variant). |
| `api/model/` | `StatsEntry.java` | POJO: `statsType`, `statsId`, `statsComponent`, `valueInt`, `valueText`, `lastEdit` |
| | `RegistrationResponse.java` | POJO: `accessToken`, `userId` |
| `utils/` | `WaitUtils.java` | Static helpers: `waitForVisible(locator, timeout)`, `waitForClickable()`, `waitForInvisible()`, `waitForPresence()`. Wraps `WebDriverWait`. |
| | `TestDataLoader.java` | Loads JSON from `testdata/{app}-{lang}.json`. `loadRegistrationData(AppType, lang, Class)` returns deserialized POJO. `loadOnboardingVariants()` returns variant list. |

### `src/test/java/com/neuronation/`

| Package | Class | What It Does |
|---------|-------|-------------|
| `base/` | `BaseTest.java` | **Test lifecycle.** `@BeforeMethod`: creates driver, initializes `screens`, `context`, `registrationApi`, `statsApi`, `softAssert`, snapshot manager. `@AfterMethod`: quits driver, resets screens, removes context. Has `verifyOrRecordContent(screen, name)` helper. |
| | `BaseScreen.java` | **Abstract POM base.** Constructor calls `PageFactory.initElements()`. Actions: `tap(element)`, `type(element, text)`, `getText()`, `getTextSafe()`, `isDisplayed()`, `swipeUp()`, `swipeDown()`. Abstract `captureContent()` returns `Map<String,String>` of screen text for snapshots. |
| | `Screens.java` | **Playwright-style fixture container.** Lazy-initialized accessors for all 26 screens: `screens.launch()`, `screens.medWelcome()`, `screens.btDashboard()`, etc. `reset()` clears cache between tests. |
| `context/` | `TestContext.java` | `ThreadLocal` per-test state: `authToken`, `userId`, `variantId`, `language`, `email`, `password`, `appType`, `platform`. Set in `BaseTest.setUp()`, cleared in `tearDown()`. |
| `content/` | `ContentSnapshot.java` | POJO: `screenName` + `Map<String,String>` of element text |
| | `SnapshotManager.java` | `record()` saves JSON to `snapshots/{app}/{platform}/{lang}/variant_{id}/{Screen}.json`. `load()` reads it back. `exists()` checks if baseline exists. |
| | `ContentVerifier.java` | Compares two snapshots. Returns list of mismatches (NEW keys, MISSING keys, changed values). Logs to Allure as attachment. Does NOT fail the test (soft). |
| `screens/common/` | `LaunchScreen` | App entry point. `tapStartNow()`, `tapLogin()`, `isStartNowDisplayed()` |
| | `LoginScreen` | `login(email, password)`, `isErrorDisplayed()`, `getErrorText()` |
| | `DashboardScreen` | `isDashboardDisplayed()`, `tapStartTraining()`, `tapProfile()` |
| | `ProfileScreen` | `getName()`, `getEmail()`, `tapLogout()`, `tapSettings()` |
| `screens/med/` | 17 screens | Full MED onboarding: Welcome → Age → Goal → Focus → Frequency → Reminder → GameIntro (skip logic) → GamePlay → GameResult → Personalization → PlanReady → Registration → Verification → DiGACode → OnboardingSummary → Paywall → Subscription |
| `screens/bt/` | 5 screens | BT flow: Welcome → Onboarding → Registration → Dashboard → Paywall |
| `tests/med/` | `MedRegistrationE2ETest` | Full MED E2E with DiGA code. Groups: `med`, `registration`, `smoke`, `critical` |
| | `MedOnboardingFlowTest` | Onboarding navigation + game skip + variant resolution. Groups: `med`, `onboarding` |
| `tests/bt/` | `BtRegistrationE2ETest` | Full BT E2E + paywall dismissal. Groups: `bt`, `registration`, `smoke` |
| `tests/common/` | `LoginFlowTest` | Valid login, invalid login, empty fields. Groups: `login`, `smoke`, `edge-cases` |
| | `EdgeCaseTests` | Back navigation, profile navigation, cold start. Groups: `edge-cases`, `dashboard`, `profile`, `smoke` |
| `listeners/` | `AllureScreenshotListener` | `ITestListener`. On test failure/skip: captures screenshot and attaches to Allure report as PNG. |
| `testdata/` | `Features.java` | TestNG group name constants: `MED`, `BT`, `REGISTRATION`, `LOGIN`, `ONBOARDING`, `SMOKE`, `REGRESSION`, `CRITICAL`, etc. |
| | `RegistrationData.java` | POJO: `emailPrefix`, `emailDomain`, `password`, `name`. `generateEmail()` appends timestamp for unique emails per run. |
| | `OnboardingVariant.java` | POJO: `variantId`, `screenSequence` list |

### `src/test/resources/`

| Path | What It Does |
|------|-------------|
| `config.properties` | Base defaults: Appium URL, timeouts, snapshot mode, API base URL, BrowserStack toggle |
| `config-med-android.properties` | MED Android overlay: package, activity, device, UDID, app path |
| `config-med-ios.properties` | MED iOS overlay: bundle ID, device name, platform version |
| `config-bt-android.properties` | BT Android overlay |
| `config-bt-ios.properties` | BT iOS overlay |
| `testdata/med-en.json` | MED English registration data + onboarding variants |
| `testdata/med-de.json` | MED German data |
| `testdata/bt-en.json` | BT English data |
| `testdata/bt-de.json` | BT German data |
| `snapshots/` | Auto-generated. JSON baselines per `{app}/{platform}/{lang}/variant_{id}/` |
| `suites/` | TestNG XML suites (see [Running Tests](#running-tests)) |

---

## Setup & Prerequisites

### 1. Install Java 17

```bash
# macOS
brew install openjdk@17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Verify
java -version   # Should show 17.x
mvn -version    # Should show Maven 3.x with Java 17
```

### 2. Install Appium 2

```bash
npm install -g appium

# Install drivers
appium driver install uiautomator2    # Android
appium driver install xcuitest         # iOS

# Verify
appium --version    # Should show 2.x
```

### 3. Android Setup

```bash
# Install Android SDK (via Android Studio or command line)
# Set environment variables:
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools

# Create an emulator (or use Android Studio AVD Manager)
avdmanager create avd -n "Pixel_7" -k "system-images;android-34;google_apis;arm64-v8a"

# Start the emulator
emulator -avd Pixel_7 &

# Verify device is connected
adb devices
# Should show: emulator-5554    device
```

### 4. iOS Setup (macOS only)

```bash
# Xcode must be installed from App Store
xcode-select --install

# Open a simulator
open -a Simulator

# Verify
xcrun simctl list devices | grep Booted
```

### 5. Install the App on Emulator/Simulator

```bash
# Android — install APK
adb install apps/neuronation-med.apk

# iOS — install on simulator
xcrun simctl install booted apps/neuronation-med.app
```

### 6. Compile the Framework

```bash
mvn clean compile test-compile
# Should print: BUILD SUCCESS
```

---

## First Run — Step by Step

### Step 1: Start Appium Server

```bash
# Terminal 1 — start Appium
appium --port 4723

# You should see: Appium REST http interface listener started on 0.0.0.0:4723
```

### Step 2: Start Emulator + Install App

```bash
# Terminal 2 — start Android emulator
emulator -avd Pixel_7 &

# Wait for it to boot, then install the APK
adb wait-for-device
adb install apps/neuronation-med.apk
```

### Step 3: Update Config (if needed)

Edit `src/test/resources/config-med-android.properties`:

```properties
# Match your emulator
device.name=Pixel_7
device.udid=emulator-5554

# If the APK is already installed, use package/activity instead of app.path:
app.package=nn.mobile.app.med
app.activity=com.neuronation.med.MainActivity
# app.path=apps/neuronation-med.apk    ← comment out if using package
```

### Step 4: Run a Single Smoke Test to Verify

```bash
# Run ONLY the cold start test (simplest — just checks app launches)
mvn test \
  -DsuiteFile=src/test/resources/suites/med-android-en.xml \
  -Dapp.type=med \
  -Dplatform=android \
  -Dgroups=smoke \
  -Dtest="EdgeCaseTests#testAppColdStart"
```

**What to expect:**
- Appium opens the MED app on the emulator
- Test checks if the launch screen appears
- Test passes or fails (if locators don't match yet — that's expected)
- Driver quits, app closes

### Step 5: Inspect Elements with Appium Inspector

The placeholder locators won't work until you replace them with real IDs. Use Appium Inspector to find them:

1. Download [Appium Inspector](https://github.com/appium/appium-inspector/releases)
2. Connect to your running Appium server:
   - Remote Host: `127.0.0.1`
   - Remote Port: `4723`
   - Remote Path: `/`
3. Set desired capabilities:
   ```json
   {
     "platformName": "Android",
     "appium:automationName": "UiAutomator2",
     "appium:deviceName": "Pixel_7",
     "appium:appPackage": "nn.mobile.app.med",
     "appium:appActivity": "com.neuronation.med.MainActivity"
   }
   ```
4. Click **Start Session**
5. Navigate through the app, click on elements to see their:
   - `resource-id` → use for `@AndroidFindBy(id = "...")`
   - `accessibility-id` → use for `@iOSXCUITFindBy(accessibility = "...")`
   - `content-desc` → alternative for accessibility

### Step 6: Replace Locators in Screen Files

Example — update `LaunchScreen.java`:

```java
// BEFORE (placeholder)
@AndroidFindBy(id = "nn.mobile.app.med:id/launch_title")
@iOSXCUITFindBy(accessibility = "launch_title")
private WebElement titleText;

// AFTER (real locator from Appium Inspector)
@AndroidFindBy(id = "nn.mobile.app.med:id/tv_welcome_title")
@iOSXCUITFindBy(accessibility = "Welcome to NeuroNation")
private WebElement titleText;
```

### Step 7: Run Again After Locator Fix

```bash
mvn test \
  -DsuiteFile=src/test/resources/suites/med-android-en.xml \
  -Dapp.type=med \
  -Dplatform=android \
  -Dtest="EdgeCaseTests#testAppColdStart"
```

Now it should PASS with real locators.

### Step 8: Record Content Snapshots

Once locators work, record baselines for all screens:

```bash
mvn test \
  -Dsnapshot.mode=record \
  -DsuiteFile=src/test/resources/suites/med-android-en.xml \
  -Dapp.type=med \
  -Dplatform=android
```

This creates JSON files under:
```
src/test/resources/snapshots/med/android/en/variant_0/
  ├── LaunchScreen.json
  ├── MedWelcomeScreen.json
  ├── AgeSelectionScreen.json
  └── ...
```

### Step 9: Verify Snapshots on Next Run

```bash
# Default mode is verify — just run normally
mvn test \
  -DsuiteFile=src/test/resources/suites/med-android-en.xml \
  -Dapp.type=med \
  -Dplatform=android
```

If any screen text changed since recording, mismatches show in the Allure report (but tests don't fail — they're soft assertions).

### Step 10: View the Allure Report

```bash
mvn allure:serve
```

Opens a browser with:
- **Suites** tab: test results grouped by class
- **Features** tab: grouped by `@Feature` annotation (Registration, Login, Onboarding, Edge Cases)
- **Stories** tab: grouped by `@Story`
- Each test: description, severity, steps, screenshots on failure, content mismatch attachments

---

## Running Tests

### By App + Platform + Language (Suite Files)

```bash
# MED on Android in English
mvn test -DsuiteFile=src/test/resources/suites/med-android-en.xml -Dapp.type=med -Dplatform=android

# BT on iOS in German
mvn test -DsuiteFile=src/test/resources/suites/bt-ios-de.xml -Dapp.type=bt -Dplatform=ios
```

Available suites: `med-android-en`, `med-android-de`, `med-ios-en`, `med-ios-de`, `bt-android-en`, `bt-android-de`, `bt-ios-en`, `bt-ios-de`

### By Feature

```bash
mvn test -DsuiteFile=src/test/resources/suites/feature-registration.xml
mvn test -DsuiteFile=src/test/resources/suites/feature-login.xml
mvn test -DsuiteFile=src/test/resources/suites/feature-onboarding.xml
```

### By Priority

```bash
mvn test -DsuiteFile=src/test/resources/suites/smoke.xml        # Critical path only
mvn test -DsuiteFile=src/test/resources/suites/full-regression.xml  # Everything (4 threads)
```

### By TestNG Group (no suite file)

```bash
mvn test -Dgroups=smoke                    # All smoke tests
mvn test -Dgroups=registration             # All registration tests
mvn test -Dgroups="med,registration"       # MED registration only
mvn test -Dgroups=edge-cases               # Edge cases only
mvn test -Dgroups=login                    # Login tests only
```

Available groups: `med`, `bt`, `registration`, `login`, `onboarding`, `dashboard`, `profile`, `paywall`, `game`, `edge-cases`, `smoke`, `regression`, `critical`

### Single Test Method

```bash
mvn test -Dtest="LoginFlowTest#testLoginWithValidCredentials"
mvn test -Dtest="MedRegistrationE2ETest#testMedFullRegistrationE2E"
```

### With Snapshot Recording

```bash
mvn test -Dsnapshot.mode=record -DsuiteFile=src/test/resources/suites/med-android-en.xml
```

### With Game Skipping (dev mode)

```bash
mvn test -Dskip.games=true -DsuiteFile=src/test/resources/suites/med-android-en.xml
```

---

## Content Snapshot System

### How It Works

```
RECORD mode (first run):
  Screen → captureContent() → {"title": "Fitness...", "button": "Start"}
    → saved to snapshots/med/android/en/variant_0/LaunchScreen.json

VERIFY mode (subsequent runs):
  Screen → captureContent() → current map
  Load baseline JSON → baseline map
  Compare → mismatches logged as soft assertions + Allure attachment
  Test continues (functional assertions still run)
```

### File Structure

```
src/test/resources/snapshots/
├── med/
│   ├── android/
│   │   ├── en/
│   │   │   └── variant_0/
│   │   │       ├── LaunchScreen.json
│   │   │       ├── LoginScreen.json
│   │   │       └── MedWelcomeScreen.json
│   │   └── de/
│   │       └── variant_0/
│   │           └── LaunchScreen.json
│   └── ios/
│       └── en/
│           └── variant_0/
│               └── ...
└── bt/
    └── android/
        └── en/
            └── variant_0/
                └── ...
```

### Re-recording After Content Change

When the app text intentionally changes:

```bash
# Re-record specific suite
mvn test -Dsnapshot.mode=record -DsuiteFile=src/test/resources/suites/med-android-en.xml

# Re-record German baselines
mvn test -Dsnapshot.mode=record -DsuiteFile=src/test/resources/suites/med-android-de.xml -Dlanguage=de
```

---

## Replacing Placeholder Locators

Every screen file has placeholder locators marked with inline comments. Workflow:

1. Open Appium Inspector with the app running
2. Navigate to the target screen
3. Click elements to find `resource-id` (Android) or `accessibility-id` (iOS)
4. Update the screen file:

```java
// Find-and-replace pattern in each screen:
@AndroidFindBy(id = "nn.mobile.app.med:id/PLACEHOLDER")   // ← replace PLACEHOLDER
@iOSXCUITFindBy(accessibility = "PLACEHOLDER")             // ← replace PLACEHOLDER
```

5. Recompile: `mvn test-compile`
6. Run the smoke test to verify

**Tip**: Do one screen at a time. Start with `LaunchScreen` since every test begins there.

---

## Adding New Tests

### 1. Create the test class

```java
package com.neuronation.tests.med;

import com.neuronation.base.BaseTest;
import com.neuronation.testdata.Features;
import io.qameta.allure.*;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Epic("NeuroNation MED App")
@Feature("My New Feature")           // Allure report grouping
public class MyNewFeatureTest extends BaseTest {

    @Test(
        description = "Short description for test list",
        groups = {Features.MED, Features.REGRESSION}   // TestNG groups
    )
    @Severity(SeverityLevel.NORMAL)
    @Story("User story this test covers")
    @Description("Detailed description shown in Allure report. "
            + "Explain preconditions, steps, and expected outcome.")
    public void testMyFeature() {
        // All screens available via fixture:
        screens.launch().tapStartNow();
        screens.medWelcome().tapGetStarted();

        // Content verification (optional):
        verifyOrRecordContent(screens.ageSelection(), "AgeSelectionScreen");

        // Functional assertions:
        assertTrue(screens.dashboard().isDashboardDisplayed());

        // Collect content soft assertions at the end:
        softAssert.assertAll();
    }
}
```

### 2. Add to a suite XML

```xml
<class name="com.neuronation.tests.med.MyNewFeatureTest"/>
```

### 3. Add a new screen (if needed)

```java
package com.neuronation.screens.med;

import com.neuronation.base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;
import java.util.*;

public class MyNewScreen extends BaseScreen {

    @AndroidFindBy(id = "nn.mobile.app.med:id/my_element")
    @iOSXCUITFindBy(accessibility = "my_element")
    private WebElement myElement;

    @Step("Tap my element on MyNew screen")
    public void tapMyElement() {
        log.info("Tapping my element");
        tap(myElement);
    }

    @Override
    public Map<String, String> captureContent() {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("myElement", getTextSafe(myElement));
        return content;
    }
}
```

Then add to `Screens.java`:
```java
private MyNewScreen myNew;
public MyNewScreen myNew() {
    if (myNew == null) myNew = new MyNewScreen();
    return myNew;
}
```

---

## Allure Reports

### What You See

Every test in the Allure report includes:

| Annotation | Where It Shows | Example |
|-----------|---------------|---------|
| `@Epic` | Top-level grouping | "NeuroNation MED App" |
| `@Feature` | Feature tab grouping | "Registration", "Login" |
| `@Story` | Story tab grouping | "New user completes registration" |
| `@Description` | Test detail page | Full paragraph explaining the test |
| `@Severity` | Severity badge | BLOCKER, CRITICAL, NORMAL |
| `@Step` | Step timeline in test | "Tap Start Now button", "API: Create account" |

On failure: screenshot is auto-attached. Content mismatches: attached as text file.

### Generate Report

```bash
# Interactive (opens browser)
mvn allure:serve

# Generate static HTML
mvn allure:report
# Output: target/site/allure-maven-plugin/index.html
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `Could not start a new session` | Is Appium running? `appium --port 4723` |
| `Device not found` | Is emulator running? `adb devices` should show it |
| `App not installed` | Install: `adb install apps/neuronation-med.apk` |
| `Element not found` | Locator is wrong. Open Appium Inspector, find the real ID |
| `Connection refused on :4723` | Appium not started, or wrong port. Check terminal output |
| `No snapshot found` | Run with `-Dsnapshot.mode=record` first |
| `BUILD FAILURE` on compile | Run `mvn clean test-compile` and check error output |
| Tests pass but no Allure data | Check `target/allure-results/` directory exists and has JSON files |
| `java.lang.NoClassDefFoundError: org/aspectj` | Run `mvn dependency:resolve` to download AspectJ weaver |
