package com.neuronation.knownissues;

import com.neuronation.config.Platform;

import java.time.LocalDate;

/** A single tracked known issue, deserialized from an entry in known-issues.json. */
public class KnownIssue {

    private static final int DEFAULT_REVIEW_DAYS = 30;

    // The JSON key is injected by the registry after deserialization (Gson does not map keys).
    private transient String id;

    private String jira;        // full ticket URL
    private String platform;    // "ios" | "android" | "all"
    private String description; // optional
    private String opened;      // optional ISO date
    private String review;      // optional ISO date
    private Boolean strict;     // optional, default true

    public String getId() { return id; }
    void setId(String id) { this.id = id; }

    public String getJira() { return jira; }
    public String getDescription() { return description; }

    /** Strict (default): an unexpected pass fails the build so the flag gets removed. Non-strict
     *  (for inherently inconsistent bugs): the assertion is report-only — it never fails the build
     *  whether it passes or fails. */
    public boolean isStrict() {
        return strict == null || strict;
    }

    /** Ticket key parsed from the URL (last path segment), e.g. "MIBA-4277". */
    public String jiraKey() {
        if (jira == null || jira.isBlank()) return null;
        String trimmed = jira.replaceAll("/+$", "");
        int slash = trimmed.lastIndexOf('/');
        return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    }

    /** True when this entry's platform scope matches the current run platform. */
    public boolean matchesPlatform(Platform current) {
        if (platform == null || current == null) return false;
        return "all".equalsIgnoreCase(platform) || platform.equalsIgnoreCase(current.name());
    }

    /** True when {@code today} is past this entry's review date (or opened+30d if review is unset).
     *  Entries with neither date are never aged. */
    public boolean isAging(LocalDate today) {
        LocalDate reviewDate = effectiveReviewDate();
        return reviewDate != null && today != null && today.isAfter(reviewDate);
    }

    private LocalDate effectiveReviewDate() {
        if (review != null && !review.isBlank()) return LocalDate.parse(review.trim());
        if (opened != null && !opened.isBlank()) return LocalDate.parse(opened.trim()).plusDays(DEFAULT_REVIEW_DAYS);
        return null;
    }
}
