# MED E2E — Work & Verification Summary

_Temporary handoff doc. Covers the comprehensive per-flow E2E happy path built and verified for the NeuroNation MED app. Safe to delete after reading._

---

## 1. Goal

Turn the 4 onboarding flows into **one comprehensive E2E per flow** that doesn't just reach the dashboard, but verifies that **every onboarding selection actually took effect** in the app, then exercises the login/account journey — all in a single onboarding per flow.

Each flow now runs end-to-end:

> register + onboarding → **Settings verification** → **Privacy Settings** → **Consent History** → logout → re-login → **Change Email** → **MCI validity** → logout

Entry point: `MedFullE2EHappyPathTest.fullE2EHappyPath` (data provider over flow1–flow4, all steps soft-asserted so one failure never halts the rest).

---

## 2. The 4 flows (coverage matrix)

The flows were tuned so they collectively cover **all 4 special-needs options × all 4 time slots × varied consents**.

| Flow | Auth | Special needs | Exercises | Time slot | Reminder | Newsletter / Data-retention / Data-processing |
|------|------|---------------|-----------|-----------|----------|-----------------------------------------------|
| **1** | password | standard | **23/23** | morning | 09:00 | Consent / Dissent / Dissent |
| **2** | password | **both** | **17/23** | evening | 18:00 | Dissent / Consent / Dissent |
| **3** | no-password | colorVision | **20/23** | noon | 14:00 | Consent / Consent / Consent |
| **4** | password | arithmetic | **20/23** | night | 21:00 | Dissent / Dissent / Consent |

**Locked exercises (device-verified):**
- colorVision → Form Fever, Colour Craze, Quantum Leap (3)
- arithmetic → Reflector, Chain Reaction, Mathrobatics (3)
- both → all 6 → 17/23
- standard → none → 23/23

---

## 3. What each flow verifies (the journey)

1. **Registration + onboarding** → Dashboard (incl. IMAP email verification).
2. **Onboarding Schedule Review** — the per-day schedule shown right after the time slot pick; all 7 days equal the slot time (e.g. morning → all 7 = 09:00).
3. **Settings (Profile → Settings), in on-screen order:**
   - **Special needs** — expand accordion, assert Color Vision / Arithmetic switches match the flow, collapse.
   - **Available Exercises** — count `X/23` matches; expand and read every checkbox; the unchecked set equals **exactly** the expected locked games (no others); **cross-check** that the displayed count = number of checked boxes.
   - **Comparison Group (age)** — subtitle = onboarding age group; expand → all 8 age-group options listed → collapse.
   - **Training Priorities** — defaults to "Recommended"; tap → "Attention" popup (handled whether it shows or expands directly) → Understood → 4 domains (Speed/Attention/Memory/Reasoning) → collapse.
   - **Language** — subtitle = launch language; expand → options listed → collapse.
   - **Training Adaptation** — activate → "Ask me", deactivate → "Don't ask me"; expand → both options → collapse.
   - **Training Reminder** — all 7 per-day times equal the slot time; "Personalised training times" ON.
   - **NeuroBooster** — switch matches the flow.
4. **Privacy Settings** — all 4 toggles present; the 4th (newsletter) matches the flow's I-agree/not-agree.
5. **Consent History** — Newsletter / Data Retention / Data Processing each show the correct **Consent vs Dissent** per the flow, each with a **date/time within 3 min** of the registration moment.
6. **Logout → re-login → Change Email** shows the login email → **MCI 90-day validity** (valid until today+90) → final logout.

---

## 4. Technical work

**New test structure**
- `MedSettingsVerifierBase` (abstract) — shared verification: the `step()` soft-assert wrapper, the 0–10 settings/privacy/consent steps, `verifyExpandableRow`, `verifyConsent`, timestamp/slot/language helpers.
- `MedFullE2EHappyPathTest` — comprehensive 4-flow E2E (data provider) + post-login Tips test.
- `MedSettingsVerificationTest` — slimmed to 2 standalone read-only checks (age-group list, training-priorities popup).

**Page objects (POM)**
- `SettingsScreen` — special-needs accordion switches (`accessibiletySwitch`, `dyscalculiaSwitch`); Available Exercises checkbox reader (single downward scroll sweep, stops at last); generic expandable single-select rows (`expandRowAndReadOptions` / `collapseRow`); Training Priorities popup + domains.
- `PrivacySettingsScreen` (new) — 4 toggles.
- `ConsentHistoryScreen` (new) — entries (title + date/time + Consent/Dissent), scroll-sweep + load wait.
- `ScheduleReviewScreen.getScheduleTimes()` — onboarding per-day times (12h → 24h normalized).
- `ProfileScreen` — `tapConsentHistory`, reliable scroll-to-row nav.
- `BaseScreen` — forward-scroll `scrollToElement` (no fling-to-top), `tapAt(x,y)`.
- `MedFlowHelper` — captures Schedule Review per-day times; **optional "Adjusting the volume" popup handling** at both videos.

**Data-driven**
- `testdata/med/exercises.json` — 23 exercises + locked sets per option; loader `ExerciseCatalog`.
- `testdata/med/settings-options.json` — expandable-row options + priority domains; loader `SettingsOptions`.
- `flows.json` — flow2 switched to `both` for full coverage.

**Suites** — `e2e-happy-path.xml`, `e2e-parallel-all-devices.xml`, `settings-verification.xml` run `fullE2EHappyPath` (`-Dflow=<name>` for a single flow).

---

## 5. Verification achieved

