# Known-Issue Quarantine — Guide

How the automation keeps the nightly **green while confirmed product bugs stay open**, without
hiding them. This is the reference for **adding** and **removing** known issues.

---

## 1. What it does (mental model)

Some assertions fail because of a **real, tracked product bug** (usually iOS-only). We don't want
those to turn the whole nightly red for weeks (a permanently-red build gets ignored), but we also
must **not silently hide** them.

A "known issue" is an assertion that is **quarantined**: it still runs, but a failure is **reported**
(Allure + Slack) instead of failing the build. The rules:

| Situation | Build result |
|---|---|
| No known issue, assertion passes | ✅ pass |
| No known issue, assertion fails | ❌ **red** (real regression) |
| Known issue active, assertion fails | 🟡 reported, **not red** (expected) |
| Known issue active (strict), assertion **passes** | ❌ **red** — "UNEXPECTED PASS, remove the flag" |
| Known issue active (strict:false), assertion passes or fails | 🟡 reported, **never red** |

The **UNEXPECTED PASS → red** rule is the honesty safeguard: the day dev fixes the bug, the build
goes red and tells you to delete the flag. It cannot rot into permanent masking.

**Platform scoping:** an entry is scoped to `ios`, `android`, or `all`. An `ios` entry is *inert*
on Android — there the assertion runs **hard** (normal). So Android never masks an iOS-only bug.

---

## 2. Where everything lives

| Thing | Path |
|---|---|
| **Registry** (the list of known issues) | `src/test/resources/testdata/med/known-issues.json` |
| Engine classes | `src/test/java/com/neuronation/knownissues/` |
| `knownIssue(...)` helper | `src/test/java/com/neuronation/tests/med/profile/MedSettingsVerifierBase.java` |
| Unit tests | same `knownissues/` folder (`*Test.java`) |
| Unit test suite | `src/test/resources/suites/unit-knownissues.xml` |
| Allure failure buckets | `src/test/resources/allure/categories.json` |
| Run summary (generated) | `target/known-issues-report.json` |
| Design spec | `docs/superpowers/specs/2026-07-15-known-issue-quarantine-design.md` (local only) |

**Engine classes (you rarely touch these):**
- `KnownIssue` — one registry entry (`jiraKey()`, `isStrict()`, `matchesPlatform()`, `isAging()`).
- `KnownIssueRegistry` — loads/queries the JSON (`load()`, `active(id, platform)`).
- `KnownIssueEvaluator` / `KnownIssueDecision` — the pure decision matrix.
- `KnownIssueAction` — maps a decision to fail / record / nothing + builds messages.
- `KnownIssueReport` / `KnownIssueTracker` — collect what fired, write `known-issues-report.json`.

---

## 3. The registry format

`known-issues.json` is a map of **`id` → entry**. The `id` (the JSON key) is the join between the
file and the `knownIssue("<id>", …)` call in the test code.

```json
{
  "consent-date-format": {
    "jira": "https://neuronation.atlassian.net/browse/MIBA-4277",
    "platform": "ios",
    "strict": false,
    "description": "…why it's a known issue…",
    "opened": "2026-07-15",
    "review": "2026-08-15"
  }
}
```

| Field | Required | Meaning |
|---|---|---|
| `jira` | ✅ | Full ticket URL. The key (`MIBA-4277`) is parsed from it automatically. |
| `platform` | ✅ | `ios` \| `android` \| `all`. Where the bug exists; elsewhere the assertion runs hard. |
| `strict` | optional (default `true`) | `true`: an unexpected pass turns the build red (auto-alert when fixed). `false`: **report-only** — never fails the build either way. Use `false` only for *inconsistent* bugs (see §6). |
| `description` | optional | Human note. |
| `opened` | optional | ISO date the flag was added. |
| `review` | optional | ISO date; past it, the entry shows as **aging** in the summary. If omitted, defaults to `opened + 30 days`; if `opened` is also omitted, never ages. |

---

## 4. ➕ Add a new known issue

There are two cases. **Case A** (the common one) is when the assertion already exists in the test.

### Case A — quarantine an assertion that already exists

**Step 1 — get a real Jira ticket.** No ticket, no flag. Copy its URL.

**Step 2 — add the registry entry** in `known-issues.json`. Pick a short, stable `id`:

```json
"my-new-issue": {
  "jira": "https://neuronation.atlassian.net/browse/MIBA-1234",
  "platform": "ios",
  "description": "one line on the bug",
  "opened": "2026-07-16",
  "review": "2026-08-16"
}
```

**Step 3 — wrap the assertion** in the test with `knownIssue(...)`, keeping the original line
commented out (so restoring it later is a one-line uncomment). Two overloads:

```java
// assertEquals style:
knownIssue("my-new-issue", actualValue, expectedValue, "message");
// assertTrue style (condition = the PASS condition):
knownIssue("my-new-issue", someBoolean, "message");
```

Example — converting an existing assertion:

```java
// BEFORE
softAssert.assertEquals(screens.settings().isNeuroBoosterEnabled(), flow.isNeuroBooster(),
        "NeuroBooster switch should match flow.neuroBooster");

// AFTER
// Known issue on iOS (MIBA-1234): <one line>. When fixed: delete the known-issues.json entry,
// then uncomment the line below and remove the knownIssue() call.
// softAssert.assertEquals(screens.settings().isNeuroBoosterEnabled(), flow.isNeuroBooster(),
//         "NeuroBooster switch should match flow.neuroBooster");
knownIssue("my-new-issue", screens.settings().isNeuroBoosterEnabled(), flow.isNeuroBooster(),
        "NeuroBooster switch should match flow.neuroBooster");
```

