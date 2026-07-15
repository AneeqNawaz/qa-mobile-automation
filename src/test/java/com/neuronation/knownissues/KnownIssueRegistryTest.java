package com.neuronation.knownissues;

import com.neuronation.config.Platform;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.testng.Assert.*;

public class KnownIssueRegistryTest {

    private static final String JSON =
            "{\n" +
            "  \"consent-date-format\": {\n" +
            "    \"jira\": \"https://neuronation.atlassian.net/browse/MIBA-4277\",\n" +
            "    \"platform\": \"ios\"\n" +
            "  },\n" +
            "  \"cross-platform-thing\": {\n" +
            "    \"jira\": \"https://neuronation.atlassian.net/browse/MIBA-9999\",\n" +
            "    \"platform\": \"all\"\n" +
            "  },\n" +
            "  \"aging-one\": {\n" +
            "    \"jira\": \"https://neuronation.atlassian.net/browse/MIBA-1000\",\n" +
            "    \"platform\": \"ios\",\n" +
            "    \"opened\": \"2026-01-01\",\n" +
            "    \"review\": \"2026-02-01\"\n" +
            "  }\n" +
            "}";

    private KnownIssueRegistry reg() {
        return KnownIssueRegistry.fromJson(JSON);
    }

    @Test
    public void iosEntry_active_onIos() {
        Optional<KnownIssue> ki = reg().active("consent-date-format", Platform.IOS);
        assertTrue(ki.isPresent(), "iOS entry should be active on iOS");
        assertEquals(ki.get().jiraKey(), "MIBA-4277", "jira key should be parsed from the URL");
    }

    @Test
    public void iosEntry_inactive_onAndroid() {
        assertTrue(reg().active("consent-date-format", Platform.ANDROID).isEmpty(),
                "an ios-scoped entry must NOT be active on Android (it should assert hard there)");
    }

    @Test
    public void allEntry_active_onBothPlatforms() {
        assertTrue(reg().active("cross-platform-thing", Platform.ANDROID).isPresent());
        assertTrue(reg().active("cross-platform-thing", Platform.IOS).isPresent());
    }

    @Test
    public void unknownId_inactive() {
        assertTrue(reg().active("no-such-id", Platform.IOS).isEmpty(),
                "an unknown id must not be treated as an active known issue");
    }

    @Test
    public void aging_pastReviewDate_isAging() {
        KnownIssue ki = reg().active("aging-one", Platform.IOS).orElseThrow();
        assertTrue(ki.isAging(LocalDate.of(2026, 3, 1)), "past its review date → aging");
        assertFalse(ki.isAging(LocalDate.of(2026, 1, 15)), "before its review date → not aging");
    }

    @Test
    public void aging_noDates_neverAging() {
        KnownIssue ki = reg().active("consent-date-format", Platform.IOS).orElseThrow();
        assertFalse(ki.isAging(LocalDate.of(2030, 1, 1)),
                "an entry with no review/opened date is never aged");
    }

    @Test
    public void strict_defaultsTrue_falseWhenSet() {
        KnownIssueRegistry r = KnownIssueRegistry.fromJson(
                "{\"a\":{\"jira\":\"https://x/browse/M-1\",\"platform\":\"ios\"}," +
                "\"b\":{\"jira\":\"https://x/browse/M-2\",\"platform\":\"ios\",\"strict\":false}}");
        assertTrue(r.active("a", Platform.IOS).orElseThrow().isStrict(), "strict defaults to true");
        assertFalse(r.active("b", Platform.IOS).orElseThrow().isStrict(), "explicit strict:false honored");
    }

    @Test
    public void all_returnsEveryEntry_forReporting() {
        assertEquals(reg().all().size(), 3, "all() should expose every entry for the run summary");
    }

    /** Guards the real shipped registry file: the 3 MED iOS known issues must be active on iOS
     *  and inert on Android (where they should still assert hard). */
    @Test
    public void shippedRegistry_hasThreeIosEntries_inertOnAndroid() {
        KnownIssueRegistry reg = KnownIssueRegistry.load();
        String[] ids = {"consent-date-format", "reminder-permission-config", "neurobooster-cancel-toggle"};
        for (String id : ids) {
            assertTrue(reg.active(id, Platform.IOS).isPresent(), id + " should be active on iOS");
            assertTrue(reg.active(id, Platform.ANDROID).isEmpty(), id + " must be inert on Android");
        }
        assertEquals(reg.active("consent-date-format", Platform.IOS).orElseThrow().jiraKey(), "MIBA-4277");
        assertEquals(reg.active("reminder-permission-config", Platform.IOS).orElseThrow().jiraKey(), "MIBA-4280");
        assertEquals(reg.active("neurobooster-cancel-toggle", Platform.IOS).orElseThrow().jiraKey(), "MIBA-4281");

        // consent format is inconsistent on iOS → report-only; the other two are strict (auto-alert).
        assertFalse(reg.active("consent-date-format", Platform.IOS).orElseThrow().isStrict());
        assertTrue(reg.active("reminder-permission-config", Platform.IOS).orElseThrow().isStrict());
        assertTrue(reg.active("neurobooster-cancel-toggle", Platform.IOS).orElseThrow().isStrict());
    }
}