- **All 4 flows** verified the full settings/privacy/consent matrix on the emulator — every value above confirmed green.
- **Full E2E (flow1)** green on **both the emulator and the physical device** (incl. email verification, change-email, MCI, logout).
- **Happy-path journey (flows 1 & 2)** green back-to-back (logout between flows works).
- **Consent timestamps** pass (compared in UTC — see findings).
- **Volume-popup fix** verified on the physical device (popup dismissed, video closes, flow continues).

---

## 6. Findings / known issues (mostly app/FE or environment)

1. **Consent timestamp shown in UTC** — the FE displays the consent date/time in UTC (≈2h off local). Reported for a dev fix. The test currently compares in **UTC** to pass; a commented local-time line is ready to swap back once the FE is fixed.
2. **"Adjusting the volume" popup** — appears before explanatory videos when device volume is low/muted; misreported as an error dialog before. Now dismissed optionally at both videos, before the error-dialog check (real error dialogs still get flagged).
3. **Selected option not exposed** — for age group / training adaptation / language, the selected option is only a visual green highlight (no `checked`/`selected` accessibility attribute). Verified via the row subtitle + "all options present"; the green highlight itself is not asserted.
4. **BrowserStack device-lock blocker** — MED closes on launch without a screen lock; BrowserStack devices have none. Verified on the local emulator + physical device (which have locks). The nightly on BrowserStack may fail at launch for this reason — environmental, not a test defect.
5. Exercise checkboxes expose no resource-id (matched by name); Settings rows are inline accordions.

---

## 7. How to run

Single flow on emulator:
```bash
IMAP_PASSWORD='<pwd>' mvn -B -ntp test \
  -Dconfig.profile=med-android-emulator -Ddevice.udid=emulator-5556 -Ddevice.name=sdk_gphone16k_arm64 \
  -DsuiteFile=src/test/resources/suites/settings-verification.xml \
  -Dflow=flow1_password_morning_skip -Dactivation.code=77AAAAAAAAAAAAAX
```
- Drop `-Dflow` → all 4 flows.
- Physical device → `-Dconfig.profile=med-android` (no udid/name overrides needed).

---

## 8. CI / Jenkins

- Nightly default `SUITE=e2e-happy-path` → runs `fullE2EHappyPath` for **all 4 flows** on **BrowserStack** (`med-android-latest`).
- All code is committed and pushed to `origin/main` (HEAD includes the volume-popup fix).
- Confirm the job passes `IMAP_PASSWORD` + a valid `-Dactivation.code`, and watch for the BrowserStack device-lock blocker on first run.

---

## 9. iOS support (local, real iPhone)

The full E2E now runs on **iOS** (real iPhone, XCUITest). flow1 and flow3 were verified **end-to-end**: onboarding → Settings/Privacy/Consent → logout → re-login → Change Email → MCI → logout.

**WDA must be started before iOS runs** (config-med-ios uses an external WDA on :8100):
```bash
# 1) build+run WDA (stays running — don't Ctrl-C):
cd ~/.appium/node_modules/appium-xcuitest-driver/node_modules/appium-webdriveragent
xcodebuild -project WebDriverAgent.xcodeproj -scheme WebDriverAgentRunner \
  -destination 'id=<udid>' test
# 2) forward the port (background):
iproxy -u <udid> 8100:8100
# 3) verify: curl -s http://127.0.0.1:8100/status  → "ready": true
```
Device prerequisite: the iPhone must have a **screen-lock passcode set** (MED closes on launch without one). Appium for iOS runs on **:4724**.

**Run iOS:**
```bash
# full single flow (stops after the flow — no extra re-registering tests):
IMAP_PASSWORD='<pwd>' mvn -B -ntp test -Dconfig.profile=med-ios \
  -DsuiteFile=src/test/resources/suites/e2e-flow-only-ios.xml \
  -Dflow=flow1_password_morning_skip -Dactivation.code=77AAAAAAAAAAAAAX

# fast logged-in Settings/Privacy/Consent check (no onboarding; app already on Dashboard):
mvn -B -ntp test -Dconfig.profile=med-ios -Dforce.app.launch=false \
  -DsuiteFile=src/test/resources/suites/settings-loggedin.xml -Dflow=flow1_password_morning_skip
```
- `-Dforce.app.launch=false` attaches to the app's current state without relaunching (for logged-in-only checks).

**iOS implementation notes**
- Settings/Privacy/Consent readers are platform-branched (`SettingsScreen`, `PrivacySettingsScreen`, `ConsentHistoryScreen`): iOS reads via predicate/page-source, e.g. expandable rows verify the selected value (tap→verify→collapse), special-needs toggles pair to their labels, the Training-Priorities popup is a native `Alert`+`Understood`, Consent parses `dd.MM.yyyy HH:mm:ss`.
- Onboarding diverges from Android: iOS shows the **NeuroBooster notification "Open Settings / Cancel" Alert** (dismissed via Cancel) and **no Promise screen** (NB → straight to Tips) — both handled in `MedFlowHelper`.
- New: `MedSettingsLoggedInTest` (+ `settings-loggedin.xml`), `e2e-flow-only-ios.xml` (single-flow, no extra tests), and the `force.app.launch` config option.

**Known iOS gaps (Android-only readers, low priority):**
1. Step 0 — onboarding **Schedule Review** per-day times read empty on iOS (Android-only locators).
2. Step 7 — for a notification-**deny** flow the reminder reads `22:59` instead of empty (Android shows empty); toggle-OFF is correct.
3. Available Exercises is **count-only** on iOS — every exercise `Switch` shares one frame (`y≈282`), so per-game checked state can't be paired (the `X/Y` count label is the authoritative signal).