> **Scope the wrap to when the bug actually occurs.** If a bug only happens in one scenario (e.g.
> only when notifications are denied), wrap it only in that branch and keep the normal assert
> elsewhere — otherwise the flows where the app is *correct* will trigger an UNEXPECTED PASS.
> (This is exactly what bit builds #66: the reminder/neurobooster bugs only occur in the
> notification-denied flow.)

**Step 4 — verify** with a clean compile + the unit suite, then run on device:

```bash
mvn clean test-compile
mvn test -DsuiteFile=src/test/resources/suites/unit-knownissues.xml
```

Commit and let it run on the nightly. Expected: that platform stays green; the issue shows up as
reported (see §7).

### Case B — the assertion doesn't exist yet

Same as Case A, but in Step 3 you *write* the assertion for the first time using `knownIssue(...)`
instead of `softAssert`. (This is what we did for MIBA-4277 — there was no format check before.)

---

## 5. ➖ Remove / retire a known issue (bug fixed)

When dev fixes the bug, the assertion starts passing on the affected platform.

**If the entry is `strict` (default):** the very next run **fails red** with:

```
UNEXPECTED PASS: MIBA-1234 ('my-new-issue') now passes on ios — the bug appears fixed.
Remove this entry from known-issues.json to re-enable the assertion.
```

That is your signal. To retire it:

1. **Delete the entry** from `known-issues.json`.
2. In the test, **remove the `knownIssue(...)` call and uncomment the original assertion**
   (and delete the "Known issue …" comment block). For a Case-B issue, just convert the
   `knownIssue(...)` back to a normal `softAssert...`.
3. `mvn clean test-compile` + run the unit suite, then push.

After that, the assertion is a normal hard check again — if the bug ever regresses, it goes red.

**If the entry is `strict:false`:** there's no auto-alert (that's the trade-off). Retire it when the
ticket is closed: same steps 1–3.

> **Minimum to stop reporting an issue = delete the JSON entry.** The build won't fail (the code
> still has the `knownIssue(...)` call, which just behaves like a normal assert once no entry
> matches). But you should also do step 2 so the code is clean and the original assert is restored.

---

## 6. strict vs strict:false

- **strict (default)** — use for a bug that fails **consistently** while open. You get the
  auto-alert-when-fixed safeguard. Prefer this.
- **strict:false** — use only for an **inconsistent / flaky** product bug where the assertion
  sometimes passes even though the bug is open (e.g. MIBA-4277: iOS renders the consent date in a
  different format per flow, and one flow accidentally matches). A strict flag there would flip the
  build red on the "accidental pass". strict:false makes it purely report-only. Cost: you must
  retire it manually when the ticket closes (no auto-alert).

---

## 7. Where known issues show up

Known issues ride on **passing** tests, so Allure's pass/fail donut and the **Categories** tab
(which only bucket failed/broken) will **not** list them. They appear here instead:

- **Allure → Overview → Environment widget:** a `KnownIssues` row per platform, e.g.
  `MIBA-4277 (hit); MIBA-4280 (hit); MIBA-4281 (hit) - 3 hit, 0 aging`.
- **Allure → any affected test → Tags:** `known-issue` and the ticket key (e.g. `MIBA-4277`) as
  filterable chips.
- **Allure → any affected test → Links:** the Jira ticket as a clickable Issue link.
- **Allure → test → step tree:** a `🟡 Known issue MIBA-… (expected fail): …` step.
- **Slack summary** (`#qa-automation-nightly`, when `NOTIFY_SLACK` is on): the per-platform line
  gets `🟡 N known (KEYS) ⏳ N aging`.
- **`target/known-issues-report.json`:** machine-readable list (id, key, jira, encountered, aging).

`categories.json` also adds Allure buckets for **UNEXPECTED PASS** (a fixed known issue) and real
regressions — those *do* show, because they fail.

---

## 8. Current entries (worked examples)

| id | Ticket | Platform | strict | Wrapped at |
|---|---|---|---|---|
| `consent-date-format` | MIBA-4277 | ios | **false** | `MedSettingsVerifierBase.verifyConsent()` — consent timestamp format |
| `reminder-permission-config` | MIBA-4280 | ios | true | `verifyOnboardingSettingsPrivacyConsent()` step 7, **deny-path** `anyTimeShown` |
| `neurobooster-cancel-toggle` | MIBA-4281 | ios | true | step 8, **only when notifications denied** |

---

## 9. Rules / gotchas

- **A flag needs a real ticket.** No ticket = masking. Don't add one without a Jira URL.
- **Keep the original assertion as a comment**, not deleted — one-line restore when fixed.
- **Scope the wrap to the failing scenario** (platform + flow/branch), or correct behaviour on
  other flows becomes an UNEXPECTED PASS.
- **Always `mvn clean test-compile`** before pushing — incremental compile can hide errors that
  only surface in CI (this caused build #68's failure).
- **Known issue ≠ flaky ≠ regression.** If a run fails from a BrowserStack/session timeout or a
  one-off element-not-found in onboarding, that's flakiness — not a known issue; don't add a flag,
  just re-run (see build #69).
- **Android stays honest.** iOS-scoped entries do nothing on Android; the assertion runs hard there.
